package com.example.demo.repository;

import com.example.demo.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Fetch all products for a specific shop
    List<Product> findAllByTenantId(Long tenantId);

    // Fetch a specific product ensuring it belongs to the shop
    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);

    // Optional but highly recommended: Fetch all products that belong to a specific schema/category
    List<Product> findAllBySchemaIdAndTenantId(Long schemaId, Long tenantId);

    // ==========================================
    // NEW: Secure product search for the AI Tool
    // ==========================================
    List<Product> findByTenantIdAndBaseNameContainingIgnoreCase(Long tenantId, String baseName);

    // FRIEND'S UPGRADE: Fallback search by category/schema name
    List<Product> findByTenantIdAndSchema_NameContainingIgnoreCase(Long tenantId, String schemaName);
}