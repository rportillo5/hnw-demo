package com.edwardjones.demo.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all open SSE connections from React dashboard clients.
 * Thread-safe — AlertPollingService calls broadcast() from a virtual thread.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for the given clientId.
     * Called when React mounts the AlertTray component.
     */
    public SseEmitter register(String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            log.debug("SSE connection completed for client: {}", clientId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            log.debug("SSE connection timed out for client: {}", clientId);
        });
        emitter.onError(e -> {
            emitters.remove(clientId);
            log.debug("SSE connection error for client {}: {}", clientId, e.getMessage());
        });

        emitters.put(clientId, emitter);
        log.info("SSE client registered: {} (total connected: {})", clientId, emitters.size());

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE handshake to client {}: {}", clientId, e.getMessage());
        }

        return emitter;
    }

    /**
     * Broadcast an event to all connected React clients.
     * Called by AlertPollingService when a new Databricks alert is detected.
     */
    public void broadcast(String eventName, Object payload) {
        if (emitters.isEmpty()) {
            log.debug("No SSE clients connected — skipping broadcast of event: {}", eventName);
            return;
        }

        log.info("Broadcasting '{}' event to {} client(s)", eventName, emitters.size());

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(eventName)
                        .data(payload)
                );
            } catch (IOException e) {
                log.warn("Failed to send SSE to client {}, removing: {}", clientId, e.getMessage());
                emitters.remove(clientId);
            }
        });
    }

    public int connectedClientCount() {
        return emitters.size();
    }
}
