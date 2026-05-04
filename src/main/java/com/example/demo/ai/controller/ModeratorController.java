package com.example.demo.ai.controller;

import com.example.demo.ai.assistant.ModeratorAssistantService;
import com.example.demo.ai.dto.ModeratorChatRequest;
import com.example.demo.ai.dto.ModeratorChatResponse;
import com.example.demo.ai.tools.ModeratorTools;
import com.example.demo.domain.AppUser;
import com.example.demo.domain.AiChatMessage;
import com.example.demo.domain.Contact;
import com.example.demo.dto.AiAnalyticsResponse;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.AiChatMessageRepo;
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
    private final AiChatMessageRepo aiChatMessageRepo;

    @Autowired
    public ModeratorController(ModeratorAssistantService moderatorAssistantService,
                               ContactRepo contactRepository,
                               AppUserRepository userRepository,
                               AiChatMessageRepo aiChatMessageRepo) {
        this.moderatorAssistantService = moderatorAssistantService;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.aiChatMessageRepo = aiChatMessageRepo;
    }

    @PostMapping("/ai-assistant")
    public ResponseEntity<ModeratorChatResponse> chatWithAssistant(
            @RequestBody ModeratorChatRequest request,
            Principal principal) {

        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

        Long secureTenantId = currentUser.getTenant().getId();
        String adminName = currentUser.getFullName();

        // 1. Save Admin Message
        AiChatMessage adminMessage = new AiChatMessage();
        adminMessage.setContent(request.getMessage());
        adminMessage.setSender(AiChatMessage.Sender.ADMIN);
        adminMessage.setTenantId(secureTenantId);
        aiChatMessageRepo.save(adminMessage);

        try {
            // 2. Call AI Service
            String responseText = moderatorAssistantService.useAssistant(
                    request.getMessage(),
                    request.getContactId(),
                    secureTenantId,
                    adminName);

            // 3. Extract Table Data from ThreadLocal BEFORE saving the message
            AiAnalyticsResponse.TableData tableData = ModeratorTools.currentTableData.get();

            // 4. Save AI Text Response AND attached Table Data to the DB
            AiChatMessage aiMessage = new AiChatMessage();
            aiMessage.setContent(responseText);
            aiMessage.setSender(AiChatMessage.Sender.AI);
            aiMessage.setTenantId(secureTenantId);

            if (tableData != null) {
                aiMessage.setTableData(tableData);
            }
            aiChatMessageRepo.save(aiMessage);

            // 5. Build the structured DTO for the frontend
            ModeratorChatResponse responseDTO = new ModeratorChatResponse(responseText, false, null);

            if (tableData != null) {
                responseDTO.setTable(true);
                responseDTO.setTableData(tableData);
            }

            return ResponseEntity.ok(responseDTO);

        } finally {
            // CRITICAL: Always clear the ThreadLocal regardless of success or failure
            // This prevents multi-tenant data leaks and memory leaks.
            ModeratorTools.clearTableData();
        }
    }

    @GetMapping("/ai-history")
    public ResponseEntity<List<AiChatMessage>> getAiHistory(Principal principal) {
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

        Long secureTenantId = currentUser.getTenant().getId();

        List<AiChatMessage> history = aiChatMessageRepo.findByTenantIdOrderByCreatedAtAsc(secureTenantId);

        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/ai-history")
    public ResponseEntity<Void> clearAiHistory(Principal principal) {
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

        Long secureTenantId = currentUser.getTenant().getId();

        aiChatMessageRepo.deleteByTenantId(secureTenantId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/contacts")
    public List<Contact> getContacts(Principal principal) {
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

        return contactRepository.findByTenantId(currentUser.getTenant().getId());
    }
}