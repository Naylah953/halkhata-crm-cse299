package com.example.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class TenantUpdateRequest {
    private String shopName;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;
    private String businessCategory;
}