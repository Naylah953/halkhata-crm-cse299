package com.example.demo.service;

import com.example.demo.dto.ProductSchemaCreateRequest;
import com.example.demo.dto.OpenAiDto;
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
    private final String model;

    public AiService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${openrouter.api.url}") String apiUrl,
            @Value("${openrouter.api.key}") String apiKey,
            @Value("${openrouter.api.model}") String model) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public ProductSchemaCreateRequest generateSchemaBlueprint(String userPrompt) {
        // 1. The Secret Sauce: The System Prompt
        String systemPrompt = "You are an e-commerce database architect. " +
                "The user will describe a product category they want to sell. " +
                "Generate a JSON object with exactly two fields: 'name' (a string representing the category name, like 'Clothing' or 'Books') " +
                "and 'schemaDefinition' (a map of attribute rules). " +
                "Each rule in schemaDefinition must have a 'type' (strictly 'string', 'number', 'boolean', or 'enum'), " +
                "a 'required' boolean, and if it is an enum, an 'options' array of strings. " +
                "Output ONLY valid JSON and nothing else. Do not wrap the output in markdown blocks.";

        // 2. Build the OpenRouter Request Payload
        OpenAiDto.Request request = OpenAiDto.Request.builder()
                .model(model)
                .max_tokens(2000) // ADDED: Give the AI enough tokens to finish the JSON
                .messages(List.of(
                        OpenAiDto.Message.builder().role("system").content(systemPrompt).build(),
                        OpenAiDto.Message.builder().role("user").content(userPrompt).build()
                ))
                .build();

        // 3. Make the HTTP Call using modern RestClient
        OpenAiDto.Response response = restClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "http://localhost:8080") // Required by OpenRouter/Cloudflare
                .body(request)
                .retrieve()
                .body(OpenAiDto.Response.class);

        // 4. Extract and Validate the JSON
        try {
            // Get the raw JSON string from OpenRouter's response
            String jsonOutput = response.getChoices().get(0).getMessage().getContent();

            // Clean the AI output just in case it wraps the JSON in markdown blocks
            jsonOutput = jsonOutput.replaceAll("```json", "").replaceAll("```", "").trim();

            // Map it perfectly to your existing DTO!
            return objectMapper.readValue(jsonOutput, ProductSchemaCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response into schema: " + e.getMessage(), e);
        }
    }
}