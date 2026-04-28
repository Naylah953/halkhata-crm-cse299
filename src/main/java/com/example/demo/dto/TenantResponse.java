package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TenantResponse {
    private String shopName;
    private String businessCategory;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;

    // The newly added Meta fields
    private String facebookPageId;
    private String pageAccessToken;

    // --- NEW: Global Auto-AI Toggle ---
    private boolean enableAiReplies;
}