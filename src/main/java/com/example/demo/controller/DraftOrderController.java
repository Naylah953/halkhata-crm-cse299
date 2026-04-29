package com.example.demo.controller;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.DraftOrder;
import com.example.demo.domain.Tenant;
import com.example.demo.dto.DraftOverrideRequest;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.service.DraftResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftOrderController {

    private final DraftResolutionService draftResolutionService;
    private final AppUserRepository userRepository;

    private Long getTenantId(Principal principal) {
        return userRepository.findByPhoneNumber(principal.getName())
                .map(AppUser::getTenant)
                .map(Tenant::getId)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
    }

    @GetMapping("/contact/{contactId}")
    public ResponseEntity<List<DraftOrder>> getDrafts(Principal principal, @PathVariable String contactId) {
        return ResponseEntity.ok(draftResolutionService.getPendingDraftsForContact(contactId, getTenantId(principal)));
    }

    // ADD THIS TO YOUR DRAFT ORDER CONTROLLER
    @DeleteMapping("/{draftId}")
    public ResponseEntity<String> discardDraft(Principal principal, @PathVariable Long draftId) {
        try {
            // Assuming draftResolutionService has the discardDraft method we built earlier
            draftResolutionService.discardDraft(draftId, principal.getName());
            return ResponseEntity.ok("Draft discarded successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to discard draft: " + e.getMessage());
        }
    }

    @PutMapping("/{draftId}/cancel")
    public ResponseEntity<String> cancelDraft(Principal principal, @PathVariable Long draftId) {
        draftResolutionService.cancelDraft(draftId, getTenantId(principal), principal.getName());
        return ResponseEntity.ok("Draft Cancelled");
    }

    @PostMapping("/{draftId}/confirm")
    public ResponseEntity<String> confirmDraft(
            Principal principal,
            @PathVariable Long draftId,
            @RequestBody DraftOverrideRequest overrides) {
        draftResolutionService.confirmDraft(draftId, getTenantId(principal), principal.getName(), overrides);
        return ResponseEntity.ok("Draft Confirmed and Order Created");
    }
}