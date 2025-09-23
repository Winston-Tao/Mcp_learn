package com.example.mcp.transport;

import com.example.mcp.server.McpMessage;
import com.example.mcp.server.McpServerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/mcp")
public class McpSseController {

    private static final Logger logger = LoggerFactory.getLogger(McpSseController.class);

    private final McpServerImpl mcpServer;
    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<String>> connections = new ConcurrentHashMap<>();
    private final AtomicLong connectionIdCounter = new AtomicLong(1);

    @Autowired
    public McpSseController(McpServerImpl mcpServer, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.objectMapper = objectMapper;
    }

    /**
     * Establish SSE connection and return endpoint information
     */
    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> establishConnection() {
        String connectionId = "mcp-" + connectionIdCounter.getAndIncrement();
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        connections.put(connectionId, sink);
        logger.info("New SSE connection established: {}", connectionId);

        try {
            // Send endpoint event with message endpoint URL
            Map<String, Object> endpointEvent = Map.of(
                "type", "endpoint",
                "uri", "/mcp/message",
                "connectionId", connectionId
            );
            String eventData = "event: endpoint\ndata: " + objectMapper.writeValueAsString(endpointEvent) + "\n\n";
            sink.tryEmitNext(eventData);

            logger.debug("Sent endpoint event to connection: {}", connectionId);
        } catch (Exception e) {
            logger.error("Failed to send endpoint event", e);
            connections.remove(connectionId);
            sink.tryEmitError(e);
        }

        return sink.asFlux()
            .doOnCancel(() -> {
                logger.info("SSE connection cancelled: {}", connectionId);
                connections.remove(connectionId);
            })
            .doOnTerminate(() -> {
                logger.info("SSE connection terminated: {}", connectionId);
                connections.remove(connectionId);
            })
            .mergeWith(Flux.interval(Duration.ofSeconds(30))
                .map(tick -> "event: heartbeat\ndata: {\"type\":\"heartbeat\"}\n\n"));
    }

    /**
     * Handle MCP messages via HTTP POST
     */
    @PostMapping(path = "/message",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> handleMessage(
            @RequestBody McpMessage request,
            @RequestHeader(value = "X-MCP-Connection-ID", required = false) String connectionId) {

        logger.debug("Received MCP message: {} from connection: {}", request, connectionId);

        return mcpServer.handleMessage(request)
            .map(response -> {
                if (response != null) {
                    // Send response via SSE if we have a connection
                    if (connectionId != null && connections.containsKey(connectionId)) {
                        sendViaSse(connectionId, response);
                        return ResponseEntity.ok(Map.<String, Object>of("status", "sent"));
                    } else {
                        // Fallback: return response directly
                        return ResponseEntity.ok(responseToMap(response));
                    }
                } else {
                    return ResponseEntity.ok(Map.<String, Object>of("status", "processed"));
                }
            })
            .onErrorReturn(ResponseEntity.status(500)
                .body(Map.of("error", "Internal server error")));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
            "status", "OK",
            "transport", "HTTP+SSE",
            "activeConnections", connections.size(),
            "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(health);
    }

    /**
     * Server info endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = Map.of(
            "name", "MCP Java Server",
            "transport", "HTTP+SSE",
            "version", "1.0.0",
            "description", "HTTP+SSE transport for Model Context Protocol",
            "endpoints", Map.of(
                "sse", "/mcp/sse",
                "message", "/mcp/message",
                "health", "/mcp/health"
            )
        );
        return ResponseEntity.ok(info);
    }

    private void sendViaSse(String connectionId, McpMessage message) {
        Sinks.Many<String> sink = connections.get(connectionId);
        if (sink != null) {
            try {
                String eventData = "event: message\ndata: " +
                    objectMapper.writeValueAsString(responseToMap(message)) + "\n\n";
                sink.tryEmitNext(eventData);
                logger.debug("Sent response via SSE to connection: {}", connectionId);
            } catch (Exception e) {
                logger.error("Failed to send message via SSE to connection: " + connectionId, e);
                connections.remove(connectionId);
                sink.tryEmitError(e);
            }
        }
    }

    private Map<String, Object> responseToMap(McpMessage message) {
        Map<String, Object> map = Map.of(
            "jsonrpc", message.getJsonrpc(),
            "id", message.getId() != null ? message.getId() : "",
            "result", message.getResult() != null ? message.getResult() : Map.of()
        );
        return map;
    }
}