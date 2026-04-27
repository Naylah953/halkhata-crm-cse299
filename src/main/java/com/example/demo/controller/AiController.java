package com.example.demo.controller;

import com.example.demo.dto.ProductSchemaCreateRequest;
import com.example.demo.service.AiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {


    private final AiService aiService;

    @PostMapping("/generate-schema")
    public ResponseEntity<ProductSchemaCreateRequest> generateSchema(@RequestBody AiPromptRequest request) {
        // We do NOT require a Principal/JWT here if you want it to be a free tool,
        // but typically you'd secure this so random people can't use your API key!
        return ResponseEntity.ok(aiService.generateSchemaBlueprint(request.getPrompt()));
    }
}

// A simple local DTO to catch the user's chat message
@Data
class AiPromptRequest {
    private String prompt;
}