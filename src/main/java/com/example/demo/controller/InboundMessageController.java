package com.example.demo.controller;

import com.example.demo.dto.MessengerWebhookPayload;
import com.example.demo.service.InboundMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbound")
public class InboundMessageController {

    @Autowired
    private InboundMessageService inboundMessageService;

    // 1. Meta uses this GET request to verify your webhook during setup
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        // You can change this string to whatever password you want to use when setting up the Meta App
        String VERIFY_TOKEN = "my_secret_verify_token";

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            System.out.println("WEBHOOK VERIFIED BY META");
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    // 2. Meta uses this POST request to send you real user messages
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveMessage(@RequestBody MessengerWebhookPayload payload) {
        try {
            inboundMessageService.processInboundMessage(payload);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            // It's best practice to still return 200 OK so Meta doesn't disable your webhook,
            // but we'll print the error to the console for debugging
            return ResponseEntity.status(HttpStatus.OK).build();
        }
    }
}