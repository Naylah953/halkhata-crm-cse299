package com.example.demo.repository;

import com.example.demo.domain.ProductSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSchemaRepository extends JpaRepository<ProductSchema, Long> {

    List<ProductSchema> findAllByTenantId(Long tenantId);

    Optional<ProductSchema> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByNameAndTenantId(String name, Long tenantId);
}