package com.example.demo.repository;

import com.example.demo.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders for the specific shop
    List<Order> findAllByTenantId(Long tenantId);

    // Get a specific order, ensuring it belongs to the shop
    Optional<Order> findByIdAndTenantId(Long id, Long tenantId);

    // Get all orders for a specific customer within a specific shop
    List<Order> findAllByCustomerIdAndTenantId(Long customerId, Long tenantId);

    // Phase 1: Securely fetch all orders containing a specific product by ID
    List<Order> findByItems_ProductIdAndTenantIdOrderByCreatedAtDesc(Long productId, Long tenantId);

    // Phase 2: Relational Bulk Fetch - Finds all distinct orders containing a product name
    List<Order> findDistinctByItems_Product_BaseNameContainingIgnoreCaseAndTenantId(String productName, Long tenantId);
}