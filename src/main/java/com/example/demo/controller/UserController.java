package com.example.demo.controller;

import com.example.demo.dto.StaffCreationRequest;
import com.example.demo.dto.StaffResponse;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ==========================================
    // POST: Add a new Staff Member (Admins Only)
    // ==========================================
    @PostMapping("/staff")
    public ResponseEntity<String> createStaff(@RequestBody StaffCreationRequest request) {
        // Hand the JSON data off to the Brains
        userService.createStaffMember(request);

        // Return a 200 OK success message to the frontend
        return ResponseEntity.ok("Staff member created successfully!");
    }

    // ==========================================
    // GET: View Team Members (Admins & Moderators)
    // ==========================================
    @GetMapping("/staff")
    public ResponseEntity<List<StaffResponse>> getStaff(Principal principal) {
        // principal.getName() automatically holds the phone number from the JWT!
        List<StaffResponse> team = userService.getTeamMembers(principal.getName());

        return ResponseEntity.ok(team);
    }

    // ==========================================
    // GET: View Current User Profile
    // ==========================================
    @GetMapping("/me")
    public ResponseEntity<StaffResponse> getCurrentUser(Principal principal) {
        StaffResponse userProfile = userService.getCurrentUserDetails(principal.getName());
        return ResponseEntity.ok(userProfile);
    }

    // ==========================================
    // DELETE: Remove a Staff Member (Admins Only)
    // ==========================================
    @DeleteMapping("/staff/{id}")
    public ResponseEntity<String> deleteStaff(Principal principal, @PathVariable Long id) {
        userService.deleteStaffMember(principal.getName(), id);
        return ResponseEntity.ok("Staff member removed successfully.");
    }
}