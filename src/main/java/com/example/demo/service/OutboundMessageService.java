package com.example.demo.service;

import com.example.demo.domain.*;
import com.example.demo.dto.CRMResponse;
import com.example.demo.repository.*;
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
    @Autowired private OrderRepository orderRepository;
    @Autowired private PdfService pdfService;
    @Autowired private CustomerRepository customerRepository;

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

    //FARIZA CHANGE
    @Transactional
    public void sendPdfAsAttachment(String psid, Long orderId) {
        // 1. Fetch data and check security
        System.out.println("WOOOOOWOOOOOWOOOOO sendPdfAsAttachment");
        Contact contact = contactRepo.findById(psid)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        // We need the Order to generate the PDF
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 2. Generate PDF bytes
        byte[] pdfBytes = pdfService.generateOrderReceipt(order);

        // 3. Prepare the Multipart Body
        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();

        // Meta requires these as JSON strings in a multipart request
        body.add("recipient", "{\"id\":\"" + psid + "\"}");
        body.add("message", "{\"attachment\":{\"type\":\"file\", \"payload\":{\"is_reusable\":true}}}");

        // Wrap the file bytes with correct headers
        org.springframework.http.HttpHeaders fileHeaders = new org.springframework.http.HttpHeaders();
        fileHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        fileHeaders.setContentDispositionFormData("filedata", "receipt_" + orderId + ".pdf");

        org.springframework.http.HttpEntity<byte[]> fileEntity = new org.springframework.http.HttpEntity<>(pdfBytes, fileHeaders);
        body.add("filedata", fileEntity);

        // 4. Send the Request
        String url = fbBaseUrl + "me/messages?access_token=" + contact.getTenant().getPageAccessToken();

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        try {
            var response = restTemplate.postForEntity(url, new org.springframework.http.HttpEntity<>(body, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                // Log to your UI that the receipt was successfully sent
                //saveToDatabase(customer, "📄 Attached Order Receipt #" + orderId, Message.SenderType.AGENT);
            }
        } catch (Exception e) {
            System.err.println("Failed to send multipart PDF: " + e.getMessage());
        }
    }
}