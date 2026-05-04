package com.example.demo.ai.tools;

import com.example.demo.domain.*;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.repository.*;
import com.example.demo.service.AiAnalyticsService;
import com.example.demo.dto.AiAnalyticsResponse;
import com.example.demo.service.SseService;

import jakarta.persistence.EntityManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

@Service
public class ModeratorTools {

    // NEW: ThreadLocal to stash table data for the current HTTP request
    public static final ThreadLocal<AiAnalyticsResponse.TableData> currentTableData = new ThreadLocal<>();

    // --- ALL DEPENDENCIES DECLARED AS FINAL (NO @Autowired fields) ---
    private final ContactRepo contactRepo;
    private final EntityManager entityManager;
    private final AiAnalyticsService aiAnalyticsService;
    private final SseService sseService;
    private final OrderRepository orderRepository;
    private final ComplaintRepo complaintRepo;
    private final TaskRepo taskRepo;
    private final MessageRepo messageRepo;

    // --- SINGLE CONSTRUCTOR FOR INJECTION ---
    public ModeratorTools(ContactRepo contactRepo,
                          EntityManager entityManager,
                          AiAnalyticsService aiAnalyticsService,
                          SseService sseService,
                          OrderRepository orderRepository,
                          ComplaintRepo complaintRepo,
                          TaskRepo taskRepo,
                          MessageRepo messageRepo) {
        this.contactRepo = contactRepo;
        this.entityManager = entityManager;
        this.aiAnalyticsService = aiAnalyticsService;
        this.sseService = sseService;
        this.orderRepository = orderRepository;
        this.complaintRepo = complaintRepo;
        this.taskRepo = taskRepo;
        this.messageRepo = messageRepo;
    }

    // ==========================================
    // 1. ANALYTICS & FINANCIAL TOOLS
    // ==========================================

