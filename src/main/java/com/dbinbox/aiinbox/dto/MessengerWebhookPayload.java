package com.dbinbox.aiinbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


//represents the message DTO
public record MessengerWebhookPayload(
        String object,
        List<MessengerEntry> entry
) {
    public record MessengerEntry(
            @JsonProperty("messaging") // Maps FB 'messaging' to your 'messageEventList'
            List<MessageEvent> messageEventList,
            String id,
            Long time
    ) {}

    public record MessageEvent(
            Recipient recipient,
            Sender sender,
            Long timestamp,
            Message message
    ) {}

    public record Recipient(String id) {}
    public record Sender(String id) {}

    public record Message(
            String mid,
            String text,
            List<Attachment> attachments // Added to handle images/audio
    ) {
        public Boolean isEcho()
        {
            return true;
        }

    }

    public record Attachment(
            String type, // "image", "audio", "video", "file"
            Payload payload
    ) {}

    public record Payload(
            String url // The temporary link to the media file
    ) {}
}