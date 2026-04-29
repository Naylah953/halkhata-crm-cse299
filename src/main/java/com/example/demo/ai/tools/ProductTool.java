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

    @Tool(description = "Lookup product price and availability by name. Use this when the customer asks about a product.")
    public String productLookup(
            @ToolParam(description = "The name of the product to search for") String query,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        List<Product> products = productRepo.findByTenantIdAndBaseNameContainingIgnoreCase(tenantId, query);
        if (products.isEmpty()) return "I couldn't find any products matching '" + query + "'.";

        return products.stream()
                .map(p -> String.format("- %s: %.2f BDT (%d in stock)", p.getBaseName(), p.getPrice(), p.getQuantity()))
                .collect(Collectors.joining("\n"));
    }

        /* --- COMMENTED OUT FOR GENERIC DUMMY TESTING ---

    @Tool(description = "Saves a draft order. Call this ONLY after successfully collecting the customer's name, phone, delivery address, and desired items.")
    public String draftOrder(

            @ToolParam(description = "The customer's actual name") String name,
            @ToolParam(description = "The customer's phone number") String phone,
            @ToolParam(description = "The delivery address") String address,
            @ToolParam(description = "A clear text summary of the items they want to buy") String items,
            @ToolParam(description = "The PSID of the contact") String contactId,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) { ... }

    */

    /**
     * MUST READ: INSTRUCTION FOR FUTURE DEVELOPERS
     * * This is a test tool for AI-driven order drafting.
     * DO NOT REMOVE the 30-second debounce logic or the early return constraints.
     * * Context: Generative LLMs (e.g., Llama 3, Gemini) natively execute independent
     * tasks via parallel tool calling within a single conversational turn. If the
     * model hallucinates or attempts a self-correction loop, it can fire multiple
     * identical requests simultaneously (e.g., 7 parallel requests in < 1 second).
     * * - The 30-second debounce guarantees idempotency against parallel spam, preventing
     * duplicate database row creation and maintaining schema integrity.
     */
    @Tool(description = "Creates a NEW order draft. Call this strictly ONCE per order confirmation.")
    public String draftOrder(
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

        // 2. Strict Insertion Logic
        DraftOrder draft = new DraftOrder();
        draft.setContact(contact);
        draft.setTenant(contact.getTenant());
        draft.setStatus(DraftOrder.DraftStatus.PENDING);

        draft.setProvidedName(contact.getName());
        draft.setProvidedPhone("01700000003");
        draft.setProvidedEmail("izaz@example.com");
        draft.setProvidedAddress("Dhanmondi, Dhaka");
        draft.setDeliveryMethod(DeliveryMethod.PATHAO);
        draft.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        // --- RESTORED DUMMY PAYLOAD LINE ITEM ---
        // Item 1
        DraftOrderItem item1 = new DraftOrderItem();
        item1.setProductId(1L);
        item1.setQuantity(1);
        draft.addDraftItem(item1);

        // Item 2
        DraftOrderItem item2 = new DraftOrderItem();
        item2.setProductId(1L);
        item2.setQuantity(2);
        draft.addDraftItem(item2);

        draftOrderRepo.save(draft);

        // 3. Update realtime UI state
        contact.setOrderReady(true);
        contactRepo.save(contact);
        sseService.pushContactUpdateToTenant(tenantId, contact);

        return "SUCCESS: New order draft created. EXECUTION COMPLETE. Do not call this tool again for current items. Let the user know that an agent will confirm their order.";
    }


    @Tool(description = "Flags the conversation for human intervention. Call this if the user asks to speak to a human or agent.")
    public String requestHuman(
            @ToolParam(description = "The PSID of the contact") String contactId,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        Contact contact = contactRepo.findByIdAndTenantId(contactId, tenantId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        contact.setRequiresHuman(true);
        contactRepo.save(contact);
        sseService.pushContactUpdateToTenant(tenantId, contact);

        return "Human agent requested successfully. Inform the user someone will be with them shortly.";
    }
}