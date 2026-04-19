package com.example.demo.service;

import com.example.demo.dto.UserFacebookProfile;
import com.example.demo.dto.MessengerWebhookPayload;
import com.example.demo.domain.Contact;
import com.example.demo.domain.Message;
import com.example.demo.domain.Tenant;
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.MessageRepo;
import com.example.demo.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class InboundMessageService {

    @Autowired private ContactRepo contactRepo;
    @Autowired private MessageRepo messageRepo;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RestTemplate restTemplate;

    @Value("${meta.api.base-url}")
    private String fbBaseUrl;

    public void processInboundMessage(MessengerWebhookPayload payload) {
        if (payload.entry() == null) return;

        for (MessengerWebhookPayload.MessengerEntry entry : payload.entry()) {
            if (entry.messageEventList() == null) continue;

            // DYNAMIC ROUTING: Find the shop using the recipient Page ID
            String pageId = entry.id();
            Optional<Tenant> tenantOpt = tenantRepository.findByFacebookPageId(pageId);

            if (tenantOpt.isEmpty()) {
                System.err.println("Webhook received for unknown Facebook Page ID: " + pageId);
                continue; // Skip messages sent to unregistered pages
            }

            Tenant tenant = tenantOpt.get();
            String pageAccessToken = tenant.getPageAccessToken();

            for (MessengerWebhookPayload.MessageEvent event : entry.messageEventList()) {
                if (event.message() == null) continue;

                String senderId = event.sender().id();
                String mid = event.message().mid();

                // Pass the specific tenant and their PAT to securely process the contact
                Contact contact = getOrCreateContact(senderId, tenant, pageAccessToken);

                if (event.message().text() != null) {
                    saveMessage(contact, event.message().text(), "text", mid);
                }

                if (event.message().attachments() != null) {
                    for (MessengerWebhookPayload.Attachment attachment : event.message().attachments()) {
                        saveMessage(contact, attachment.payload().url(), attachment.type(), mid);
                    }
                }
            }
        }
    }

    private Contact getOrCreateContact(String senderId, Tenant tenant, String pat) {
        // Only fetch a contact if it belongs to THIS specific shop
        return contactRepo.findByIdAndTenantId(senderId, tenant.getId())
                .orElseGet(() -> {
                    Contact newContact = new Contact();
                    newContact.setId(senderId);

                    // LOCK TO TENANT!
                    newContact.setTenant(tenant);

                    UserFacebookProfile profile = fetchProfileFromMeta(senderId, pat);
                    newContact.setName(parseNameFromProfile(profile));
                    newContact.setMessageList(new ArrayList<>());

                    return contactRepo.save(newContact);
                });
    }

    private UserFacebookProfile fetchProfileFromMeta(String senderId, String pat) {
        try {
            String url = String.format("%s%s?fields=first_name,last_name&access_token=%s", fbBaseUrl, senderId, pat);
            return restTemplate.getForObject(url, UserFacebookProfile.class);
        } catch (Exception e) {
            System.err.println("Error fetching profile from Meta.");
            return null;
        }
    }

    private String parseNameFromProfile(UserFacebookProfile profile) {
        if (profile != null && profile.first_name() != null) {
            return profile.first_name() + " " + (profile.last_name() != null ? profile.last_name() : "");
        }
        return "Facebook User";
    }

    private void saveMessage(Contact contact, String content, String type, String mid) {
        Message newMessage = new Message();
        newMessage.setContent(content);
        newMessage.setMetaMid(mid);
        newMessage.setMessageType(type);
        newMessage.setContact(contact);
        newMessage.setDirection(Message.Direction.INBOUND);
        newMessage.setSenderType(Message.SenderType.USER);

        messageRepo.save(newMessage);
        System.out.println("Saved inbound message for shop: " + contact.getTenant().getName());
    }
}