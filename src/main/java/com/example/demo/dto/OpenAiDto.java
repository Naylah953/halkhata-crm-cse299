package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

public class OpenAiDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String model;
        private List<Message> messages;
        private Integer max_tokens; // ADDED: To prevent truncated responses
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class Response {
        private List<Choice> choices;

        @Data
        public static class Choice {
            private Message message;
        }
    }
}