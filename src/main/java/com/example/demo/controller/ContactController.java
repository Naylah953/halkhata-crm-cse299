package com.example.demo.controller;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.Complaint;
import com.example.demo.domain.Contact;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ComplaintRepo;
import com.example.demo.repository.ContactRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactRepo contactRepo;
    private final ComplaintRepo complaintRepo;
    private final AppUserRepository userRepository;

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

    // --- Fetch Complaints for a specific customer ---
    @GetMapping("/{psid}/complaints")
    public ResponseEntity<List<Complaint>> getCustomerComplaints(@PathVariable String psid, Principal principal) {
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));

        Long secureTenantId = currentUser.getTenant().getId();

        List<Complaint> complaints = complaintRepo.findByPsidAndTenantIdOrderByCreatedAtDesc(psid, secureTenantId);

        return ResponseEntity.ok(complaints);
    }

    // --- Resolve a specific complaint by exact ID ---
    @PutMapping("/complaints/{complaintId}/resolve")
    public ResponseEntity<Void> resolveComplaint(@PathVariable Long complaintId, Principal principal) {
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));
        Long secureTenantId = currentUser.getTenant().getId();

        Optional<Complaint> complaintOpt = complaintRepo.findById(complaintId);

        // Ensure the complaint exists and belongs to the currently logged-in shop
        if (complaintOpt.isPresent() && complaintOpt.get().getTenantId().equals(secureTenantId)) {
            Complaint complaint = complaintOpt.get();
            complaint.setStatus("RESOLVED");
            complaintRepo.save(complaint);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }

    // --- Bidirectional Tag Removal (Triggered via UI) ---
    @DeleteMapping("/{psid}/tags/{tag}")
    public ResponseEntity<Void> removeTag(@PathVariable String psid, @PathVariable String tag, Principal principal) {
        AppUser currentUser = userRepository.findByPhoneNumber(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));
        Long secureTenantId = currentUser.getTenant().getId();

        Optional<Contact> contactOpt = contactRepo.findById(psid);

        // Security check: Must exist and belong to the logged-in shop
        if (contactOpt.isPresent() && contactOpt.get().getTenant().getId().equals(secureTenantId)) {
            Contact contact = contactOpt.get();

            if (contact.getTags() != null) {
                // Split the comma-separated string, remove the target, and rejoin
                String[] currentTags = contact.getTags().split(",");
                List<String> updatedTags = new ArrayList<>();

                for (String t : currentTags) {
                    if (!t.trim().equalsIgnoreCase(tag.trim())) {
                        updatedTags.add(t.trim());
                    }
                }

                if (updatedTags.isEmpty()) {
                    contact.setTags(null); // No tags left
                } else {
                    contact.setTags(String.join(", ", updatedTags));
                }

                contactRepo.save(contact);
            }
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }
}