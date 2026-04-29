package com.example.demo.dto;

import com.example.demo.domain.enums.DeliveryMethod;
import com.example.demo.domain.enums.PaymentMethod;
import lombok.Data;
import java.util.List;

@Data
public class DraftOverrideRequest {
    private String overridePhone;
    private String overrideAddress;
    private DeliveryMethod overrideDelivery;
    private PaymentMethod overridePayment;
    private List<OrderItemRequest> items;
}