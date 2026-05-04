package com.example.demo.service;

import com.example.demo.domain.*;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.OrderItemRequest;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    OutboundMessageService outboundMessageService;

    @Autowired private ContactRepo contactRepo;

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

        //FARIZA CHANGE - PDF
        Contact correspondingContact = contactRepo.findByCustomer_IdAndTenantId(customer.getId(), tenant.getId());

        outboundMessageService.sendPdfAsAttachment(correspondingContact.getId(), order.getId());

        return mapToDto(order);
    }



    public List<OrderDto> getOrders(String username) {
        Tenant tenant = getStaffFromUsername(username).getTenant();
        return orderRepository.findAllByTenantId(tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getOrdersByCustomer(String username, Long customerId) {
        Tenant tenant = getStaffFromUsername(username).getTenant();

        // Fetches orders strictly tied to this specific customer AND this specific shop
        return orderRepository.findAllByCustomerIdAndTenantId(customerId, tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getOrdersByProduct(String username, Long productId) {
        Tenant tenant = getStaffFromUsername(username).getTenant();

        return orderRepository.findByItems_ProductIdAndTenantIdOrderByCreatedAtDesc(productId, tenant.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto cancelOrder(String username, Long orderId) {
        AppUser staff = getStaffFromUsername(username);
        Tenant tenant = staff.getTenant();

        Order order = orderRepository.findByIdAndTenantId(orderId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Order not found or unauthorized access."));

        // 1. State Machine Enforcement
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that is already " + order.getStatus().name());
        }

        // 2. Inventory Restoration
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // 3. Customer Lifetime Stats Reversal
        Customer customer = order.getCustomer();
        customer.setTotalSpent(customer.getTotalSpent().subtract(order.getTotalAmount()));
        customer.setOrderCount(customer.getOrderCount() - 1);
        customerRepository.save(customer);

        // 4. Finalize Cancellation
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        return mapToDto(order);
    }

    // --- NEW METHOD: Generic Status Update ---
    @Transactional
    public OrderDto updateOrderStatus(String username, Long orderId, String newStatus) {
        AppUser staff = getStaffFromUsername(username);
        Tenant tenant = staff.getTenant();

        // 1. Fetch Order and enforce strict Tenant isolation
        Order order = orderRepository.findByIdAndTenantId(orderId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Order not found or unauthorized access."));

        // 2. Validate the incoming status string against your Enum
        OrderStatus parsedStatus;
        try {
            parsedStatus = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status provided: " + newStatus);
        }

        // 3. If the user selects "CANCELLED" from the dropdown, run the full cancellation logic
        if (parsedStatus == OrderStatus.CANCELLED) {
            return cancelOrder(username, orderId);
        }

        // 4. Otherwise, simply apply the new status
        order.setStatus(parsedStatus);
        order = orderRepository.save(order);

        // 5. Return mapped DTO to update the frontend instantly
        return mapToDto(order);
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

        // Map the Order Items into the DTO safely
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