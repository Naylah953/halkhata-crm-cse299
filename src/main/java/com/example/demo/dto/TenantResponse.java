package com.example.demo.dto;

public class TenantResponse {

    private String shopName; // Kept as shopName so the frontend HTML doesn't break!
    private String businessCategory;

    // The three new fields
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;

    public TenantResponse(String shopName, String businessCategory, String businessAddress, String contactPhone, String contactEmail) {
        this.shopName = shopName;
        this.businessCategory = businessCategory;
        this.businessAddress = businessAddress;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getShopName() {
        return shopName;
    }

    public String getBusinessCategory() {
        return businessCategory;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }
}