package com.example.demo.dto;

import lombok.Data;

@Data
public class CustomerCreateRequest {
    private String fullName;
    private String phoneNumber;
    private String email;
    private String address;

    // Optional: If the shop owner is promoting an existing messenger lead
    private String contactId;
}