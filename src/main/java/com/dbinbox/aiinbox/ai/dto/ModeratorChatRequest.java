package com.dbinbox.aiinbox.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for incoming moderator commands to the AI Assistant.
 */
public record ModeratorChatRequest(
        @NotBlank(message = "Message cannot be empty")
        String message,

        @NotNull(message = "A contact must be selected")
        String selectedContactId
) {
        public String getMessage()
        {
                return message;
        }

        public String getContactId()
        {
                return selectedContactId;
        }


}