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
    @Autowired private AppUserRepository userRepository;
    @Autowired private SseService sseService;

    @Value("${meta.api.base-url}")
    private String fbBaseUrl;

    @Transactional
    public void sendReplyToUser(String psid, String text) {
        String currentUserPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = userRepository.findByPhoneNumber(currentUserPhone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant loggedInTenant = currentUser.getTenant();
        Optional<Contact> contactOpt = contactRepo.findById(psid);

        if (contactOpt.isEmpty()) {
            System.err.println("Cannot send message: Contact not found.");
            return;
        }

        Contact contact = contactOpt.get();
        Tenant contactTenant = contact.getTenant();

        if (!contactTenant.getId().equals(loggedInTenant.getId())) {
            throw new RuntimeException("Security Violation: Unauthorized!");
        }

        String pat = loggedInTenant.getPageAccessToken();
        CRMResponse response = new CRMResponse(new CRMResponse.Recipient(psid), new CRMResponse.Message(text));
        String url = fbBaseUrl + "me/messages?access_token=" + pat;

        try {
            var metaResponse = restTemplate.postForEntity(url, response, String.class);
            if (metaResponse.getStatusCode().is2xxSuccessful()) {
                // CHANGE: Pass AGENT here so the DB knows a human replied
                saveToDatabase(contact, text, Message.SenderType.AGENT);

                if (contact.getRequiresHuman() != null && contact.getRequiresHuman()) {
                    contact.setRequiresHuman(false);
                    contactRepo.save(contact);
                    sseService.pushContactUpdateToTenant(loggedInTenant.getId(), contact);
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void sendAiReply(Contact contact, String text) {
        Tenant tenant = contact.getTenant();
        String pat = tenant.getPageAccessToken();

        CRMResponse response = new CRMResponse(new CRMResponse.Recipient(contact.getId()), new CRMResponse.Message(text));
        String url = fbBaseUrl + "me/messages?access_token=" + pat;

        try {
            var metaResponse = restTemplate.postForEntity(url, response, String.class);
            if (metaResponse.getStatusCode().is2xxSuccessful()) {
                // CHANGE: Pass BOT here for AI automated replies
                saveToDatabase(contact, text, Message.SenderType.BOT);
            }
        } catch (Exception e) {
            System.err.println("Error sending AI message: " + e.getMessage());
        }
    }

    // UPDATED: Now accepts SenderType as an argument
    private void saveToDatabase(Contact contact, String text, Message.SenderType senderType) {
        Message outboundMsg = new Message();
        outboundMsg.setContent(text);
        outboundMsg.setDirection(Message.Direction.OUTBOUND);
        outboundMsg.setSenderType(senderType); // Set dynamically!
        outboundMsg.setMessageType("text");
        outboundMsg.setContact(contact);

        outboundMsg = messageRepo.save(outboundMsg);
        sseService.pushMessageToTenant(contact.getTenant().getId(), outboundMsg);
    }
}