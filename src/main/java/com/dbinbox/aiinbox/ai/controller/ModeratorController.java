package com.dbinbox.aiinbox.ai.controller;

import com.dbinbox.aiinbox.ai.assistant.ModeratorAssistantService;
import com.dbinbox.aiinbox.ai.dto.ModeratorChatRequest;
import com.dbinbox.aiinbox.model.Contact;
import com.dbinbox.aiinbox.model.Message;
import com.dbinbox.aiinbox.repository.ContactRepo;
import com.dbinbox.aiinbox.service.InboundMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


//this controller specifically receives the moderator requests
@RestController
@RequestMapping("api/v1/admin")
public class ModeratorController
{
    private final ModeratorAssistantService moderatorAssistantService;
    private ContactRepo contactRepository;
    private InboundMessageService messageService;

    @Autowired
    public ModeratorController(ModeratorAssistantService moderatorAssistantService, ContactRepo contactRepository, InboundMessageService messageService)
    {
        this.moderatorAssistantService = moderatorAssistantService;
        this.contactRepository = contactRepository;
        this.messageService = messageService;
    }

    @PostMapping("/ai-assistant")
    public ResponseEntity<String> chatWithAssistant(@RequestBody ModeratorChatRequest request)
    {
        // 'request' contains the prompt and the ID of the contact currently selected in the UI
        String response = moderatorAssistantService.useAssistant(request.getMessage(), request.getContactId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contacts")
    public List<Contact> getContacts() {
        return contactRepository.findAll().stream()
                .map(contact -> new Contact(
                        contact.getId(),
                        contact.getName()
                ))
                .toList();
    }

    @GetMapping("/messages/{contactId}")
    public ResponseEntity<List<Message>> getChatHistory(@PathVariable String contactId) {
        return ResponseEntity.ok(messageService.getMessagesForContact(contactId));
    }



}

