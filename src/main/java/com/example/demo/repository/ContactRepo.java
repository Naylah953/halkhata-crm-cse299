package com.example.demo.repository;

import com.example.demo.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactRepo extends JpaRepository<Contact, String> {

    // NEW MULTI-TENANT RULE: Find a contact by their PSID, but ONLY if they belong to the specific Tenant
    Optional<Contact> findByIdAndTenantId(String id, Long tenantId);
}