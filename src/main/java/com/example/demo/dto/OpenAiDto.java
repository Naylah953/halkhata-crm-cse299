package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

public class OpenAiDto {

    @Data @Builder
    public static class Request {
        private String model;
        private List<Message> messages;
    }

    @Data @Builder
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