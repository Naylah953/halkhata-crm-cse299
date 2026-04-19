package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductDto {
    private Long id;
    private Long schemaId;
    private String schemaName;
    private String baseName;
    private BigDecimal price;
    private Integer quantity;
    private Map<String, Object> attributes;
}