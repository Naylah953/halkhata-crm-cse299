package com.example.demo.ai.tools;

import com.example.demo.domain.Contact;
import com.example.demo.domain.Tenant;
import com.example.demo.domain.Order;
import com.example.demo.domain.Complaint;
import com.example.demo.domain.Task;
import com.example.demo.domain.Message;
import com.example.demo.domain.enums.DeliveryMethod;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.domain.enums.PaymentMethod;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ComplaintRepo;
import com.example.demo.repository.TaskRepo;
import com.example.demo.repository.MessageRepo;
import com.example.demo.service.AiAnalyticsService;
import com.example.demo.dto.AiAnalyticsResponse;

import jakarta.persistence.EntityManager;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ModeratorTools {

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    private EntityManager entityManager; // Used to safely fetch the Tenant reference

    @Autowired
    private AiAnalyticsService aiAnalyticsService;

    @Autowired
    private MessageRepo messageRepo;

    // --- THE ANALYTICS BRIDGE TOOL ---
    // --- THE ANALYTICS BRIDGE TOOL ---
    @Tool(description = "Use this tool ANYTIME the user asks for analytics, sales data, product inventory, order history, or complex statistics. Pass their exact question as the prompt.")
    public String runDatabaseAnalytics(
            @ToolParam(description = "The exact question the user asked about their data") String prompt,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        System.out.println("Manager AI is delegating a complex query to the Analytics Specialist...");

        AiAnalyticsResponse response = aiAnalyticsService.processAnalyticsQuery(prompt, tenantId);

        if (response.isTable()) {
            // Extract the raw data rows so the Manager AI can read them
            String rawData = response.getTableData().getRows().toString();

            return "Raw Database Results: " + rawData +
                    " \n\n[SYSTEM NOTE: The frontend is already rendering this exact data as a visual table for the user. Your job is to read the raw results above and write a brief, insightful summary (2-3 sentences) highlighting the key takeaways. Do not list all the raw data out, just provide the human-readable analysis.]";
        } else {
            return "Analytics Result: " + response.getAiSummary();
        }
    }

    // --- Fariza's ADAPTED CRM TOOLS ---
    @Tool(description = "Create a new contact or update a placeholder contact with a real name.")
    public String createContact(
            @ToolParam(description = "The PSID of the user") String psid,
            @ToolParam(description = "The actual name provided by the user") String name,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        // SECURE READ: Ensure we only look in this specific shop
        Optional<Contact> existingContact = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (existingContact.isPresent()) {
            Contact contact = existingContact.get();
            if ("Facebook User".equalsIgnoreCase(contact.getName())) {
                contact.setName(name);
                contactRepo.save(contact);
                return "Updated placeholder contact! User is now saved as: " + name;
            }
            return "Contact already exists with name: " + contact.getName();
        }

        // SECURE WRITE: Link the new contact to the current Tenant
        Contact newContact = new Contact();
        newContact.setId(psid);
        newContact.setName(name);

        // Get a proxy reference to the Tenant without hitting the DB
        Tenant tenantRef = entityManager.getReference(Tenant.class, tenantId);
        newContact.setTenant(tenantRef);

        contactRepo.save(newContact);
        return "Successfully created new contact: " + name;
    }

    @Tool(description = "Update an existing contact's details.")
    public String updateContact(
            @ToolParam(description = "The unique PSID of the contact to update") String psid,
            @ToolParam(description = "The new name") String name,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        Optional<Contact> existingContact = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (existingContact.isEmpty()) {
            return "Error: Could not find a contact with ID " + psid + " in this shop to update.";
        }

        Contact contact = existingContact.get();
        if (name != null && !name.isBlank()) {
            contact.setName(name);
        }

        contactRepo.save(contact);
        return "Successfully updated contact: " + contact.getName();
    }

    @Tool(description = "Delete a contact from the CRM database.")
    public String deleteContact(
            @ToolParam(description = "The PSID of the contact to remove") String psid,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        Optional<Contact> contact = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (contact.isEmpty()) {
            return "Error: Contact with ID " + psid + " does not exist in this shop.";
        }

        contactRepo.delete(contact.get());
        return "Contact " + psid + " has been safely deleted from this shop.";
    }

    // ==========================================
    // Naylah'sCRM Tools
    // ==========================================

    // --- 1. THE CUSTOMER INTELLIGENCE TOOL ---
    @Autowired
    private OrderRepository orderRepository;

    @Tool(description = "Get a complete dossier of a customer, including their notes, tags, and total spending history.")
    public String getCustomerDossier(
            @ToolParam(description = "The PSID of the contact") String psid,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (contactOpt.isEmpty()) {
            return "Error: Contact not found.";
        }

        Contact contact = contactOpt.get();
        double totalSpent = 0.0;

        // Calculate spending if a customer profile is linked
        if (contact.getCustomer() != null) {
            List<Order> orders = orderRepository.findAllByCustomerIdAndTenantId(contact.getCustomer().getId(), tenantId);
            totalSpent = orders.stream()
                    .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0)
                    .sum();
        }

        return String.format(
                "CUSTOMER DOSSIER:\n" +
                        "- Name: %s\n" +
                        "- Tags: %s\n" +
                        "- Notes: %s\n" +
                        "- Total Lifetime Spending: $%.2f\n" +
                        "- Linked Customer ID: %s",
                contact.getName(),
                (contact.getTags() != null ? contact.getTags() : "None"),
                (contact.getNotes() != null ? contact.getNotes() : "No notes yet"),
                totalSpent,
                (contact.getCustomer() != null ? contact.getCustomer().getId() : "No profile linked")
        );
    }

    // --- 2. THE SMART NOTE & TAGGING TOOL ---
    @Tool(description = "Update a customer's notes or tags (e.g., VIP status, delivery preferences).")
    public String updateCustomerMeta(
            @ToolParam(description = "The PSID of the contact") String psid,
            @ToolParam(description = "The content to add (the note or the tag name)") String content,
            @ToolParam(description = "Type of update: 'NOTE' or 'TAG'") String type,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (contactOpt.isEmpty()) {
            return "Error: Contact not found.";
        }

        Contact contact = contactOpt.get();
        if ("TAG".equalsIgnoreCase(type)) {
            contact.setTags(content); // Overwrites or appends tag
        } else {
            contact.setNotes(content); // Overwrites or appends note
        }

        contactRepo.save(contact);
        return "Successfully updated " + type + " for " + contact.getName();
    }

    // --- 3. THE ORDER MANAGEMENT TOOL ---
    @Tool(description = "Manage customer orders. Actions: 'LIST' or 'UPDATE'.")
    public String manageOrders(
            @ToolParam(description = "Action: 'LIST' or 'UPDATE'") String action,
            @ToolParam(description = "The PSID of the customer (required for LIST)") String psid,
            @ToolParam(description = "The Order ID (required for UPDATE)") Long orderId,
            @ToolParam(description = "New status: 'PENDING', 'SHIPPED', 'DELIVERED', etc.") String status,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if ("LIST".equalsIgnoreCase(action)) {
            Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);
            if (contactOpt.isEmpty() || contactOpt.get().getCustomer() == null) {
                return "Error: No customer profile linked to this PSID.";
            }

            List<Order> orders = orderRepository.findAllByCustomerIdAndTenantId(contactOpt.get().getCustomer().getId(), tenantId);
            if (orders.isEmpty()) return "No orders found for this customer.";

            return orders.stream()
                    .map(o -> "Order #" + o.getId() + " - Status: " + o.getStatus() + " - Total: $" + o.getTotalAmount())
                    .collect(Collectors.joining("\n"));

        } else if ("UPDATE".equalsIgnoreCase(action)) {
            Optional<Order> orderOpt = orderRepository.findByIdAndTenantId(orderId, tenantId);
            if (orderOpt.isEmpty()) return "Error: Order #" + orderId + " not found.";

            try {
                // Convert the String from AI (e.g., "shipped") to the actual Enum
                OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
                Order order = orderOpt.get();
                order.setStatus(newStatus); // No more red underline!
                orderRepository.save(order);
                return "Successfully updated Order #" + orderId + " to " + newStatus;
            } catch (IllegalArgumentException e) {
                return "Error: '" + status + "' is not a valid order status.";
            }
        }
        return "Invalid action.";
    }

    // --- 4. THE COMPLAINT & RESOLUTION TOOL ---
    @Autowired
    private ComplaintRepo complaintRepo;

    @Tool(description = "Manage customer complaints. Actions: 'LOG' (new issue) or 'LIST' (see all open issues).")
    public String handleComplaints(
            @ToolParam(description = "Action to perform: 'LOG' or 'LIST'") String action,
            @ToolParam(description = "PSID of the customer (required for LOG)") String psid,
            @ToolParam(description = "Description of the problem") String description,
            @ToolParam(description = "Priority: HIGH, MEDIUM, LOW") String priority,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if ("LOG".equalsIgnoreCase(action)) {
            Complaint complaint = new Complaint();
            complaint.setPsid(psid);
            complaint.setDescription(description);
            complaint.setPriority(priority != null ? priority.toUpperCase() : "MEDIUM");
            complaint.setStatus("OPEN");
            complaint.setTenantId(tenantId);
            complaintRepo.save(complaint);
            return "Logged new HIGH priority complaint for PSID: " + psid;
        } else {
            List<Complaint> openIssues = complaintRepo.findByTenantIdAndStatus(tenantId, "OPEN");
            if (openIssues.isEmpty()) return "No open complaints for this shop.";

            return openIssues.stream()
                    .map(c -> "[" + c.getPriority() + "] " + c.getDescription() + " (ID: " + c.getId() + ")")
                    .collect(Collectors.joining("\n"));
        }
    }

    // --- 5. THE TASK & FOLLOW-UP TOOL ---
    @Autowired
    private TaskRepo taskRepo;

    @Tool(description = "Manage moderator tasks and follow-ups. Actions: 'CREATE' or 'LIST'.")
    public String manageTasks(
            @ToolParam(description = "Action: 'CREATE' or 'LIST'") String action,
            @ToolParam(description = "Task description") String text,
            @ToolParam(description = "Customer PSID (optional)") String psid,
            @ToolParam(description = "Days from now to set due date (e.g., '1' for tomorrow)") Integer daysOut,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        if ("CREATE".equalsIgnoreCase(action)) {
            Task task = new Task();
            task.setDescription(text);
            task.setPsid(psid);
            task.setTenantId(tenantId);
            task.setDueDate(LocalDateTime.now().plusDays(daysOut != null ? daysOut : 1));
            taskRepo.save(task);
            return "Task created: " + text + " (Due in " + (daysOut != null ? daysOut : 1) + " days)";
        } else {
            List<Task> tasks = taskRepo.findByTenantIdAndCompletedFalse(tenantId);
            if (tasks.isEmpty()) return "No pending tasks!";

            return tasks.stream()
                    .map(t -> "- " + t.getDescription() + " (Due: " + t.getDueDate().toLocalDate() + ")")
                    .collect(Collectors.joining("\n"));
        }
    }

    // --- 6. THE SHOP SNAPSHOT TOOL ---
    @Tool(description = "Get a quick snapshot of shop performance (total revenue and order count).")
    public String getShopPerformance(
            @ToolParam(description = "The timeframe to check: 'TODAY', 'WEEK', or 'MONTH'") String timeframe,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        List<Order> allOrders = orderRepository.findAllByTenantId(tenantId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit;

        // Filter based on timeframe
        if ("WEEK".equalsIgnoreCase(timeframe)) {
            limit = now.minusWeeks(1);
        } else if ("MONTH".equalsIgnoreCase(timeframe)) {
            limit = now.minusMonths(1);
        } else {
            limit = now.toLocalDate().atStartOfDay(); // Default to Today
        }

        List<Order> filteredOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(limit))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = filteredOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format(
                "SHOP PERFORMANCE (%s):\n" +
                        "- Total Orders: %d\n" +
                        "- Total Revenue: $%.2f",
                timeframe.toUpperCase(),
                filteredOrders.size(),
                totalRevenue
        );
    }

    // --- 7. THE COMMUNICATION HISTORY TOOL ---
    @Tool(description = "Pulls the recent chat logs to help understand the context of a customer's issue.")
    public String getDetailedChatLogs(
            @ToolParam(description = "The PSID of the customer") String psid,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        // Uses the new specific method we added to the Repo
        List<Message> recentMessages = messageRepo.findTop10ByContact_IdAndContact_Tenant_IdOrderByCreatedAtDesc(psid, tenantId);

        if (recentMessages.isEmpty()) return "No recent communication found for this customer.";

        return "Recent Chat Logs:\n" + recentMessages.stream()
                .map(m -> {
                    String sender = (m.getContact() != null) ? m.getContact().getName() : "Customer";
                    return sender + ": " + m.getContent();
                })
                .collect(Collectors.joining("\n"));
    }
    // ==========================================
}