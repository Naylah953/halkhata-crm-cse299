package com.example.demo.service;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.Role;
import com.example.demo.domain.Tenant;
import com.example.demo.dto.AuthenticationRequest;
import com.example.demo.dto.AuthenticationResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.TenantRepository;
import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AppUserRepository repository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // 1. Create and save the new shop (Tenant)
        Tenant tenant = new Tenant();
        tenant.setName(request.getShopName());
        tenant.setBusinessCategory(request.getBusinessCategory()); // Add this line
        var savedTenant = tenantRepository.save(tenant);

        // 2. Create the Admin user for this shop
        AppUser user = new AppUser();
        user.setFullName(request.getAdminFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPinHash(passwordEncoder.encode(request.getPin())); // CRITICAL: Hash the PIN!
        user.setRole(Role.ADMIN);
        user.setTenant(savedTenant);
        user.setActive(true);

        repository.save(user);

        // 3. Generate the JWT token
        String jwtToken = jwtService.generateToken(user, savedTenant.getId(), user.getRole().name());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getPhoneNumber(),
                        request.getPin()
                )
        );

        AppUser user = repository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(user, user.getTenant().getId(), user.getRole().name());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}