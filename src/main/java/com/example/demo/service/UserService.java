package com.example.demo.service;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.Role;
import com.example.demo.domain.Tenant;
import com.example.demo.dto.StaffCreationRequest;
import com.example.demo.dto.StaffResponse;
import com.example.demo.dto.TenantResponse;
import com.example.demo.dto.TenantUpdateRequest;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository; // Added to save Tenant updates
    private final PasswordEncoder passwordEncoder;

    // ==========================================
    // 1. STAFF CREATION LOGIC
    // ==========================================
    public void createStaffMember(StaffCreationRequest request) {
        // 1. Grab the currently logged-in Admin directly from the VIP Lounge (Security Context)
        AppUser currentAdmin = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Safety Check: Make sure this phone number isn't already used by another shop
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number is already registered!");
        }

        // 3. Create Bob the Cashier
        AppUser newStaff = new AppUser();
        newStaff.setFullName(request.getFullName());
        newStaff.setPhoneNumber(request.getPhoneNumber());

        // 4. Cryptography: Scramble Bob's PIN before saving
        newStaff.setPinHash(passwordEncoder.encode(request.getPin()));

        // 5. The Golden Rule: Assign the MODERATOR role, and lock them into the Admin's exact shop!
        newStaff.setRole(Role.MODERATOR);
        newStaff.setTenant(currentAdmin.getTenant());

        // 6. Save to the database
        userRepository.save(newStaff);
    }

    // ==========================================
    // 2. GET TEAM MEMBERS (For the Settings UI)
    // ==========================================
    public List<StaffResponse> getTeamMembers(String currentUsername) {
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find everyone who works at their exact same shop
        List<AppUser> team = userRepository.findByTenantId(currentUser.getTenant().getId());

        // Convert the database entities into safe DTOs for the frontend
        return team.stream()
                // THE NEW LINE: Filter out the user who is currently logged in!
                .filter(user -> !user.getId().equals(currentUser.getId()))
                // -----------------------------------------------------------
                .map(user -> new StaffResponse(user.getId(), user.getFullName(), user.getRole().name()))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3. GET BUSINESS INFO (For the Settings UI)
    // ==========================================
    public TenantResponse getCurrentTenantDetails(String currentUsername) {
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant t = currentUser.getTenant();

        // Map the database entity to the safe frontend DTO
        return new TenantResponse(
                t.getName(),
                t.getBusinessCategory(),
                t.getBusinessAddress(),
                t.getContactPhone(),
                t.getContactEmail()
        );
    }

    // ==========================================
    // 4. UPDATE BUSINESS INFO (Admin Only)
    // ==========================================
    @Transactional
    public TenantResponse updateTenantDetails(String currentUsername, TenantUpdateRequest request) {
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = currentUser.getTenant();

        // Update the fields using the incoming JSON payload
        tenant.setName(request.getShopName());
        // No need for .valueOf() anymore!
        tenant.setBusinessCategory(request.getBusinessCategory());
        tenant.setBusinessAddress(request.getBusinessAddress());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setContactEmail(request.getContactEmail());

        // Save the updated Tenant to PostgreSQL
        tenantRepository.save(tenant);

        // Return the fresh data back to the frontend to confirm success
        return new TenantResponse(
                tenant.getName(),
                tenant.getBusinessCategory(),
                tenant.getBusinessAddress(),
                tenant.getContactPhone(),
                tenant.getContactEmail()
        );
    }

    // ==========================================
    // 5. DELETE STAFF MEMBER (Admins Only)
    // ==========================================
    public void deleteStaffMember(String currentUsername, Long staffId) {
        AppUser currentAdmin = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        AppUser staffToDelete = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        // SECURITY CHECK 1: Ensure the staff belongs to the admin's exact shop!
        if (!staffToDelete.getTenant().getId().equals(currentAdmin.getTenant().getId())) {
            throw new RuntimeException("Unauthorized: Cannot delete staff from another shop!");
        }

        // SECURITY CHECK 2: Prevent the admin from accidentally deleting themselves!
        if (staffToDelete.getId().equals(currentAdmin.getId())) {
            throw new RuntimeException("You cannot delete your own admin account!");
        }

        userRepository.delete(staffToDelete);
    }

    // ==========================================
    // 6. GET CURRENT USER PROFILE
    // ==========================================
    public StaffResponse getCurrentUserDetails(String currentUsername) {
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new StaffResponse(currentUser.getId(), currentUser.getFullName(), currentUser.getRole().name());
    }
}