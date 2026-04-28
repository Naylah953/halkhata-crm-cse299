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
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    // ==========================================
    // 1. STAFF CREATION LOGIC
    // ==========================================
    public void createStaffMember(StaffCreationRequest request) {
        AppUser currentAdmin = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number is already registered!");
        }

        AppUser newStaff = new AppUser();
        newStaff.setFullName(request.getFullName());
        newStaff.setPhoneNumber(request.getPhoneNumber());
        newStaff.setPinHash(passwordEncoder.encode(request.getPin()));
        newStaff.setRole(Role.MODERATOR);
        newStaff.setTenant(currentAdmin.getTenant());

        userRepository.save(newStaff);
    }

    // ==========================================
    // 2. GET TEAM MEMBERS
    // ==========================================
    public List<StaffResponse> getTeamMembers(String currentUsername) {
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AppUser> team = userRepository.findByTenantId(currentUser.getTenant().getId());

        return team.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(user -> new StaffResponse(user.getId(), user.getFullName(), user.getRole().name()))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 3. GET BUSINESS INFO
    // ==========================================
    public TenantResponse getCurrentTenantDetails(String currentUsername) {
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant t = currentUser.getTenant();

        // UPDATED: Now returns the Facebook fields and AI toggle to the frontend UI
        return new TenantResponse(
                t.getName(),
                t.getBusinessCategory(),
                t.getBusinessAddress(),
                t.getContactPhone(),
                t.getContactEmail(),
                t.getFacebookPageId(),
                t.getPageAccessToken(),
                t.isEnableAiReplies() // --- NEW ---
        );
    }

    // ==========================================
    // 4. UPDATE BUSINESS INFO (Fixed for Partial Updates)
    // ==========================================
    @Transactional
    public TenantResponse updateTenantDetails(String currentUsername, TenantUpdateRequest request) {
        AppUser currentUser = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = currentUser.getTenant();

        // Only update the fields that the frontend actually sent (not null)
        if (request.getShopName() != null) {
            tenant.setName(request.getShopName());
        }
        if (request.getBusinessCategory() != null) {
            tenant.setBusinessCategory(request.getBusinessCategory());
        }
        if (request.getBusinessAddress() != null) {
            tenant.setBusinessAddress(request.getBusinessAddress());
        }
        if (request.getContactPhone() != null) {
            tenant.setContactPhone(request.getContactPhone());
        }
        if (request.getContactEmail() != null) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (request.getFacebookPageId() != null) {
            tenant.setFacebookPageId(request.getFacebookPageId());
        }
        if (request.getPageAccessToken() != null) {
            tenant.setPageAccessToken(request.getPageAccessToken());
        }

        // The AI Toggle
        if (request.getEnableAiReplies() != null) {
            tenant.setEnableAiReplies(request.getEnableAiReplies());
        }

        tenantRepository.save(tenant);

        // Return the complete updated details
        return new TenantResponse(
                tenant.getName(),
                tenant.getBusinessCategory(),
                tenant.getBusinessAddress(),
                tenant.getContactPhone(),
                tenant.getContactEmail(),
                tenant.getFacebookPageId(),
                tenant.getPageAccessToken(),
                tenant.isEnableAiReplies()
        );
    }

    // ==========================================
    // 5. DELETE STAFF MEMBER
    // ==========================================
    public void deleteStaffMember(String currentUsername, Long staffId) {
        AppUser currentAdmin = userRepository.findByPhoneNumber(currentUsername)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        AppUser staffToDelete = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (!staffToDelete.getTenant().getId().equals(currentAdmin.getTenant().getId())) {
            throw new RuntimeException("Unauthorized: Cannot delete staff from another shop!");
        }

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