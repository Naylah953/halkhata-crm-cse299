package com.dbinbox.aiinbox.service;

//import com.dbinbox.aiinbox.ai.assistant.AiAssistantService;
import com.dbinbox.aiinbox.dto.UserFacebookProfile;
import com.dbinbox.aiinbox.dto.MessengerWebhookPayload;
import com.dbinbox.aiinbox.model.Contact;
import com.dbinbox.aiinbox.model.Message;
import com.dbinbox.aiinbox.repository.ContactRepo;
import com.dbinbox.aiinbox.repository.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class InboundMessageService
{

    //change
    @Autowired
    private ContactRepo contactRepo;

    //change
    @Autowired
    private MessageRepo messageRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    //private AiAssistantService aiAssistantService; // Inject the brain


    //take from DB
    @Value("${meta.api.pat}")
    private String pageAccessToken;

    @Value("${meta.api.base-url}")
    private String fbBaseUrl;

    // REMOVED: class-level messageContent and senderId to prevent multi-user bugs

    public String processInboundMessage(MessengerWebhookPayload payload)
    {
        if (payload.entry() == null) return null;

        String aiResponse = null;

        for (MessengerWebhookPayload.MessengerEntry entry : payload.entry()) {
            if (entry.messageEventList() == null) continue;

            for (MessengerWebhookPayload.MessageEvent event : entry.messageEventList()) {
                if (event.message() == null) continue;

                String currentSenderId = event.sender().id();
                String mid = event.message().mid();

                Contact contact = getOrCreateContact(currentSenderId);

                // 1. Handle Text
                if (event.message().text() != null)
                {
                    String userText = event.message().text();
                    saveMessage(contact, userText, "text", mid);

                    // GET AI RESPONSE HERE
                    //aiResponse = aiAssistantService.getResponse(currentSenderId, userText);
                }

                // 2. Handle Attachments
                if (event.message().attachments() != null) {
                    for (MessengerWebhookPayload.Attachment attachment : event.message().attachments()) {
                        saveMessage(contact, attachment.payload().url(), attachment.type(), mid);
                    }
                }
            }
        }
        return "Thanks for messaging us..."; // Return this so the Controller can send it back to Meta
    }

    // ... (rest of your existing private methods stay the same)


    private Contact getOrCreateContact(String senderId) {
        return contactRepo.findById(senderId)
                .orElseGet(() -> {
                    Contact newContact = new Contact();
                    newContact.setId(senderId);

                    // Fetch profile details from Meta
                    UserFacebookProfile profile = fetchProfileFromMeta(senderId);
                    newContact.setName(parseNameFromProfile(profile));

                    newContact.setMessageList(new ArrayList<>());
                    return contactRepo.save(newContact);
                });
    }

    private UserFacebookProfile fetchProfileFromMeta(String senderId) {
        // If it's a test ID from Postman, don't even try to call Meta
        if (senderId.startsWith("user_")) {
            return null;
        }

        String url = String.format("%s%s?fields=first_name,last_name&access_token=%s",
                fbBaseUrl, senderId, pageAccessToken);

        try {
            return restTemplate.getForObject(url, UserFacebookProfile.class);
        } catch (Exception e) {
            // Log the error but don't crash the app
            System.err.println("Could not fetch Meta profile for ID: " + senderId + ". Using default.");
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
        newMessage.setMetaMid(mid); // Crucial for deduplication
        newMessage.setMessageType(type);
        newMessage.setContact(contact); // This links the name to the message!

        // These fix your UI issues (The Blue Bubbles)
        newMessage.setDirection(Message.Direction.INBOUND);
        newMessage.setSenderType(Message.SenderType.USER);

        messageRepo.save(newMessage);
        System.out.println("Saved " + type + " message from: " + contact.getName());
    }

    //@Autowired
    //aiAssistance.getResponse(senderId, messageContent);

    // Inside your MessageService or InboundMessageService
    public List<Message> getMessagesForContact(String contactId)
    {
        // This assumes your MessageRepo has a method to find by the contact's ID
        return messageRepo.findByContactIdOrderByCreatedAtAsc(contactId);
    }

}