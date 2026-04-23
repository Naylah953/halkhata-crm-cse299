package com.example.demo.service;

import com.example.demo.dto.ProductSchemaCreateRequest;
import com.example.demo.dto.gemini.GeminiRequest;
import com.example.demo.dto.gemini.GeminiResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class AiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;

    public AiService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api.url}") String apiUrl,
            @Value("${gemini.api.key}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public ProductSchemaCreateRequest generateSchemaBlueprint(String userPrompt) {
        // 1. The Secret Sauce: The System Prompt
        String systemPrompt = "You are an e-commerce database architect. " +
                "The user will describe a product category they want to sell. " +
                "Generate a JSON object with exactly two fields: 'name' (a string representing the category name, like 'Clothing' or 'Books') " +
                "and 'schemaDefinition' (a map of attribute rules). " +
                "Each rule in schemaDefinition must have a 'type' (strictly 'string', 'number', 'boolean', or 'enum'), " +
                "a 'required' boolean, and if it is an enum, an 'options' array of strings. " +
                "Output ONLY valid JSON and nothing else.";

        // 2. Build the Gemini Request Payload
        GeminiRequest request = GeminiRequest.builder()
                .systemInstruction(GeminiRequest.SystemInstruction.builder()
                        .parts(List.of(GeminiRequest.Part.builder().text(systemPrompt).build()))
                        .build())
                .generationConfig(GeminiRequest.GenerationConfig.builder()
                        .responseMimeType("application/json") // Forces pure JSON output
                        .build())
                .contents(List.of(GeminiRequest.Content.builder()
                        .role("user")
                        .parts(List.of(GeminiRequest.Part.builder().text(userPrompt).build()))
                        .build()))
                .build();

        // 3. Make the HTTP Call using modern RestClient
        GeminiResponse response = restClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        // 4. Extract and Validate the JSON
        try {
            // Get the raw JSON string from Gemini's response
            String jsonOutput = response.getCandidates().get(0).getContent().getParts().get(0).getText();

            // Map it perfectly to your existing DTO!
            return objectMapper.readValue(jsonOutput, ProductSchemaCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response into schema: " + e.getMessage(), e);
        }
    }
}