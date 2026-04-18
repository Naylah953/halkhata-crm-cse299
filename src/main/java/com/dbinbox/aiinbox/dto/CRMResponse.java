package com.dbinbox.aiinbox.dto;

//DTO for outbound message
//Nested -- Recipient + Message --> CRMResponse
public record CRMResponse(Recipient recipient, Message message)
{
    public record Recipient(String id){};
    public record Message(String text){};
}

