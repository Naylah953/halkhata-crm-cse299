package com.example.demo.ai.tools;

import com.example.demo.domain.Contact;
import com.example.demo.domain.DraftOrder;
import com.example.demo.domain.Product;
import com.example.demo.domain.Tenant;
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.DraftOrderRepo;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.SseService;
import jakarta.persistence.EntityManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductTool {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    private DraftOrderRepo draftOrderRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SseService sseService;

    @Tool(description = "Lookup product price and availability by name. Use this when the customer asks about a product.")
    public String productLookup(
            @ToolParam(description = "The name of the product to search for") String query,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        // SECURE MULTI-TENANT READ
        List<Product> products = productRepo.findByTenantIdAndBaseNameContainingIgnoreCase(tenantId, query);

        if (products.isEmpty()) {
            return "I couldn't find any products matching '" + query + "'.";
        }

        return products.stream()
                .map(p -> String.format("- %s: %.2f BDT (%d in stock)",
                        p.getBaseName(), p.getPrice(), p.getQuantity()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Saves a draft order. Call this ONLY after successfully collecting the customer's name, phone, delivery address, and desired items.")
    public String draftOrder(
            @ToolParam(description = "The customer's actual name") String name,
            @ToolParam(description = "The customer's phone number") String phone,
            @ToolParam(description = "The delivery address") String address,
            @ToolParam(description = "A clear text summary of the items they want to buy (e.g., '2x Classic Summer T-Shirt')") String items,
            @ToolParam(description = "The PSID of the contact") String contactId,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(contactId, tenantId);
        if (contactOpt.isEmpty()) {
            return "Error: Contact not found in this shop.";
        }

        Contact contact = contactOpt.get();

        // Save the Draft
        DraftOrder draft = new DraftOrder();
        draft.setItemsSummary(items);
        draft.setProvidedName(name);
        draft.setProvidedPhone(phone);
        draft.setProvidedAddress(address);
        draft.setContact(contact);
        draft.setTenant(entityManager.getReference(Tenant.class, tenantId));

        draftOrderRepo.save(draft);

        // Instantly notify the frontend UI that a human needs to review this order
        contact.setOrderReady(true);
        contactRepo.save(contact);
        sseService.pushContactUpdateToTenant(tenantId, contact);

        return "Order draft saved successfully. Let the user know a human agent will review it shortly to confirm.";
    }
}