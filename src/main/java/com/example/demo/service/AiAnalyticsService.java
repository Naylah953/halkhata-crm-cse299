package com.example.demo.service;

import com.example.demo.domain.ProductSchema;
import com.example.demo.dto.AiAnalyticsResponse;
import com.example.demo.dto.OpenAiDto;
import com.example.demo.repository.ProductSchemaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;

@Service
public class AiAnalyticsService {

    private final JdbcTemplate primaryJdbcTemplate;
    private final JdbcTemplate readOnlyJdbcTemplate;
    private final RestClient restClient;
    private final ProductSchemaRepository productSchemaRepository;

    // Inject all templates and the new ProductSchemaRepository
    public AiAnalyticsService(
            JdbcTemplate primaryJdbcTemplate,
            @Qualifier("readOnlyJdbcTemplate") JdbcTemplate readOnlyJdbcTemplate,
            RestClient restClient,
            ProductSchemaRepository productSchemaRepository) {
        this.primaryJdbcTemplate = primaryJdbcTemplate;
        this.readOnlyJdbcTemplate = readOnlyJdbcTemplate;
        this.restClient = restClient;
        this.productSchemaRepository = productSchemaRepository;
    }

    @Value("${openrouter.api.url}")
    private String openRouterUrl;
    @Value("${openrouter.api.key}")
    private String openRouterKey;
    @Value("${openrouter.api.model}")
    private String openRouterModel;

