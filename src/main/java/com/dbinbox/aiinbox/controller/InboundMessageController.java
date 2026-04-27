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
    private final String VERIFY_TOKEN = "social_crm_2026";

    public InboundMessageController(InboundMessageService service)
    {
        this.service = service;
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


    //endpoint to receive the payload

    @PostMapping(value = "/webhook")
    public ResponseEntity<Void> handleIncoming(@RequestBody MessengerWebhookPayload payload) {
        // 1. Hand it off to the service immediately.
        // Because the service method is @Async, this call returns instantly.
        service.processInboundMessage(payload);

        // 2. Return 200 OK to Meta IMMEDIATELY.
        // This stops Meta from sending those annoying duplicates.
        return ResponseEntity.ok().build();
    }}