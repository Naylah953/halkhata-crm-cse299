package com.example.demo.controller;

import com.example.demo.domain.Contact;
import com.example.demo.repository.ContactRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactRepo contactRepo;

    @PutMapping("/{psid}/read")
    public ResponseEntity<Void> markContactAsRead(@PathVariable String psid) {
        Optional<Contact> contactOpt = contactRepo.findById(psid);

        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            contact.setUnreadCount(0);
            contactRepo.save(contact);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }
}