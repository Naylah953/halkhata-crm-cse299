package com.dbinbox.aiinbox.service;

import com.dbinbox.aiinbox.ai.assistant.CustomerAssistantService;
import com.dbinbox.aiinbox.ai.assistant.IntentClassifier;
import com.dbinbox.aiinbox.ai.tools.ProductTool;
import com.dbinbox.aiinbox.dto.UserFacebookProfile;
import com.dbinbox.aiinbox.dto.MessengerWebhookPayload;
import com.dbinbox.aiinbox.model.Contact;
import com.dbinbox.aiinbox.model.Conversation;
import com.dbinbox.aiinbox.model.Message;
import com.dbinbox.aiinbox.repository.ContactRepo;
import com.dbinbox.aiinbox.repository.ConversationRepo;
import com.dbinbox.aiinbox.repository.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;


@Service
public class InboundMessageService
{

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    private MessageRepo messageRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${meta.api.pat}")
    private String pageAccessToken;

    @Value("${meta.api.base-url}")
    private String fbBaseUrl;

    // Add these missing autowires
    @Autowired
    private IntentClassifier intentClassifier;

    @Autowired
    private ConversationRepo conversationRepo;
    @Autowired
    private OutboundMessageService outboundMessageService;

    @Autowired
    private ProductTool productTool;

    @Autowired
    private CustomerAssistantService customerAssistantService;

    public void processInboundMessage(MessengerWebhookPayload payload)
    {
        if (payload.entry() == null) return;

        for (MessengerWebhookPayload.MessengerEntry entry : payload.entry()) {
            for (MessengerWebhookPayload.MessageEvent event : entry.messageEventList()) {
                // 1. IS IT A MESSAGE? (Ignore delivery receipts/read receipts)
                //if (event.message() == null || Boolean.TRUE.equals(event.message().isEcho())) continue;

                String mid = event.message().mid();

                // 2. DEDUPLICATION (The Quota Shield)
                if (messageRepo.existsByMetaMid(mid)) continue;

                String senderId = event.sender().id();
                Contact contact = getOrCreateContact(senderId);
                Conversation conversation = getOrCreateConversation(contact);

                // 3. CIRCUIT BREAKER: If human is active, just save and stop
                //if (conversation.getState() == Conversation.State.ESCALATED) {
                    saveMessage(contact, event.message().text(), "text", mid);
                    // Trigger a WebSocket/Notification for the frontend dashboard here
                    //continue;


                String userText = event.message().text();
                String intent = intentClassifier.classify(userText);

                if ("PRODUCT_QUERY".equalsIgnoreCase(intent))
                {
                    System.out.println("DEBUG: Routing to AI Tool Flow...");
                    // Call the specific method that has the ChatClient + ProductTool
                    handleAiFlow(conversation, userText);
                }
                else if ("ORDER_REQUEST".equalsIgnoreCase(intent))
                {
                    // We can use the same AI logic, Gemini will see both tools
                    // and choose 'draftOrder' because of the intent!
                    handleAiFlow(conversation, userText);
                }
                else if ("HUMAN".equalsIgnoreCase(intent))
                {
                    System.out.println("HUMAN NEEDED");
                }
                else
                {
                    System.out.println("DEBUG: No action needed, just saving.");
                }
                }
            }
        }

    private void handleAiFlow(Conversation conversation, String userText)
    {
        customerAssistantService.handleAiLogic(conversation, userText);
    }

    private Contact getOrCreateContact(String senderId)
    {
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

    private Conversation getOrCreateConversation(Contact contact) {
        return conversationRepo.findByContactAndIsActiveTrue(contact)
                .orElseGet(() -> {
                    Conversation conv = new Conversation();
                    conv.setContact(contact);
                    conv.setState(Conversation.State.AI); // Start new chats with AI
                    //conv.setActive(true);
                    return conversationRepo.save(conv);
                });
    }

    private UserFacebookProfile fetchProfileFromMeta(String senderId)
    {
        String url = String.format("%s%s?fields=first_name,last_name&access_token=%s",
                fbBaseUrl, senderId, pageAccessToken);

        // Use the autowired restTemplate instead of creating a new one
        return restTemplate.getForObject(url, UserFacebookProfile.class);
    }

    private String parseNameFromProfile(UserFacebookProfile profile)
    {
        if (profile != null && profile.first_name() != null) {
            return profile.first_name() + " " + (profile.last_name() != null ? profile.last_name() : "");
        }
        return "Facebook User";
    }

    private void saveMessage(Contact contact, String content, String type, String mid)
    {
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



}