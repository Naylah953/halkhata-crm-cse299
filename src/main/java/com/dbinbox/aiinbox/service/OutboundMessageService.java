package com.dbinbox.aiinbox.service;

import com.dbinbox.aiinbox.dto.CRMResponse;
import com.dbinbox.aiinbox.model.Contact;
import com.dbinbox.aiinbox.model.Message;
import com.dbinbox.aiinbox.repository.ContactRepo;
import com.dbinbox.aiinbox.repository.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class OutboundMessageService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MessageRepo messageRepo; // To save the reply

    @Autowired
    private ContactRepo contactRepo; // To link the reply to the user

    @Value("${meta.api.pat}")
    private String pat;

    @Value("${meta.api.base-url}")
    private String fbBaseUrl;

    public void sendReplyToUser(String psid, String text) {
        CRMResponse response = new CRMResponse(new CRMResponse.Recipient(psid), new CRMResponse.Message(text));

        // Use v21.0 or your preferred version
        String url = fbBaseUrl + "me/messages?access_token=" + pat;

        try {
            // 1. Fire the message to Meta
            // We use postForEntity to check the status code
            var metaResponse = restTemplate.postForEntity(url, response, String.class);

            if (metaResponse.getStatusCode().is2xxSuccessful()) {
                // 2. If successful, Mirror to Database
                saveToDatabase(psid, text);
                System.out.println("Outbound message sent and mirrored to DB.");
            }
        } catch (Exception e) {
            System.err.println("Error sending message to Meta: " + e.getMessage());
        }
    }

    private void saveToDatabase(String psid, String text) {
        Message outboundMsg = new Message();
        outboundMsg.setContent(text);
        outboundMsg.setDirection(Message.Direction.OUTBOUND); // This triggers the purple CSS
        outboundMsg.setSenderType(Message.SenderType.BOT);
        outboundMsg.setMessageType("text");

        // Find the contact so the message appears in the right chat thread
        contactRepo.findById(psid).ifPresent(outboundMsg::setContact);

        messageRepo.save(outboundMsg);
    }
}