package com.example.demo.controller;

import com.example.demo.dto.ProductSchemaCreateRequest;
import com.example.demo.dto.ProductSchemaDto;
import com.example.demo.service.ProductSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class ProductSchemaController {

    private final ProductSchemaService schemaService;

    @PostMapping
    public ResponseEntity<ProductSchemaDto> createSchema(
            Principal principal,
            @RequestBody ProductSchemaCreateRequest request) {
        return ResponseEntity.ok(schemaService.createSchema(principal.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<ProductSchemaDto>> getAllSchemas(Principal principal) {
        return ResponseEntity.ok(schemaService.getAllSchemas(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductSchemaDto> getSchemaById(
            Principal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(schemaService.getSchemaById(principal.getName(), id));
    }
}