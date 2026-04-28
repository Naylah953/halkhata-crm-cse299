package com.example.demo.dto;

import lombok.Data;

@Data
public class TenantUpdateRequest {
    private String shopName;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;
    private String businessCategory;
    private String facebookPageId;
    private String pageAccessToken;

    // --- NEW: Global Auto-AI Toggle ---
    private Boolean enableAiReplies;
}