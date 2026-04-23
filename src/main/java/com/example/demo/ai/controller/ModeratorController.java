package com.example.demo.ai.controller;

import com.example.demo.ai.assistant.ModeratorAssistantService;
import com.example.demo.ai.dto.ModeratorChatRequest;
import com.example.demo.domain.Contact; // Adjust if your model is in a different folder
import com.example.demo.repository.ContactRepo; // Adjust if your repo is in a different folder
// import com.example.demo.service.InboundMessageService; // Uncomment if you bring this over later

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/admin")
public class ModeratorController {

    private final ModeratorAssistantService moderatorAssistantService;
    private final ContactRepo contactRepository;
    // private final InboundMessageService messageService;

    @Autowired
    public ModeratorController(ModeratorAssistantService moderatorAssistantService, ContactRepo contactRepository) {
        this.moderatorAssistantService = moderatorAssistantService;
        this.contactRepository = contactRepository;
    }

    @PostMapping("/ai-assistant")
    public ResponseEntity<String> chatWithAssistant(
            @RequestBody ModeratorChatRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) Long tenantId) {

        // Pass both the Contact ID and the Tenant ID into the Brain
        String response = moderatorAssistantService.useAssistant(
                request.getMessage(),
                request.getContactId(),
                tenantId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/contacts")
    public List<Contact> getContacts(@RequestHeader(value = "X-Tenant-ID") Long tenantId) {
        // SECURE READ: Only fetch contacts belonging to this specific shop
        return contactRepository.findByTenantId(tenantId);
    }
}