    @Transactional(readOnly = true)
    public AiAnalyticsResponse processAnalyticsQuery(String userPrompt, Long tenantId) {

        String customJsonbContext = fetchDynamicJsonKeys(tenantId);

        String systemInstruction = """
            You are a PostgreSQL expert acting as a Text-to-SQL translator for a CRM.
            
            CRITICAL SECURITY RULE: The currently logged-in shop owner has a tenant_id of: %s.
            You MUST APPEND 'WHERE tenant_id = %s' to EVERY SINGLE SQL QUERY you generate.
            If you are using JOINs, ensure the tenant_id filter is applied to the primary table.
            Failure to do this will result in a massive data breach.
            
            Here is the database schema:
            - products(id, base_name, attributes JSONB, price, quantity, created_at, updated_at, schema_id, tenant_id)
            - orders(id, total_amount, status, delivery_method, payment_method, customer_id, staff_id, tenant_id, created_at, updated_at)
            - order_items(id, quantity, unit_price, order_id, product_id)
            - customers(id, full_name, phone_number, email, address, total_spent, order_count, tenant_id, created_at, updated_at)
            - contacts(id, name, tenant_id, customer_id, unread_count, requires_human, order_ready, ai_summary, tags, notes)
            - draft_orders(id, provided_name, provided_phone, provided_email, provided_address, delivery_method, payment_method, status, contact_id, tenant_id, created_at)
            - draft_order_items(id, product_id, quantity, draft_order_id)            
            
            CRITICAL RULES:
            1. To query dynamic product attributes, you MUST use the ->> operator on the 'attributes' JSONB column. 
               Context for this tenant's JSONB keys: %s
            2. ALWAYS use human-readable ALIASES using 'AS' for columns (e.g., base_name AS "Product Name").
            3. Output ONLY the raw SQL string. No markdown, no ```sql blocks, no explanations. Just the query.
            4. When filtering by text strings... ALWAYS use the case-insensitive ILIKE operator with wildcards.
            5. Use SELECT DISTINCT when the user asks exclusively for a list of customers or contacts. If the user asks for orders or itemized records, use a standard SELECT to preserve duplicates.
            6. CRITICAL: Never SELECT the following primay keys or foreign keys (id, customer_id, tenant_id, staff_id, schema_id, porduct_id). Never show created_at or updated_at unless explicitly asked. Only project columns directly relevant to the user's specific query to prevent table bloat.
            7. CRITICAL JOIN RULE: 'draft_orders.contact_id' is a String (PSID) that links to 'contacts.id'. It DOES NOT link to 'customers.id' (Long). To link draft orders to customers, you MUST join draft_orders -> contacts -> customers.
            8. MANDATORY COLUMNS: Whenever your query outputs data from the 'customers' table, you MUST ALWAYS include customers.full_name and customers.phone_number in the SELECT clause, even if not explicitly asked for. For the 'contacts' table, select only 'name' unless their ID is specifically requested.
            9. For strict status columns (like 'status', 'delivery_method', 'payment_method'), NEVER use ILIKE. You MUST use strict equality.
            10. ENUM MAPPING: You MUST map natural language in the user's prompt to the exact uppercase ENUM string below before querying strict columns:
                - status: 'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'
                - delivery_method: 'PATHAO', 'STEADFAST', 'REDX', 'SELF_PICKUP'
                - payment_method: 'CASH_ON_DELIVERY', 'BKASH', 'NAGAD', 'BANK_TRANSFER'
                (Example: If the user asks for "cash on delivery", use payment_method = 'CASH_ON_DELIVERY').
            11. A 'contact' only becomes a 'customer' after their first order. To find contacts who have NEVER ordered, simply use 'WHERE contacts.customer_id IS NULL'.
            12. EXCEPTION TO RULE 6 (ORDERS): You MUST explicitly select 'orders.id AS "Order ID"' when querying order data. Do not hide the Order ID.
            13. MANDATORY ORDER COLUMNS: When querying non-aggregate order data (orders or order_items), you MUST ALWAYS explicitly SELECT orders.id AS "Order ID", customers.full_name AS "Customer Name", products.base_name AS "Product Name", and orders.status AS "Status". You MUST JOIN the customers, order_items, and products tables to fetch this human-readable context.
            14. DRAFT ORDERS VS ORDERS: If the user asks for "drafts" or "draft orders", you MUST query the 'draft_orders' and 'draft_order_items' tables. NEVER query the 'orders' table looking for a status of 'DRAFT' (that status does not exist). When querying draft_orders, select 'draft_orders.id AS "Draft ID"'.
            15. ZERO-RESULT FALLBACK: You are a strict data retrieval system. Do not use outside knowledge. If a tool or database query returns empty data, you must reply exclusively with 'No records found' and offer no further explanation.
            16. PSID FILTERING: If the prompt asks to filter by 'this customer' and provides a long numeric PSID, you MUST NEVER filter using 'customers.id = PSID'. The PSID is a String that corresponds to 'contacts.id'. To find a customer's orders using their PSID, you MUST JOIN the 'contacts' table (ON contacts.customer_id = orders.customer_id) and filter using "WHERE contacts.id = 'the_psid'".
            """.formatted(tenantId, tenantId, customJsonbContext);

        String rawSql = "";

        try {
            System.out.println("Attempting AI generation with OpenRouter...");

            OpenAiDto.Request request = OpenAiDto.Request.builder()
                    .model(openRouterModel)
                    .messages(List.of(
                            OpenAiDto.Message.builder().role("system").content(systemInstruction).build(),
                            OpenAiDto.Message.builder().role("user").content(userPrompt).build()
                    ))
                    .build();

            OpenAiDto.Response response = restClient.post()
                    .uri(openRouterUrl)
                    .header("Authorization", "Bearer " + openRouterKey)
                    .header("HTTP-Referer", "http://localhost:8080")
                    .body(request)
                    .retrieve()
                    .body(OpenAiDto.Response.class);

            rawSql = response.getChoices().get(0).getMessage().getContent();
            System.out.println("Success! OpenRouter generated the query.");

        } catch (Exception openRouterException) {
            System.out.println("CRITICAL: OpenRouter failed. Error: " + openRouterException.getMessage());
            return AiAnalyticsResponse.builder()
                    .aiSummary("Our AI analysts are currently overwhelmed. Please try again in a few moments.")
                    .isTable(false)
                    .build();
        }

        rawSql = rawSql.replaceAll("```sql", "").replaceAll("```", "").trim();
        System.out.println("\n=== FINAL AI GENERATED SQL ===\n" + rawSql + "\n============================\n");

        final String finalSql = rawSql;

        // CRITICAL UPDATE: We execute this purely on the SECURE READ-ONLY connection
        return readOnlyJdbcTemplate.execute((ConnectionCallback<AiAnalyticsResponse>) con -> {
            try (Statement stmt = con.createStatement()) {

                List<Map<String, Object>> rows = new ArrayList<>();
                List<String> columns = new ArrayList<>();

                var rs = stmt.executeQuery(finalSql);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnLabel(i));
                }

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columns.get(i - 1), rs.getObject(i));
                    }
                    rows.add(row);
                }

                AiAnalyticsResponse.TableData tableData = AiAnalyticsResponse.TableData.builder()
                        .columns(columns)
                        .rows(rows)
                        .build();

                return AiAnalyticsResponse.builder()
                        .aiSummary("Here is the data you requested:")
                        .isTable(!rows.isEmpty())
                        .tableData(tableData)
                        .build();
            } catch (Exception e) {
                // Return gracefully if the AI generates syntax errors or attempts to write
                System.err.println("SQL Execution Failed (likely caught by Read-Only constraints): " + e.getMessage());
                return AiAnalyticsResponse.builder()
                        .aiSummary("I couldn't process your data query at this time. Please try rephrasing it.")
                        .isTable(false)
                        .build();
            }
        });
    }

    private String fetchDynamicJsonKeys(Long tenantId) {
        try {
            List<ProductSchema> schemas = productSchemaRepository.findAllByTenantId(tenantId);

            if (schemas == null || schemas.isEmpty()) {
                return "This tenant has no custom product schemas defined yet.";
            }

            StringBuilder blueprint = new StringBuilder("\nAvailable product schemas and their JSON attribute definitions:\n");

            for (ProductSchema schema : schemas) {
                blueprint.append("- Category Name: '").append(schema.getName()).append("'\n")
                        .append("  -> Filter using: schema_id = ").append(schema.getId()).append("\n")
                        .append("  -> Attributes (Data Types): ")
                        .append(schema.getSchemaDefinition() != null ? schema.getSchemaDefinition().toString() : "None")
                        .append("\n");
            }

            return blueprint.toString();

        } catch (Exception e) {
            System.err.println("Failed to fetch dynamic JSON keys: " + e.getMessage());
            return "Assume standard keys like 'brand' or 'color'.";
        }
    }
}