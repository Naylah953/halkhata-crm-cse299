package com.dbinbox.aiinbox.ai.tools;

import com.dbinbox.aiinbox.model.Contact;
import com.dbinbox.aiinbox.model.Order;
import com.dbinbox.aiinbox.model.Product;
import com.dbinbox.aiinbox.repository.ContactRepo;
import com.dbinbox.aiinbox.repository.OrderRepo;
import com.dbinbox.aiinbox.repository.ProductRepo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductTool
{
        @Autowired
        private ProductRepo productRepo;

        @Autowired
        private ContactRepo contactRepo;

        @Autowired
        private OrderRepo orderRepo;


        @Tool(description = "Lookup product price and availability by name")
        public String productLookup(String query)
        {
            System.out.println("DEBUG 1: Entering Tool with query: " + query);

            // 1. Explicitly define List<Product> instead of using var or raw List
            List<Product> products = productRepo.findByNameContainingIgnoreCase(query);
            System.out.println("DEBUG 2: DB found " + products.size() + " items");

            // 2. Now Java knows that 'products' is a List, so .isEmpty() works
            if (products.isEmpty()) {
                return "I couldn't find any products matching '" + query + "'.";
            }

            // 3. Since Java knows the list contains 'Product' objects,
            // it can find getName(), getPrice(), etc.
            System.out.println("DEBUG 3: Returning to AI: ");
            return products.stream()
                    .map(p -> String.format("- %s: %.2f BDT (%d in stock)",
                            p.getName(), p.getPrice(), p.getQuantity()))
                    .collect(Collectors.joining("\n"));
        }

    @Tool(description = "Saves a draft order. Call this after collecting name, phone, and items.")
    public String draftOrder(String name, String phone, String items, String contactId)
    {
        System.out.println("DEBUG: Processing order for Contact ID: " + contactId);

        // 1. Update Contact Details
        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        //contact.setName(name);
        //contact.setPhoneNumber(phone);
        //contactRepo.save(contact); // Persist the identity info

        // 2. Create the Order
        Order order = new Order();
        order.setContact(contact);
        order.setOrderSummary(items);
        order.setStatus("PENDING_CONFIRMATION");
        orderRepo.save(order);

        return "Order for " + name + " saved and linked to contact profile.";
    }
}