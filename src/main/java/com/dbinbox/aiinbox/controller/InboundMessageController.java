package com.dbinbox.aiinbox.controller;

import com.dbinbox.aiinbox.dto.MessengerWebhookPayload;
//import com.dbinbox.aiinbox.ai.assistant.AiAssistantService;
import com.dbinbox.aiinbox.service.InboundMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbound")
public class InboundMessageController {

    private final InboundMessageService service;
    //private final AiAssistantService aiAssistantService;
    private final String VERIFY_TOKEN = "social_crm_2026";

    public InboundMessageController(InboundMessageService service)
    {
        this.service = service;
        //this.aiAssistantService = aiAssistantService;
    }

    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PostMapping(value = "/webhook")
    public ResponseEntity<Void> handleIncoming(@RequestBody MessengerWebhookPayload payload) {
        // This now returns the AI's "Banglish" response!
        String responseForUser = service.processInboundMessage(payload);

        if (responseForUser != null) {
            // TODO: Call your MetaMessageSender here
            System.out.println("Ready to send to Meta: " + responseForUser);
        }

        return ResponseEntity.ok().build();
    }
    // NOTE: I removed the extra @PostMapping without a path to avoid ambiguity.
}