package com.example.demo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ProductSchemaDto {
    private Long id;
    private String name;
    private Map<String, Object> schemaDefinition;
}