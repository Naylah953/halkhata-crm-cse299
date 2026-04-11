package com.example.demo.controller;

import com.example.demo.dto.TenantResponse;
import com.example.demo.dto.TenantUpdateRequest;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final UserService userService;

    // ==========================================
    // GET: Read Business Info (Admins & Moderators)
    // ==========================================
    @GetMapping("/me")
    public ResponseEntity<TenantResponse> getMyTenant(Principal principal) {
        // principal.getName() automatically holds the phone number from the JWT!
        TenantResponse response = userService.getCurrentTenantDetails(principal.getName());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // PUT: Update Business Info (Admins Only)
    // ==========================================
    @PutMapping("/me")
    public ResponseEntity<TenantResponse> updateMyTenant(Principal principal, @RequestBody TenantUpdateRequest request) {
        TenantResponse updatedTenant = userService.updateTenantDetails(principal.getName(), request);
        return ResponseEntity.ok(updatedTenant);
    }
}