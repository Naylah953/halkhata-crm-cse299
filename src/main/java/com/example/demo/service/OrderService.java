package com.example.demo.service;

import com.example.demo.domain.*;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.OrderItemRequest;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository userRepository;

    // Helper to get the logged-in staff member (and their Tenant)
    private AppUser getStaffFromUsername(String username) {
        return userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
    }

    @Transactional // CRITICAL: Rolls back everything if an error occurs!
    public OrderDto createOrder(String username, OrderCreateRequest request) {
        AppUser staff = getStaffFromUsername(username);
        Tenant tenant = staff.getTenant();

        // 1. Fetch the Customer
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenant.getId())
                .orElseThrow(() -> new RuntimeException("Customer not found in your shop."));

        // 2. Initialize the Order (Total Amount starts at 0, we calculate it below)
        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .deliveryMethod(request.getDeliveryMethod())
                .paymentMethod(request.getPaymentMethod())
                .customer(customer)
                .staff(staff)
                .tenant(tenant)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        // 3. Process each item in the cart
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndTenantId(itemRequest.getProductId(), tenant.getId())
                    .orElseThrow(() -> new RuntimeException("Product ID " + itemRequest.getProductId() + " not found."));

            // Stock Check
            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + product.getBaseName() + ". Only " + product.getQuantity() + " left.");
            }

            // Deduct the stock
            product.setQuantity(product.getQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            // Create the Line Item (Locking in the price)
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            // Add item to the order (This uses our helper method to sync the relationship)
            order.addOrderItem(orderItem);

            // Calculate running total
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            calculatedTotal = calculatedTotal.add(lineTotal);
        }

        // 4. Set the final order total
        order.setTotalAmount(calculatedTotal);

        // 5. Update the Customer's lifetime stats
        customer.setOrderCount(customer.getOrderCount() + 1);
        customer.setTotalSpent(customer.getTotalSpent().add(calculatedTotal));
        customerRepository.save(customer);

        // 6. Save the Order (Because of CascadeType.ALL, this automatically saves the OrderItems too!)
        order = orderRepository.save(order);

        return mapToDto(order);
    }

    public List<OrderDto> getOrders(String username) {
        Tenant tenant = getStaffFromUsername(username).getTenant();
        return orderRepository.findAllByTenantId(tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // --- NEW METHOD ADDED ---
    public List<OrderDto> getOrdersByCustomer(String username, Long customerId) {
        Tenant tenant = getStaffFromUsername(username).getTenant();

        // Fetches orders strictly tied to this specific customer AND this specific shop
        return orderRepository.findAllByCustomerIdAndTenantId(customerId, tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private OrderDto mapToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomer().getId());
        dto.setCustomerName(order.getCustomer().getFullName());
        dto.setStaffName(order.getStaff().getFullName());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setDeliveryMethod(order.getDeliveryMethod());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCreatedAt(order.getCreatedAt());

        // NEW: Map the Order Items into the DTO safely
        if (order.getItems() != null) {
            List<OrderDto.OrderItemDto> itemDtos = order.getItems().stream().map(item -> {
                OrderDto.OrderItemDto itemDto = new OrderDto.OrderItemDto();
                itemDto.setProductId(item.getProduct().getId());
                itemDto.setProductName(item.getProduct().getBaseName());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setPrice(item.getUnitPrice());
                return itemDto;
            }).collect(Collectors.toList());
            dto.setItems(itemDtos);
        }

        return dto;
    }
}