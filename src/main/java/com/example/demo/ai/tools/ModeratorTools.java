package com.example.demo.ai.tools;

import com.example.demo.domain.Contact;
import com.example.demo.domain.Tenant;
import com.example.demo.repository.ContactRepo;
import com.example.demo.service.AiAnalyticsService;
import com.example.demo.dto.AiAnalyticsResponse;
import com.example.demo.service.SseService;

import jakarta.persistence.EntityManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ModeratorTools {

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AiAnalyticsService aiAnalyticsService;

    @Autowired
    private SseService sseService;

    // --- SECURED NEW AI ACTION FLAG TOOLS ---

    @Tool(description = "Call this tool if the user asks a complex question you cannot answer, or specifically asks to speak to a human manager/support agent.")
    public String flagForHuman(
            @ToolParam(description = "The unique PSID of the contact to flag") String psid,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        // SECURE READ
        Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            contact.setRequiresHuman(true);
            contactRepo.save(contact);

            // FIRE REAL-TIME UI UPDATE
            sseService.pushContactUpdateToTenant(tenantId, contact);

            return "Successfully flagged this conversation for human intervention.";
        }
        return "Error: Could not find contact in this shop.";
    }

    @Tool(description = "Call this tool when you have successfully collected the user's desired product, size, color, and delivery address, and are ready for a human to finalize the order.")
    public String markOrderReady(
            @ToolParam(description = "The unique PSID of the contact whose order is ready") String psid,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        // SECURE READ
        Optional<Contact> contactOpt = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            contact.setOrderReady(true);
            contactRepo.save(contact);

            // FIRE REAL-TIME UI UPDATE
            sseService.pushContactUpdateToTenant(tenantId, contact);

            return "Successfully marked this conversation as having an order ready for review.";
        }
        return "Error: Could not find contact in this shop.";
    }

    // --- EXISTING TOOLS BELOW ---

    @Tool(description = "Use this tool ANYTIME the user asks for analytics, sales data, product inventory, order history, or complex statistics. Pass their exact question as the prompt.")
    public String runDatabaseAnalytics(
            @ToolParam(description = "The exact question the user asked about their data") String prompt,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        System.out.println("Manager AI is delegating a complex query to the Analytics Specialist...");

        AiAnalyticsResponse response = aiAnalyticsService.processAnalyticsQuery(prompt, tenantId);

        if (response.isTable()) {
            String rawData = response.getTableData().getRows().toString();

            return "Raw Database Results: " + rawData +
                    " \n\n[SYSTEM NOTE: The frontend is already rendering this exact data as a visual table for the user. Your job is to read the raw results above and write a brief, insightful summary (2-3 sentences) highlighting the key takeaways. Do not list all the raw data out, just provide the human-readable analysis.]";
        } else {
            return "Analytics Result: " + response.getAiSummary();
        }
    }

    @Tool(description = "Create a new contact or update a placeholder contact with a real name.")
    public String createContact(
            @ToolParam(description = "The PSID of the user") String psid,
            @ToolParam(description = "The actual name provided by the user") String name,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

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

        Contact newContact = new Contact();
        newContact.setId(psid);
        newContact.setName(name);

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
}