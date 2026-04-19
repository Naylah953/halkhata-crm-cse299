package com.example.demo.dto;

import com.example.demo.domain.enums.DeliveryMethod;
import com.example.demo.domain.enums.OrderStatus;
import com.example.demo.domain.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}