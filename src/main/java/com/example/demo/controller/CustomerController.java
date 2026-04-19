package com.example.demo.controller;

import com.example.demo.dto.CustomerCreateRequest;
import com.example.demo.dto.CustomerDto;
import com.example.demo.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(
            Principal principal,
            @RequestBody CustomerCreateRequest request) {
        // principal.getName() securely passes the phone number from the JWT
        return ResponseEntity.ok(customerService.createCustomer(principal.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getCustomers(Principal principal) {
        return ResponseEntity.ok(customerService.getCustomers(principal.getName()));
    }
}