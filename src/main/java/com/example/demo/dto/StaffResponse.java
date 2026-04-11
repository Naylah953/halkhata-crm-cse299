package com.example.demo.dto;

public class StaffResponse {

    private Long id; // NEW FIELD
    private String fullName;
    private String role;

    public StaffResponse(Long id, String fullName, String role) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}