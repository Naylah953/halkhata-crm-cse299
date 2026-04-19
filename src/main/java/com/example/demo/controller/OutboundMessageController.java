package com.example.demo.controller;

import com.example.demo.dto.CRMResponse;
import com.example.demo.service.OutboundMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outbound")
public class OutboundMessageController {

    private final OutboundMessageService messageService;

    @Autowired
    public OutboundMessageController(OutboundMessageService messageService) {
        this.messageService = messageService;
    }

    // Expects a JSON body (Great for your future AI service to call)
    @PostMapping
    public ResponseEntity<Void> sendMessage(@RequestBody CRMResponse response) {
        messageService.sendReplyToUser(response.recipient().id(), response.message().text());
        return ResponseEntity.ok().build();
    }

    // Expects URL parameters (Great for quick Postman testing!)
    @PostMapping("/send")
    public ResponseEntity<Void> handleFormSend(@RequestParam String psid, @RequestParam String text) {
        messageService.sendReplyToUser(psid, text);
        return ResponseEntity.ok().build();
    }
}