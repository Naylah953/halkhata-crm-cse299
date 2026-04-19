package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerDto {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String address;
    private BigDecimal totalSpent;
    private Integer orderCount;
}