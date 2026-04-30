package com.example.demo.ai.controller;

import com.example.demo.ai.assistant.ModeratorAssistantService;
import com.example.demo.ai.dto.ModeratorChatRequest;
import com.example.demo.domain.AppUser;
import com.example.demo.domain.Contact;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ContactRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("api/v1/admin")
public class ModeratorController {

    private final ModeratorAssistantService moderatorAssistantService;
    private final ContactRepo contactRepository;
    private final AppUserRepository userRepository;

    @Autowired
    public ModeratorController(ModeratorAssistantService moderatorAssistantService, ContactRepo contactRepository, AppUserRepository userRepository) {
        this.moderatorAssistantService = moderatorAssistantService;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/ai-assistant")
    public ResponseEntity<String> chatWithAssistant(
            @RequestBody ModeratorChatRequest request,
            Principal principal) {

        // SECURE READ: Find the logged-in user via their JWT Principal
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

        Long secureTenantId = currentUser.getTenant().getId();

        // Pass the Admin's Name into the AI for a better user experience
        String adminName = currentUser.getFullName();

        String response = moderatorAssistantService.useAssistant(
                request.getMessage(),
                request.getContactId(),
                secureTenantId,
                adminName); // <--- Added parameter

        return ResponseEntity.ok(response);
    }

    @GetMapping("/contacts")
    public List<Contact> getContacts(Principal principal) {
        // SECURE READ: Find the logged-in user and enforce tenant boundaries
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

        return contactRepository.findByTenantId(currentUser.getTenant().getId());
    }
}