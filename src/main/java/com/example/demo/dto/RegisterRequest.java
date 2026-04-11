package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String shopName;
    private String businessCategory; // Add this line
    private String adminFullName;
    private String phoneNumber;
    private String pin;
}