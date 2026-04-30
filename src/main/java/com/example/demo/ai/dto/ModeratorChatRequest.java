package com.example.demo.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ModeratorChatRequest(
        @NotBlank(message = "Message cannot be empty")
        String message,

        // @NotNull removed to allow global shop queries without a selected customer
        String selectedContactId
) {
    // Keep custom getters for backward compatibility
    public String getMessage() {
        return message;
    }

    public String getContactId() {
        return selectedContactId;
    }
}