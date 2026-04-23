package com.example.demo.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModeratorChatRequest(
        @NotBlank(message = "Message cannot be empty")
        String message,

        @NotNull(message = "A contact must be selected")
        String selectedContactId
) {
    // Keep her custom getters for backward compatibility
    public String getMessage() {
        return message;
    }

    public String getContactId() {
        return selectedContactId;
    }
}