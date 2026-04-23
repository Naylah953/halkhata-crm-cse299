package com.example.demo.controller;

import com.example.demo.dto.AiAnalyticsResponse;
import com.example.demo.service.AiAnalyticsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAnalyticsController {

    private final AiAnalyticsService aiAnalyticsService;

    @PostMapping("/analytics")
    public ResponseEntity<AiAnalyticsResponse> askAiAnalytics(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestBody AiQueryRequest request) {

        // The Service now dynamically fetches the schema context from the database!
        AiAnalyticsResponse response = aiAnalyticsService.processAnalyticsQuery(
                request.getPrompt(),
                tenantId
        );

        return ResponseEntity.ok(response);
    }
}

// A simple DTO strictly for this controller to catch the frontend JSON
@Data
class AiQueryRequest {
    private String prompt;
}