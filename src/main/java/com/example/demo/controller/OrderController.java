package com.example.demo.controller;

import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(Principal principal, @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(orderService.createOrder(principal.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrders(Principal principal) {
        return ResponseEntity.ok(orderService.getOrders(principal.getName()));
    }

    // --- NEW ENDPOINT ---
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderDto>> getOrdersByCustomer(Principal principal, @PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(principal.getName(), customerId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<OrderDto>> getOrdersByProduct(Principal principal, @PathVariable Long productId) {
        return ResponseEntity.ok(orderService.getOrdersByProduct(principal.getName(), productId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(principal.getName(), id));
    }
}