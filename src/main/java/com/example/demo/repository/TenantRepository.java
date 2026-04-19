package com.example.demo.repository;

import com.example.demo.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    // NEW ROUTING METHOD: Find the specific shop using their Facebook Page ID
    Optional<Tenant> findByFacebookPageId(String facebookPageId);
}