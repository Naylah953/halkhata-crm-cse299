package com.example.demo.ai.tools;

import com.example.demo.domain.*;
import com.example.demo.domain.enums.DeliveryMethod;
import com.example.demo.domain.enums.PaymentMethod;
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.DraftOrderRepo;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.SseService;
import jakarta.persistence.EntityManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductTool {

    @Autowired private ProductRepository productRepo;
    @Autowired private ContactRepo contactRepo;
    @Autowired private DraftOrderRepo draftOrderRepo;
    @Autowired private EntityManager entityManager;
    @Autowired private SseService sseService;

    // --- NEW: Record for dynamic JSON item mapping from the AI ---
    public record AiDraftItem(
            @ToolParam(description = "The ID of the product") Long productId,
            @ToolParam(description = "The quantity the user wants") Integer quantity
    ) {}

    @Tool(description = "Lookup products in the shop. Use this BEFORE drafting an order to find the exact Product ID, Price, and Stock availability.")
    public String productLookup(
            @ToolParam(description = "The core product name. ALWAYS use singular nouns and root keywords (e.g., use 'shirt' instead of 'shirts').") String query,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        System.out.println("AI is searching inventory for: " + query);

        List<Product> products = productRepo.findByTenantIdAndBaseNameContainingIgnoreCase(tenantId, query);

        if (products.isEmpty()) {
            products = productRepo.findByTenantIdAndSchema_NameContainingIgnoreCase(tenantId, query);
        }

        if (products.isEmpty()) {
            return "Sorry, we don't have any products or categories matching '" + query + "'.";
        }

        StringBuilder result = new StringBuilder("I found the following items:\n");
        for (Product p : products) {
            result.append(String.format("- ID: %d | Name: %s | Price: %.2f BDT | Stock: %d\n",
                    p.getId(), p.getBaseName(), p.getPrice(), p.getQuantity()));
        }

        if (products.size() > 1) {
            result.append("\nPlease ask the user which specific one they would like to order.");
        }

        return result.toString();
    }

    /**
     * MUST READ: INSTRUCTION FOR FUTURE DEVELOPERS
     * * This is a test tool for AI-driven order drafting.
     * DO NOT REMOVE the 30-second debounce logic or the early return constraints.
     */
    @Tool(description = "Creates a NEW order draft. Call this ONLY AFTER the user has explicitly given you their real phone number and delivery address. NEVER invent or use placeholder values.")
    public String draftOrder(
            @ToolParam(description = "The list of products the customer is ordering") List<AiDraftItem> items,
            @ToolParam(description = "The REAL phone number provided by the customer. Do not guess or use placeholders.") String phoneNumber,
            @ToolParam(description = "The REAL delivery address provided by the customer. Do not guess or use placeholders.") String address,
            @ToolParam(description = "The PSID of the contact") String contactId,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        Contact contact = contactRepo.findByIdAndTenantId(contactId, tenantId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        List<DraftOrder> existingDrafts = draftOrderRepo.findByContactIdAndTenantIdAndStatus(
                contactId, tenantId, DraftOrder.DraftStatus.PENDING);

        // 1. THE DEBOUNCE BLOCK (Prevents parallel LLM spam)
        for (DraftOrder existing : existingDrafts) {
            if (existing.getCreatedAt() != null &&
                    existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(30))) {

                System.out.println("Blocked parallel LLM duplicate draft creation.");
                return "Duplicate request detected. Action aborted. Do not retry this tool. Please inform the user their order is pending review.";
            }
        }

        // 2. Strict Initialization
        DraftOrder draft = new DraftOrder();
        draft.setContact(contact);
        draft.setTenant(contact.getTenant());
        draft.setStatus(DraftOrder.DraftStatus.PENDING);

        // Dynamically applying the AI's parsed customer details
        draft.setProvidedName(contact.getName());
        draft.setProvidedPhone(phoneNumber);
        draft.setProvidedAddress(address);
        draft.setDeliveryMethod(DeliveryMethod.PATHAO);
        draft.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        // 3. THE DYNAMIC ITEM LOOP (WITH FRIEND'S STOCK CHECK)
        for (AiDraftItem aiItem : items) {
            Product product = productRepo.findByIdAndTenantId(aiItem.productId(), tenantId)
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + aiItem.productId()));

            // FRIEND'S UPGRADE: Check if the shop actually has enough stock!
            if (product.getQuantity() < aiItem.quantity()) {
                return "SYSTEM HALT: Only " + product.getQuantity() + " units of '" + product.getBaseName() +
                        "' are in stock. Execution aborted. Ask the customer to reduce their quantity or choose another item.";
            }

            DraftOrderItem orderItem = new DraftOrderItem();
            orderItem.setProductId(aiItem.productId());
            orderItem.setQuantity(aiItem.quantity());
            draft.addDraftItem(orderItem);
        }

        draftOrderRepo.save(draft);

        // 4. Update realtime UI state
        contact.setOrderReady(true);
        contactRepo.save(contact);
        sseService.pushContactUpdateToTenant(tenantId, contact);

        return "SUCCESS: New order draft created. EXECUTION COMPLETE. Do not call this tool again for current items. Let the user know that an agent will confirm their order.";
    }


    //IZAZ'S WORKING VERSION
    /*@Tool(description = "Flags the conversation for human intervention. Call this if the user asks to speak to a human or agent. Return a summarised context of the last 5 messages.")
    public String requestHuman(
            @ToolParam(description = "The PSID of the contact") String contactId,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        Contact contact = contactRepo.findByIdAndTenantId(contactId, tenantId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        contact.setRequiresHuman(true);
        contactRepo.save(contact);
        sseService.pushContactUpdateToTenant(tenantId, contact);

        return "Human agent requested successfully. Inform the user someone will be with them shortly.";
    }*/

    @Tool(description = "Flags for human help. The AI will summarize the context for the agent.")
    public String requestHuman(
            @ToolParam(description = "The summarized context of the conversation so far") String aiSummary,
            @ToolParam(description = "The PSID of the contact") String contactId,
            @ToolParam(description = "The tenant ID") Long tenantId) {

        Contact contact = contactRepo.findByIdAndTenantId(contactId, tenantId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        // 1. Mark for human and save the AI-generated summary
        contact.setRequiresHuman(true);
        contact.setAdminBriefing(aiSummary); // This is the AI's helpful summary
        contactRepo.save(contact);

        // 2. Push to your teammate's SSE Dashboard
        // The moderator sees: "User needs help. Context: [Briefing]"
        sseService.pushContactUpdateToTenant(tenantId, contact);
        System.out.println("DEBUG - AI Summary for Admin: " + aiSummary);

        return "Summary sent to moderator. Let the user know help is on the way.";
    }
}