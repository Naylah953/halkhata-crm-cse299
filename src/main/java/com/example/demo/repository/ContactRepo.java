package com.example.demo.repository;

import com.example.demo.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepo extends JpaRepository<Contact, String> {

    // For the AI tools to securely find a single contact
    Optional<Contact> findByIdAndTenantId(String id, Long tenantId);

    // For the UI sidebar to securely fetch only this shop's contacts
    List<Contact> findByTenantId(Long tenantId);

    Contact findByCustomer_IdAndTenantId(Long id, Long id1);
}