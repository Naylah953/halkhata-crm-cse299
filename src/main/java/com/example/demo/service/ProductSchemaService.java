package com.example.demo.service;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.ProductSchema;
import com.example.demo.domain.Tenant;
import com.example.demo.dto.ProductSchemaCreateRequest;
import com.example.demo.dto.ProductSchemaDto;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ProductSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSchemaService {

    private final ProductSchemaRepository schemaRepository;
    private final AppUserRepository userRepository;

    // Helper to extract the Shop from the logged-in JWT user
    private Tenant getTenantFromUsername(String username) {
        return userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getTenant();
    }

    @Transactional
    public ProductSchemaDto createSchema(String username, ProductSchemaCreateRequest request) {
        Tenant tenant = getTenantFromUsername(username);

        // 1. Prevent duplicate schema names within the same shop
        if (schemaRepository.existsByNameAndTenantId(request.getName(), tenant.getId())) {
            throw new RuntimeException("A schema with the name '" + request.getName() + "' already exists.");
        }

        // 2. Build and save the schema
        ProductSchema schema = ProductSchema.builder()
                .name(request.getName())
                .schemaDefinition(request.getSchemaDefinition())
                .tenant(tenant)
                .build();

        schema = schemaRepository.save(schema);

        return mapToDto(schema);
    }

    public List<ProductSchemaDto> getAllSchemas(String username) {
        Tenant tenant = getTenantFromUsername(username);

        return schemaRepository.findAllByTenantId(tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ProductSchemaDto getSchemaById(String username, Long schemaId) {
        Tenant tenant = getTenantFromUsername(username);

        ProductSchema schema = schemaRepository.findByIdAndTenantId(schemaId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Schema not found."));

        return mapToDto(schema);
    }

    // Helper mapping method
    private ProductSchemaDto mapToDto(ProductSchema schema) {
        ProductSchemaDto dto = new ProductSchemaDto();
        dto.setId(schema.getId());
        dto.setName(schema.getName());
        dto.setSchemaDefinition(schema.getSchemaDefinition());
        return dto;
    }
}