package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductCreateRequest {
    private Long schemaId;
    private String baseName; // e.g., "Classic Summer T-Shirt"
    private BigDecimal price;
    private Integer quantity;
    // The specific details (e.g., {"size": "M", "color": "Navy Blue"})
    private Map<String, Object> attributes;
}