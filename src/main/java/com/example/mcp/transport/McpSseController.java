package com.example.mcp.transport;

import com.example.mcp.server.McpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("${mcp.transport.http.endpoint:/api/mcp}")
public class McpSseController {

    private static final Logger logger = LoggerFactory.getLogger(McpSseController.class);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, SseEmitter> activeConnections;
    private final ScheduledExecutorService scheduler;

    @Autowired
    public McpSseController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.activeConnections = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);

        startHeartbeat();
        logger.info("MCP SSE Controller initialized");
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents(@RequestParam(required = false) String clientId) {
        if (clientId == null) {
            clientId = "client-" + System.currentTimeMillis();
        }

        logger.info("New SSE connection: {}", clientId);

        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        activeConnections.put(clientId, emitter);

        final String finalClientId = clientId;

        emitter.onCompletion(() -> {
            activeConnections.remove(finalClientId);
            logger.info("SSE connection completed: {}", finalClientId);
        });

        emitter.onError(throwable -> {
            activeConnections.remove(finalClientId);
            logger.warn("SSE connection error for {}: {}", finalClientId, throwable.getMessage());
        });

        emitter.onTimeout(() -> {
            activeConnections.remove(finalClientId);
            logger.info("SSE connection timeout: {}", finalClientId);
        });

        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id("init-" + System.currentTimeMillis())
                    .name("connection")
                    .data("Connected to MCP Server", MediaType.TEXT_PLAIN);
            emitter.send(event);
        } catch (IOException e) {
            logger.error("Error sending initial SSE message: {}", e.getMessage(), e);
            activeConnections.remove(finalClientId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @PostMapping("/broadcast")
    public String broadcastMessage(@RequestBody McpMessage message) {
        logger.debug("Broadcasting message to {} connections", activeConnections.size());

        int successCount = 0;
        int failCount = 0;

        for (var entry : activeConnections.entrySet()) {
            String clientId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                String messageData = objectMapper.writeValueAsString(message);
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .id(String.valueOf(message.getId()))
                        .name("message")
                        .data(messageData, MediaType.APPLICATION_JSON);

                emitter.send(event);
                successCount++;

            } catch (IOException e) {
                logger.warn("Failed to send message to client {}: {}", clientId, e.getMessage());
                activeConnections.remove(clientId);
                emitter.completeWithError(e);
                failCount++;
            }
        }

        logger.debug("Broadcast complete: {} success, {} failed", successCount, failCount);
        return String.format("Broadcast to %d connections (%d success, %d failed)",
                activeConnections.size(), successCount, failCount);
    }

    @GetMapping("/connections")
    public Object getActiveConnections() {
        return java.util.Map.of(
                "count", activeConnections.size(),
                "connections", activeConnections.keySet()
        );
    }

    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (activeConnections.isEmpty()) {
                return;
            }

            logger.debug("Sending heartbeat to {} connections", activeConnections.size());

            for (var entry : activeConnections.entrySet()) {
                String clientId = entry.getKey();
                SseEmitter emitter = entry.getValue();

                try {
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .id("heartbeat-" + System.currentTimeMillis())
                            .name("heartbeat")
                            .data("ping", MediaType.TEXT_PLAIN);

                    emitter.send(event);

                } catch (IOException e) {
                    logger.debug("Heartbeat failed for client {}, removing connection", clientId);
                    activeConnections.remove(clientId);
                    emitter.completeWithError(e);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);

        logger.info("Heartbeat scheduler started (30s interval)");
    }

    public void shutdown() {
        logger.info("Shutting down SSE controller...");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        activeConnections.values().forEach(SseEmitter::complete);
        activeConnections.clear();

        logger.info("SSE controller shutdown complete");
    }
}