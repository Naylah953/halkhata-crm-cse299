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

    // --- EXISTING TOOLS BELOW ---

    @Tool(description = "CRITICAL: Use this tool whenever the user asks about sales, products, inventory, revenue, or customer lists. Pass their exact question. DO NOT use this for conversational chat.")
    public String runDatabaseAnalytics(
            @ToolParam(description = "The exact question the user asked about their data") String prompt,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        System.out.println("Manager AI is delegating a complex query to the Analytics Specialist...");

        AiAnalyticsResponse response = aiAnalyticsService.processAnalyticsQuery(prompt, tenantId);

        if (response.isTable()) {
            String rawData = response.getTableData().getRows().toString();

            return "Raw Database Results: " + rawData +
                    " \n\n[SYSTEM NOTE: The UI is already showing this data as a table. Write a brief 2-sentence summary highlighting the most interesting metric from this data. Do not list everything.]";
        } else {
            return "Analytics Result: " + response.getAiSummary();
        }
    }

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

    @Tool(description = "Updates an existing CRM contact. You can update their name, phone number, or address.")
    public String updateContact(
            @ToolParam(description = "The unique PSID of the contact to update") String psid,
            @ToolParam(description = "The new name (leave empty if not updating)") String name,
            @ToolParam(description = "The new phone number (leave empty if not updating)") String phone,
            @ToolParam(description = "The new address (leave empty if not updating)") String address,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        if (psid == null || psid.equals("NONE")) return "Error: No customer selected to update.";

        Optional<Contact> existingContact = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (existingContact.isEmpty()) {
            return "Error: Could not find a contact with ID " + psid + " in this shop.";
        }

        Contact contact = existingContact.get();
        boolean updated = false;

        if (name != null && !name.trim().isEmpty()) {
            contact.setName(name);
            updated = true;
        }

        // Uncomment these if your Contact entity has these fields!
        /*
        if (phone != null && !phone.trim().isEmpty()) {
            contact.setPhone(phone);
            updated = true;
        }
        if (address != null && !address.trim().isEmpty()) {
            contact.setAddress(address);
            updated = true;
        }
        */

        if (updated) {
            contactRepo.save(contact);
            return "Success. Tell the admin that the contact's details were successfully updated.";
        } else {
            return "No valid fields were provided to update.";
        }
    }

    @Tool(description = "Deletes a contact from the CRM database. Use with caution.")
    public String deleteContact(
            @ToolParam(description = "The PSID of the contact to remove") String psid,
            @ToolParam(description = "The tenant ID of the current shop") Long tenantId) {

        if (psid == null || psid.equals("NONE")) return "Error: No customer selected to delete.";

        Optional<Contact> contact = contactRepo.findByIdAndTenantId(psid, tenantId);

        if (contact.isEmpty()) {
            return "Error: Contact with ID " + psid + " does not exist in this shop.";
        }

        contactRepo.delete(contact.get());
        return "Success. Inform the admin that the contact has been permanently deleted.";
    }
}