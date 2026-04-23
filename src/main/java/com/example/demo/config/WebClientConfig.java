package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebClientConfig {

    // 1. The Builder Bean (Used by your other AI feature)
    // Provides a base builder for services that need to customize their own specific configurations.
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // 2. The Standard Client Bean (Used by AiAnalyticsService)
    // Takes the builder from above, finishes the build process, and provides a ready-to-use client.
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}