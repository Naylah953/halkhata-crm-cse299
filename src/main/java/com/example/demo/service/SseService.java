package com.example.demo.service;

import com.example.demo.domain.Contact;
import com.example.demo.domain.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
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

    // ==========================================
    // NEW METHOD FOR AI UI UPDATES
    // ==========================================
    public void pushContactUpdateToTenant(Long tenantId, Contact contact) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    // Send an event named "contactUpdate" containing the updated Contact object
                    emitter.send(SseEmitter.event().name("contactUpdate").data(contact));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        }
    }

    public void pushMessageToTenant(Long tenantId, Message message) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    // Send an event named "newMessage" containing the serialized Message object
                    emitter.send(SseEmitter.event().name("newMessage").data(message));
                } catch (Exception e) {
                    // Catch ALL exceptions (like IllegalStateException from a closed browser tab)
                    deadEmitters.add(emitter);
                }
            }
            // Instantly prune all dead connections so we don't try them again
            emitters.removeAll(deadEmitters);
        }
    }

    @Scheduled(fixedRate = 30000) // Runs every 30 seconds
    public void sendHeartbeat() {
        tenantEmitters.forEach((tenantId, emitters) -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    // Send a lightweight "ping" event to keep the connection alive
                    emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                } catch (Exception e) {
                    // If the ping fails because the browser closed, mark it for removal
                    deadEmitters.add(emitter);
                }
            }
            // Prune dead tabs gracefully
            emitters.removeAll(deadEmitters);
        });
    }
}