package com.example.demo.controller;

import com.example.demo.dto.ProductCreateRequest;
import com.example.demo.dto.ProductDto;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            Principal principal,
            @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(productService.createProduct(principal.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(Principal principal) {
        return ResponseEntity.ok(productService.getAllProducts(principal.getName()));
    }
}