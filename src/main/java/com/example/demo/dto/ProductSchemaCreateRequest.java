package com.example.demo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ProductSchemaCreateRequest {
    private String name; // e.g., "Clothing"
    // This holds the exact dynamic JSON blueprint
    private Map<String, Object> schemaDefinition;
}