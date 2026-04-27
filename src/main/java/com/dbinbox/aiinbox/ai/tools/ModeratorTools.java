package com.dbinbox.aiinbox.ai.tools;

import com.dbinbox.aiinbox.model.Contact;
import com.dbinbox.aiinbox.repository.ContactRepo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ModeratorTools
{

    @Autowired
    private ContactRepo contactRepo;

    //check - redundant if contact creation is automatic upon message arrival
    @Tool(description = "Create a new contact or update a placeholder contact with a real name.")
    public String createContact(
            @ToolParam(description = "The PSID of the user") String psid,
            @ToolParam(description = "The actual name provided by the user") String name)
    {

        Optional<Contact> existingContact = contactRepo.findById(psid);

        if (existingContact.isPresent())
        {
            Contact contact = existingContact.get();

            // Check if the current name is the generic placeholder
            if ("Facebook User".equalsIgnoreCase(contact.getName()))
            {
                contact.setName(name);
                contactRepo.save(contact);
                return "Updated placeholder contact! User is now saved as: " + name;
            }

            return "Contact already exists with name: " + contact.getName();
        }

        // Standard creation if ID doesn't exist at all
        Contact newContact = new Contact(psid, name);
        contactRepo.save(newContact);
        return "Successfully created new contact: " + name;
    }


    @Tool(description = "Update an existing contact's details. Only provide fields that the user explicitly wants to change.")
    public String updateContact(
            @ToolParam(description = "The unique PSID of the contact to update") String psid,
            @ToolParam(description = "The new name (optional)") String name,
            @ToolParam(description = "The new address (optional)") String address,
            @ToolParam(description = "The new phone number (optional)") String phone) {

        Optional<Contact> existingContact = contactRepo.findById(psid);

        if (existingContact.isEmpty())
        {
            return "Error: Could not find a contact with ID " + psid + " to update.";
        }

        Contact contact = existingContact.get();

        // Partial Update Logic: Only update if the AI provides a non-null value
        if (name != null && !name.isBlank()) contact.setName(name);
        //if (address != null && !address.isBlank()) contact.setAddress(address);
        // Assuming your Contact model has a setPhone method
        // if (phone != null && !phone.isBlank()) contact.setPhone(phone);

        contactRepo.save(contact);
        return "Successfully updated contact: " + contact.getName();
    }

    @Tool(description = "Delete a contact from the CRM database.")
    public String deleteContact(@ToolParam(description = "The PSID of the contact to remove") String psid)
    {
        if (!contactRepo.existsById(psid)) {
            return "Error: Contact with ID " + psid + " does not exist.";
        }
        contactRepo.deleteById(psid);
        return "Contact " + psid + " has been deleted.";
    }
}