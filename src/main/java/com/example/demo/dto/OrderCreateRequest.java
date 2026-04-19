package com.example.demo.dto;

import com.example.demo.domain.enums.DeliveryMethod;
import com.example.demo.domain.enums.PaymentMethod;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    private Long customerId;
    private DeliveryMethod deliveryMethod;
    private PaymentMethod paymentMethod;
    private List<OrderItemRequest> items;
}