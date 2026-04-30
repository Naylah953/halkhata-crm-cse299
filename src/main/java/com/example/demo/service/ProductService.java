package com.example.demo.service;

import com.example.demo.domain.AppUser;
import com.example.demo.domain.Product;
import com.example.demo.domain.ProductSchema;
import com.example.demo.domain.Tenant;
import com.example.demo.dto.ProductCreateRequest;
import com.example.demo.dto.ProductDto;
import com.example.demo.dto.ProductUpdateRequest;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSchemaRepository schemaRepository;
    private final AppUserRepository userRepository;

    private Tenant getTenantFromUsername(String username) {
        return userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getTenant();
    }

    @Transactional
    public ProductDto createProduct(String username, ProductCreateRequest request) {
        Tenant tenant = getTenantFromUsername(username);

        // 1. Fetch the Blueprint
        ProductSchema schema = schemaRepository.findByIdAndTenantId(request.getSchemaId(), tenant.getId())
                .orElseThrow(() -> new RuntimeException("Product Schema not found or does not belong to your shop."));

        // 2. THE BOUNCER: Validate the incoming attributes against the blueprint
        validateAttributes(schema.getSchemaDefinition(), request.getAttributes());

        // 3. Save the Product
        Product product = Product.builder()
                .baseName(request.getBaseName())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .attributes(request.getAttributes())
                .schema(schema)
                .tenant(tenant)
                .build();

        product = productRepository.save(product);
        return mapToDto(product);
    }

    public List<ProductDto> getAllProducts(String username) {
        Tenant tenant = getTenantFromUsername(username);
        return productRepository.findAllByTenantId(tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto updateProduct(String username, Long productId, ProductUpdateRequest request) {
        Tenant tenant = getTenantFromUsername(username);

        Product product = productRepository.findByIdAndTenantId(productId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Product not found or unauthorized access."));

        if (request.getPrice() != null) {
            if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price cannot be negative.");
            }
            product.setPrice(request.getPrice());
        }

        if (request.getQuantity() != null) {
            if (request.getQuantity() < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative.");
            }
            product.setQuantity(request.getQuantity());
        }

        product = productRepository.save(product);
        return mapToDto(product);
    }

    // ==========================================
    // CORE VALIDATION LOGIC
    // ==========================================
    @SuppressWarnings("unchecked")
    private void validateAttributes(Map<String, Object> schemaDef, Map<String, Object> attributes) {
        // Step A: Ensure no rogue fields were sent
        for (String key : attributes.keySet()) {
            if (!schemaDef.containsKey(key)) {
                throw new IllegalArgumentException("Unknown attribute provided: " + key);
            }
        }

        // Step B: Check every rule in the schema against the provided attributes
        for (Map.Entry<String, Object> entry : schemaDef.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> rules = (Map<String, Object>) entry.getValue();

            boolean isRequired = (Boolean) rules.getOrDefault("required", false);
            String type = (String) rules.get("type");

            if (!attributes.containsKey(key)) {
                if (isRequired) throw new IllegalArgumentException("Missing required attribute: " + key);
                continue; // Skip further checks if it's optional and missing
            }

            Object providedValue = attributes.get(key);

            // Step C: Strict Type Checking
            switch (type) {
                case "string":
                    if (!(providedValue instanceof String)) throw new IllegalArgumentException(key + " must be a string.");
                    break;
                case "number":
                    if (!(providedValue instanceof Number)) throw new IllegalArgumentException(key + " must be a number.");
                    break;
                case "boolean":
                    if (!(providedValue instanceof Boolean)) throw new IllegalArgumentException(key + " must be a boolean.");
                    break;
                case "enum":
                    if (!(providedValue instanceof String)) throw new IllegalArgumentException(key + " must be a string.");
                    List<String> options = (List<String>) rules.get("options");
                    if (options != null && !options.contains(providedValue)) {
                        throw new IllegalArgumentException(key + " must be one of: " + options);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown schema type defined: " + type);
            }
        }
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setSchemaId(product.getSchema().getId());
        dto.setSchemaName(product.getSchema().getName());
        dto.setBaseName(product.getBaseName());
        dto.setPrice(product.getPrice());
        dto.setQuantity(product.getQuantity());
        dto.setAttributes(product.getAttributes());
        return dto;
    }
}