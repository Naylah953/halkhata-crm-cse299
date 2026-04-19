package com.example.demo.repository;

import com.example.demo.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Fetch all customers for a specific shop
    List<Customer> findAllByTenantId(Long tenantId);

    // Fetch a specific customer, ensuring they belong to the shop
    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

    // Find by phone number within a specific shop (useful for validating before creation)
    Optional<Customer> findByPhoneNumberAndTenantId(String phoneNumber, Long tenantId);

    // Check if a phone number already exists for a specific shop
    boolean existsByPhoneNumberAndTenantId(String phoneNumber, Long tenantId);
}