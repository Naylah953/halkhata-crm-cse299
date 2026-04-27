package com.example.demo.service;

import com.example.demo.dto.CRMResponse;
import com.example.demo.domain.AppUser;
import com.example.demo.domain.Contact;
import com.example.demo.domain.Message;
import com.example.demo.domain.Tenant;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ContactRepo;
import com.example.demo.repository.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class OutboundMessageService {

    @Autowired private RestTemplate restTemplate;
    @Autowired private MessageRepo messageRepo;
    @Autowired private ContactRepo contactRepo;

    // 1. ADD THE USER REPOSITORY
    @Autowired private AppUserRepository userRepository;

    // Inject the SseService
    @Autowired private SseService sseService;

    @Value("${meta.api.base-url}")
    private String fbBaseUrl;

    // 2. ADD TRANSACTIONAL TO KEEP THE DB SESSION OPEN
    @Transactional
    public void sendReplyToUser(String psid, String text) {

        // 3. FIX LAZY INIT: Get the username from context, then do a fresh database lookup
        String currentUserPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = userRepository.findByPhoneNumber(currentUserPhone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant loggedInTenant = currentUser.getTenant();

        // 4. Look up the contact they are trying to message
        Optional<Contact> contactOpt = contactRepo.findById(psid);

        if (contactOpt.isEmpty()) {
            System.err.println("Cannot send message: Contact not found.");
            return;
        }

        Contact contact = contactOpt.get();
        Tenant contactTenant = contact.getTenant();

        // 5. MULTI-TENANT SECURITY SHIELD
        if (!contactTenant.getId().equals(loggedInTenant.getId())) {
            throw new RuntimeException("Security Violation: You are not authorized to message a contact from another shop!");
        }

        // 6. Get the Page Access Token for this shop
        String pat = loggedInTenant.getPageAccessToken();

        if (pat == null || pat.isEmpty()) {
            System.err.println("Cannot send message: Shop " + loggedInTenant.getName() + " has no Page Access Token configured.");
            return;
        }

        // 7. Format the response and inject the dynamic PAT into the URL
        CRMResponse response = new CRMResponse(new CRMResponse.Recipient(psid), new CRMResponse.Message(text));
        String url = fbBaseUrl + "me/messages?access_token=" + pat;

        try {
            var metaResponse = restTemplate.postForEntity(url, response, String.class);

            if (metaResponse.getStatusCode().is2xxSuccessful()) {
                saveToDatabase(contact, text);
                System.out.println("Outbound message sent via Meta for shop: " + loggedInTenant.getName());
            }
        } catch (Exception e) {
            System.err.println("Error sending message to Meta: " + e.getMessage());
        }
    }

    private void saveToDatabase(Contact contact, String text) {
        Message outboundMsg = new Message();
        outboundMsg.setContent(text);
        outboundMsg.setDirection(Message.Direction.OUTBOUND);
        outboundMsg.setSenderType(Message.SenderType.BOT);
        outboundMsg.setMessageType("text");
        outboundMsg.setContact(contact);

        // Capture the saved entity so it has the generated ID and timestamp
        outboundMsg = messageRepo.save(outboundMsg);

        // Push the outgoing message to all active agents for this tenant
        sseService.pushMessageToTenant(contact.getTenant().getId(), outboundMsg);
    }
}