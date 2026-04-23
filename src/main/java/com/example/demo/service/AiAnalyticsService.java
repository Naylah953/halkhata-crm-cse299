package com.example.demo.service;

import com.example.demo.dto.AiAnalyticsResponse;
import com.example.demo.dto.gemini.GeminiRequest;
import com.example.demo.dto.gemini.GeminiResponse;
import lombok.RequiredArgsConstructor;
import com.example.demo.dto.OpenAiDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiAnalyticsService {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;

    // --- GEMINI CONFIG ---
    @Value("${gemini.api.url}")
    private String geminiUrl;
    @Value("${gemini.api.key}")
    private String geminiKey;

    // --- OPENROUTER CONFIG ---
    @Value("${openrouter.api.url}")
    private String openRouterUrl;
    @Value("${openrouter.api.key}")
    private String openRouterKey;
    @Value("${openrouter.api.model}")
    private String openRouterModel;

    @Transactional(readOnly = true)
    public AiAnalyticsResponse processAnalyticsQuery(String userPrompt, Long tenantId) {

        // 1. DYNAMIC INJECTION: Fetch the real JSON keys for this specific shop
        String customJsonbContext = fetchDynamicJsonKeys(tenantId);

        // 2. Craft the Master Prompt for Text-to-SQL
        String systemInstruction = """
            You are a PostgreSQL expert acting as a Text-to-SQL translator for a CRM.
            
            Here is the database schema:
            - products(id, base_name, attributes JSONB, price, quantity, created_at, updated_at, schema_id, tenant_id)
            - orders(id, created_at, delivery_method, payment_method, status, total_amount, updated_at, customer_id, staff_id, tenant_id)
            - order_items(id, quantity, unit_price, order_id, product_id)
            - customers(id, full_name, email, phone_number, address, order_count, total_spent, created_at, updated_at, tenant_id)            
            
            CRITICAL RULES:
            1. To query dynamic product attributes, you MUST use the ->> operator on the 'attributes' JSONB column. 
               Context for this tenant's JSONB keys: %s
            2. ALWAYS use human-readable ALIASES using 'AS' for columns (e.g., base_name AS "Product Name").
            3. NEVER include a WHERE clause for tenant_id. The system handles this securely via RLS.
            4. Output ONLY the raw SQL string. No markdown, no ```sql blocks, no explanations. Just the query.
            5. When filtering by text strings... ALWAYS use the case-insensitive ILIKE operator with wildcards.
            6. Use SELECT DISTINCT when the user asks exclusively for a list of customers or contacts. If the user asks for orders or itemized records, use a standard SELECT to preserve duplicates.
            """.formatted(customJsonbContext);

        String rawSql = "";

        // 3. Call Gemini
        try {
            System.out.println("Attempting AI generation with Primary Engine (Gemini)...");
            GeminiRequest request = GeminiRequest.builder()
                    .systemInstruction(GeminiRequest.SystemInstruction.builder()
                            .parts(List.of(GeminiRequest.Part.builder().text(systemInstruction).build())).build())
                    .contents(List.of(GeminiRequest.Content.builder()
                            .role("user")
                            .parts(List.of(GeminiRequest.Part.builder().text(userPrompt).build())).build()))
                    .build();

            GeminiResponse response = restClient.post()
                    .uri(geminiUrl + "?key=" + geminiKey)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            rawSql = response.getCandidates().get(0).getContent().getParts().get(0).getText();
            System.out.println("Success! Gemini generated the query.");

        } catch (Exception geminiException) {

            // ==========================================
            // FALLBACK ENGINE: OPENROUTER
            // ==========================================
            System.out.println("Gemini failed (" + geminiException.getMessage() + "). Pivoting to Fallback Engine (OpenRouter)...");

            try {
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
                // If BOTH engines fail, return the graceful error
                System.out.println("CRITICAL: Both AI engines failed. OpenRouter error: " + openRouterException.getMessage());
                return AiAnalyticsResponse.builder()
                        .aiSummary("Our AI analysts are currently overwhelmed. Please try again in a few moments.")
                        .isTable(false)
                        .build();
            }
        }

        // Clean the AI output just in case it wraps it in markdown
        rawSql = rawSql.replaceAll("```sql", "").replaceAll("```", "").trim();
        System.out.println("\n=== FINAL AI GENERATED SQL ===\n" + rawSql + "\n============================\n");

        // ADD THIS LINE TO DEBUG:
        System.out.println("\n=== GEMINI GENERATED SQL ===\n" + rawSql + "\n============================\n");

        // "Freeze" the variable so the lambda accepts it
        final String finalSql = rawSql;

        // 4. Securely Execute the SQL via the Database Sandbox
        return jdbcTemplate.execute((ConnectionCallback<AiAnalyticsResponse>) con -> {
            try (Statement stmt = con.createStatement()) {

                // FORCEFIELD ACTIVATED: Lock this connection to the specific tenant and restricted AI role
                stmt.execute("SET LOCAL role = 'ai_analyst'");
                stmt.execute("SET LOCAL app.current_tenant = '" + tenantId + "'");

                List<Map<String, Object>> rows = new ArrayList<>();
                List<String> columns = new ArrayList<>();

                // Execute the AI's query
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

                // 5. Build the Dual-Payload
                AiAnalyticsResponse.TableData tableData = AiAnalyticsResponse.TableData.builder()
                        .columns(columns)
                        .rows(rows)
                        .build();

                return AiAnalyticsResponse.builder()
                        .aiSummary("Here is the data you requested:")
                        .isTable(!rows.isEmpty())
                        .tableData(tableData)
                        .build();
            }
        });
    }

    /**
     * Extracts all unique JSON keys currently used by a specific tenant in their products table.
     */
    private String fetchDynamicJsonKeys(Long tenantId) {
        String sql = """
            SELECT DISTINCT jsonb_object_keys(attributes) 
            FROM products 
            WHERE tenant_id = ? AND attributes IS NOT NULL
            """;

        try {
            List<String> keys = jdbcTemplate.queryForList(sql, String.class, tenantId);

            if (keys.isEmpty()) {
                return "This tenant has no custom JSON attributes defined yet.";
            }

            return "Available JSON keys for this tenant include: " + String.join(", ", keys);

        } catch (Exception e) {
            // Fallback in case the table is empty or doesn't exist yet during early testing
            return "Assume standard keys like 'brand' or 'color'.";
        }
    }
}