package com.example.demo.controller;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.Message;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired private MessageRepo messageRepo;
    @Autowired private AppUserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Message>> getMyShopMessages(Principal principal) {
        // 1. Find the logged-in staff member using the JWT phone number
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Fetch ONLY the messages that belong to their specific Shop (Tenant)
        List<Message> inbox = messageRepo.findByContact_Tenant_IdOrderByCreatedAtDesc(currentUser.getTenant().getId());

        return ResponseEntity.ok(inbox);
    }
}