    @Tool(description = "CRITICAL: ONLY call this to get financial metrics like total revenue and order count for TODAY, WEEK, or MONTH.")
    public String getShopPerformance(
            @ToolParam(description = "The timeframe to check: 'TODAY', 'WEEK', or 'MONTH'") String timeframe,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        List<Order> allOrders = orderRepository.findAllByTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = "WEEK".equalsIgnoreCase(timeframe) ? now.minusWeeks(1) :
                ("MONTH".equalsIgnoreCase(timeframe) ? now.minusMonths(1) : now.toLocalDate().atStartOfDay());

        List<Order> filteredOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(limit))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = filteredOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("Success. Tell the admin: SHOP PERFORMANCE (%s) - Total Orders: %d | Total Revenue: BDT %.2f",
                timeframe.toUpperCase(), filteredOrders.size(), totalRevenue);
    }

    @Tool(description = "THE CATCH-ALL ANALYTICS ENGINE: Use this tool whenever the user asks to view, list, search, or filter orders, products, or customers using conditions that the basic tools cannot handle (e.g., 'Show orders where delivery method is Pathao', 'List pending orders above 500 BDT', 'Top 5 customers'). Pass their exact question as the prompt.")
    public String runDatabaseAnalytics(String prompt, Long tenantId) {

        System.out.println("Manager AI is delegating a complex query to the Analytics Specialist...");

        AiAnalyticsResponse response = aiAnalyticsService.processAnalyticsQuery(prompt, tenantId);

        if (response.isTable()) {
            // Stash the table data into the ThreadLocal
            currentTableData.set(response.getTableData());

            String rawData = response.getTableData().getRows().toString();
            return "Raw Database Results: " + rawData +
                    " \n\n[SYSTEM NOTE: The UI is already showing this data as a table. Write a brief 2-sentence summary highlighting the most interesting metric. Do not list everything.]";
        } else {
            return "Analytics Result: " + response.getAiSummary();
        }
    }

    // Helper method to securely clear the thread memory
    public static void clearTableData() {
        currentTableData.remove();
    }


    // ==========================================
    // 2. CUSTOMER PROFILING & WORKFLOW
    // ==========================================

    @Tool(description = "CRITICAL: ONLY call this when the admin explicitly asks for a summary, profile, or dossier of a specific customer.")
    public String getCustomerDossier(
            @ToolParam(description = "The PSID of the contact") String psid,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if (psid == null || psid.equals("NONE")) return "Error: No specific customer selected.";

        Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);
        if (contactOpt.isEmpty()) return "Error: Contact not found.";

        Contact contact = contactOpt.get();
        double totalSpent = 0.0;

        if (contact.getCustomer() != null) {
            List<Order> orders = orderRepository.findAllByCustomerIdAndTenantId(contact.getCustomer().getId(), tenantId);
            totalSpent = orders.stream()
                    .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0)
                    .sum();
        }

        return String.format("Success. Summarize this dossier for the admin:\n- Name: %s\n- Tags: %s\n- Notes: %s\n- Total Lifetime Spending: BDT %.2f",
                contact.getName(),
                (contact.getTags() != null ? contact.getTags() : "None"),
                (contact.getNotes() != null ? contact.getNotes() : "No notes yet"),
                totalSpent);
    }

    // ==========================================
    // TAG & NOTE MANAGEMENT
    // ==========================================

    @Tool(description = "CRITICAL: You MUST ALWAYS verify the PSID is valid before attempting an update. ONLY call this to ADD or REMOVE specific tags (e.g., 'VIP', 'Spammer') or add notes to a customer's profile when requested by the admin.")
    public String updateCustomerMeta(
            @ToolParam(description = "Action to perform: 'ADD' or 'REMOVE'") String action,
            @ToolParam(description = "The PSID of the contact") String psid,
            @ToolParam(description = "The content to add/remove (the note or the exact tag name)") String content,
            @ToolParam(description = "Type of update: 'NOTE' or 'TAG'") String type,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if (psid == null || psid.equals("NONE")) return "Error: No specific customer selected.";

        Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);
        if (contactOpt.isEmpty()) return "Error: Contact not found.";

        Contact contact = contactOpt.get();

        if ("TAG".equalsIgnoreCase(type)) {
            if ("REMOVE".equalsIgnoreCase(action)) {
                if (contact.getTags() != null && !contact.getTags().isEmpty()) {
                    // Split, filter out the matching tag (ignoring case/spaces), and rejoin
                    String[] currentTags = contact.getTags().split(",");
                    List<String> updatedTags = new ArrayList<>();
                    for (String t : currentTags) {
                        if (!t.trim().equalsIgnoreCase(content.trim())) {
                            updatedTags.add(t.trim());
                        }
                    }
                    contact.setTags(updatedTags.isEmpty() ? null : String.join(", ", updatedTags));
                }
            } else {
                // ADD logic
                String existingTags = contact.getTags() == null ? "" : contact.getTags() + ", ";
                contact.setTags(existingTags + content);
            }
        } else {
            if ("REMOVE".equalsIgnoreCase(action)) {
                return "Error: Removing notes via the AI is not currently supported. You can only ADD notes.";
            }
            // ADD note logic
            String existingNotes = contact.getNotes() == null ? "" : contact.getNotes() + "\n";
            contact.setNotes(existingNotes + content);
        }

        contactRepo.save(contact);
        return "Success. Inform the admin that the " + type + " '" + content + "' was successfully " + action.toLowerCase() + "ed.";
    }

    // ==========================================
    // COMPLAINT MANAGEMENT
    // ==========================================

    @Tool(description = "Use this to CREATE, LIST, or RESOLVE customer complaints. CRITICAL: If the admin is currently in a multi-turn conversation to add a new complaint, you MUST use the 'CREATE' action. ONLY use the 'RESOLVE' action if the admin explicitly asks to resolve, fix, or close a complaint.")
    public String handleComplaints(
            @ToolParam(description = "Action: 'CREATE', 'LIST', or 'RESOLVE'") String action,
            @ToolParam(description = "The PSID of the customer (Required for CREATE/LIST)") String psid,
            @ToolParam(description = "The complaint text (for CREATE) OR the search keyword (for RESOLVE)") String description,
            @ToolParam(description = "The priority level: 'LOW', 'MEDIUM', 'HIGH' (Default to 'MEDIUM' if not specified)") String priority,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if ("RESOLVE".equalsIgnoreCase(action)) {
            if (description == null || description.trim().isEmpty()) {
                return "Error: You must provide a search keyword to resolve a complaint.";
            }

            // Phase 4: NLP Fuzzy Search
            List<Complaint> matches = complaintRepo.findByTenantIdAndDescriptionContainingIgnoreCase(tenantId, description);

            if (matches.isEmpty()) {
                return "No complaints found matching the keyword: '" + description + "'";
            }

            int count = 0;
            for (Complaint c : matches) {
                if (!"RESOLVED".equalsIgnoreCase(c.getStatus())) {
                    c.setStatus("RESOLVED");
                    count++;
                }
            }

            if (count == 0) {
                return "Success, but all complaints matching '" + description + "' were already resolved.";
            }

            complaintRepo.saveAll(matches);
            return "Success. You found and resolved " + count + " complaint(s) matching the phrase '" + description + "'.";

        } else if ("CREATE".equalsIgnoreCase(action)) {
            if (psid == null || psid.equals("NONE")) return "Error: No specific customer selected.";

            Complaint newComplaint = new Complaint();
            newComplaint.setPsid(psid);
            newComplaint.setDescription(description);
            // --- FIX 2: Set status to OPEN to match frontend expectations ---
            newComplaint.setStatus("OPEN");

            // Safely map the new Priority field with a MEDIUM fallback
            String safePriority = (priority != null && !priority.trim().isEmpty()) ? priority.toUpperCase() : "MEDIUM";
            newComplaint.setPriority(safePriority);

            newComplaint.setTenantId(tenantId);
            complaintRepo.save(newComplaint);
            return "Success. The " + safePriority + " priority complaint has been recorded.";

        } else if ("LIST".equalsIgnoreCase(action)) {
            // --- FIX 1: Implement missing LIST logic ---
            if (psid == null || psid.equals("NONE")) return "Error: No specific customer selected to list complaints.";

            List<Complaint> complaints = complaintRepo.findByPsidAndTenantIdOrderByCreatedAtDesc(psid, tenantId);

            if (complaints.isEmpty()) {
                return "No complaints found for this customer.";
            }

            StringBuilder sb = new StringBuilder("Customer Complaints:\n");
            for (Complaint c : complaints) {
                sb.append(String.format("- ID: %d | Status: %s | Priority: %s | Description: %s\n",
                        c.getId(), c.getStatus(), c.getPriority(), c.getDescription()));
            }
            return sb.toString();
        }

        return "Invalid action. Use CREATE, LIST, or RESOLVE.";
    }

    @Tool(description = "CRITICAL: ONLY call this to create a task/reminder for the admin, or to list their current pending tasks.")
    public String manageTasks(
            @ToolParam(description = "Action: 'CREATE' or 'LIST'") String action,
            @ToolParam(description = "Task description (required for CREATE)") String text,
            @ToolParam(description = "Customer PSID (optional)") String psid,
            @ToolParam(description = "Days from now to set due date (e.g., '1' for tomorrow)") Integer daysOut,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if ("CREATE".equalsIgnoreCase(action)) {
            Task task = new Task();
            task.setDescription(text);
            task.setPsid("NONE".equals(psid) ? null : psid);
            task.setTenantId(tenantId);
            task.setDueDate(LocalDateTime.now().plusDays(daysOut != null ? daysOut : 1));
            taskRepo.save(task);
            return "Success. Inform the admin that the task/reminder was created.";
        } else {
            List<Task> tasks = taskRepo.findByTenantIdAndCompletedFalse(tenantId);
            if (tasks.isEmpty()) return "Success. Tell the admin they have no pending tasks!";

            String taskList = tasks.stream()
                    .map(t -> "- " + t.getDescription() + " (Due: " + t.getDueDate().toLocalDate() + ")")
                    .collect(Collectors.joining("\n"));
            return "Success. Present this list of pending tasks to the admin:\n" + taskList;
        }
    }


    // ==========================================
    // 4. ORDER MANAGEMENT
    // ==========================================

    // Replace the annotation above manageOrders
    @Tool(description = "CRITICAL: ONLY call this to list a specific customer's basic order history (requires PSID) or update a SINGLE order's status. DO NOT use this tool if the admin asks to search or filter orders by delivery method, dates, or complex conditions. If they ask for complex filters, you MUST use 'runDatabaseAnalytics' instead.")
    public String manageOrders(
            @ToolParam(description = "Action: 'LIST' or 'UPDATE'") String action,
            @ToolParam(description = "The PSID of the customer (Required for LIST. If action is UPDATE, pass 'NONE')") String psid,
            @ToolParam(description = "The Order ID (required for UPDATE)") Long orderId,
            @ToolParam(description = "New status: 'PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED'") String status,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if ("LIST".equalsIgnoreCase(action)) {
            if (psid == null || psid.equals("NONE")) return "Error: No specific customer selected.";

            Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);
            if (contactOpt.isEmpty() || contactOpt.get().getCustomer() == null) {
                return "Error: No official customer profile linked to this chat.";
            }

            List<Order> orders = orderRepository.findAllByCustomerIdAndTenantId(contactOpt.get().getCustomer().getId(), tenantId);
            if (orders.isEmpty()) return "No orders found for this customer.";

            // --- UPGRADE: Build the TableData object for the UI ---
            List<String> columns = List.of("Order ID", "Date", "Status", "Total (BDT)");
            List<Map<String, Object>> rows = new ArrayList<>();

            for (Order o : orders) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("Order ID", "#" + o.getId());
                row.put("Date", o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate().toString() : "-");
                row.put("Status", o.getStatus() != null ? o.getStatus().name() : "UNKNOWN");
                row.put("Total (BDT)", o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0.00");
                rows.add(row);
            }

            // Save it to the ThreadLocal so ModeratorController picks it up and sends it to the frontend table UI
            AiAnalyticsResponse.TableData tableData = AiAnalyticsResponse.TableData.builder()
                    .columns(columns)
                    .rows(rows)
                    .build();
            currentTableData.set(tableData);

            return "Success. I found " + orders.size() + " orders. [SYSTEM NOTE: The UI is already rendering this data as a structured table. Do NOT list the orders in text. Write a brief 1-2 sentence conversational summary.]";

        } else if ("UPDATE".equalsIgnoreCase(action)) {
            if (orderId == null) return "Error: You must provide an Order ID to update it.";

            Optional<Order> orderOpt = orderRepository.findByIdAndTenantId(orderId, tenantId);
            if (orderOpt.isEmpty()) return "Error: Order #" + orderId + " not found in this shop.";

            try {
                OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
                Order order = orderOpt.get();
                order.setStatus(newStatus);
                orderRepository.save(order);
                return "Success. Inform the admin that Order #" + orderId + " was updated to " + newStatus;
            } catch (IllegalArgumentException e) {
                return "Error: '" + status + "' is not a valid order status.";
            }
        }
        return "Invalid action.";
    }

    // ==========================================
    // BULK ORDER MANAGEMENT
    // ==========================================

    @Tool(description = "CRITICAL: ONLY use this tool when the admin explicitly asks to update/cancel MULTIPLE orders based on a product name (e.g., 'Cancel all Macbooks'). Do NOT use this tool to just view or list orders, and DO NOT use it for complex filters like delivery method. Route complex listing requests to 'runDatabaseAnalytics'.")
    public String bulkManageOrders(
            @ToolParam(description = "The name or category of the product (e.g., 'Macbook', 'Viper Mini')") String productName,
            @ToolParam(description = "New status to apply: 'PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED'") String status,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if (productName == null || productName.trim().isEmpty()) {
            return "Error: You must provide a product name to perform a bulk update.";
        }

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Error: '" + status + "' is not a valid order status.";
        }

        // 1. Fetch all distinct orders containing this product
        List<Order> orders = orderRepository.findDistinctByItems_Product_BaseNameContainingIgnoreCaseAndTenantId(productName, tenantId);

        if (orders.isEmpty()) {
            return "No records found. Tell the admin there are no orders containing the product: " + productName;
        }

        // 2. State Machine Filtering: Skip orders that are already "done"
        List<Order> ordersToUpdate = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED)
                // Optional: If they ask to mark as SHIPPED, don't update orders that are ALREADY SHIPPED
                .filter(order -> order.getStatus() != newStatus)
                .collect(Collectors.toList());

        if (ordersToUpdate.isEmpty()) {
            return "Success, but no changes were made. All " + orders.size() + " orders containing '" + productName + "' are already completed (Delivered/Cancelled) or already have the status: " + newStatus;
        }

        // 3. Apply updates
        ordersToUpdate.forEach(order -> order.setStatus(newStatus));

        // 4. Bulk Save
        orderRepository.saveAll(ordersToUpdate);

        // 5. Return context-rich summary to the AI
        int skipped = orders.size() - ordersToUpdate.size();
        return String.format("Success. You bulk updated %d orders containing '%s' to %s. %d orders were skipped because they were already delivered, cancelled, or matched the target status. Summarize this nicely for the admin.",
                ordersToUpdate.size(), productName, newStatus, skipped);
    }


    // ==========================================
    // 5. STANDARD CRUD (CONTACTS)
    // ==========================================

    @Tool(description = "Creates a new CRM contact or updates an anonymous 'Facebook User' placeholder with their real name.")
    public String createContact(
            @ToolParam(description = "The PSID of the user") String psid,
            @ToolParam(description = "The actual name provided by the user") String name,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        if (psid == null || psid.equals("NONE")) return "Error: Cannot create contact without a PSID.";

        Optional<Contact> existingContact = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (existingContact.isPresent()) {
            Contact contact = existingContact.get();
            if ("Facebook User".equalsIgnoreCase(contact.getName())) {
                contact.setName(name);
                contactRepo.save(contact);
                return "Success. Inform the admin that the placeholder contact was updated to " + name;
            }
            return "Contact already exists with name: " + contact.getName();
        }

        Contact newContact = new Contact();
        newContact.setId(psid);
        newContact.setName(name);

        Tenant tenantRef = entityManager.getReference(Tenant.class, tenantId);
        newContact.setTenant(tenantRef);

        contactRepo.save(newContact);
        return "Success. Inform the admin that the new contact '" + name + "' was securely created.";
    }

    @Tool(description = "Updates an existing CRM contact's base name. Do NOT use this for tags or notes.")
    public String updateContact(
            @ToolParam(description = "The unique PSID of the contact to update") String psid,
            @ToolParam(description = "The new name") String name,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        if (psid == null || psid.equals("NONE")) return "Error: No customer selected to update.";

        Optional<Contact> existingContact = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (existingContact.isEmpty()) {
            return "Error: Could not find a contact with ID " + psid + " in this shop.";
        }

        Contact contact = existingContact.get();
        if (name != null && !name.trim().isEmpty()) {
            contact.setName(name);
            contactRepo.save(contact);
            return "Success. Tell the admin that the contact's name was successfully updated.";
        }
        return "No valid fields were provided to update.";
    }

}