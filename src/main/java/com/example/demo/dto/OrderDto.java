package com.example.demo.dto;

import com.example.demo.domain.enums.DeliveryMethod;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.domain.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private String staffName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private DeliveryMethod deliveryMethod;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;

    // NEW: The nested items list
    private List<OrderItemDto> items;

    @Data
    public static class OrderItemDto {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}