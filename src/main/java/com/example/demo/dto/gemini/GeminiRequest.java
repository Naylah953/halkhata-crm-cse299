package com.example.demo.dto.gemini;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GeminiRequest {
    private List<Content> contents;
    private SystemInstruction systemInstruction;
    private GenerationConfig generationConfig;

    @Data
    @Builder
    public static class Content {
        private String role; // Usually "user" or "model"
        private List<Part> parts;
    }

    @Data
    @Builder
    public static class Part {
        private String text;
    }

    @Data
    @Builder
    public static class SystemInstruction {
        private List<Part> parts;
    }

    @Data
    @Builder
    public static class GenerationConfig {
        private String responseMimeType; // We will set this to "application/json"
    }
}