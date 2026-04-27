package com.example.demo.service;

import com.example.demo.domain.Message;
import org.springframework.scheduling.annotation.Scheduled; // 1. Added import
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    // Maps Tenant ID to a list of active SseEmitters (handles multiple agents/tabs for one shop)
    private final Map<Long, List<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    public void addEmitter(Long tenantId, SseEmitter emitter) {
        tenantEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Clean up the emitter when the connection is closed, timed out, or errors out
        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError((e) -> removeEmitter(tenantId, emitter));
    }

    private void removeEmitter(Long tenantId, SseEmitter emitter) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    public void pushMessageToTenant(Long tenantId, Message message) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    // Send an event named "newMessage" containing the serialized Message object
                    emitter.send(SseEmitter.event().name("newMessage").data(message));
                } catch (IOException e) {
                    emitter.completeWithError(e); // This will trigger removal via onError
                }
            }
        }
    }

    // 2. ADDED THIS NEW METHOD
    @Scheduled(fixedRate = 30000) // Runs every 30 seconds
    public void sendHeartbeat() {
        tenantEmitters.forEach((tenantId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    // Send a lightweight "ping" event to keep the connection alive
                    emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                } catch (IOException e) {
                    // If the ping fails, the browser was closed. Safely trigger removal.
                    emitter.completeWithError(e);
                }
            }
        });
    }
}