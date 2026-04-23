package com.example.demo.ai.tools;

import com.example.demo.domain.Contact;
import com.example.demo.domain.Tenant;
import com.example.demo.repository.ContactRepo;
import com.example.demo.service.AiAnalyticsService;
import com.example.demo.dto.AiAnalyticsResponse;

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
    private EntityManager entityManager; // Used to safely fetch the Tenant reference

    @Autowired
    private AiAnalyticsService aiAnalyticsService;

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
}