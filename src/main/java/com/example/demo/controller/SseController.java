package com.example.demo.controller;

import com.example.demo.domain.AppUser;
import com.example.demo.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessages(@AuthenticationPrincipal AppUser currentUser) {
        // Create an emitter with a 1-hour timeout (3,600,000 milliseconds)
        SseEmitter emitter = new SseEmitter(3600000L);

        // Extract the Tenant ID from the authenticated user
        Long tenantId = currentUser.getTenant().getId();

        sseService.addEmitter(tenantId, emitter);

        return emitter;
    }
}