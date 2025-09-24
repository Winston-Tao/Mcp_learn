package com.example.mcp.transport;

import com.example.mcp.server.McpError;
import com.example.mcp.server.McpMessage;
import com.example.mcp.server.McpServerImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("${mcp.transport.http.endpoint:/api/mcp}")
@CrossOrigin(origins = "*")
public class HttpTransportController {

    private static final Logger logger = LoggerFactory.getLogger(HttpTransportController.class);
    private static final Logger messageLogger = LoggerFactory.getLogger("com.example.mcp.transport.messages");

    private final McpServerImpl mcpServer;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    @Autowired
    public HttpTransportController(McpServerImpl mcpServer, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        logger.info("HTTP Transport Controller initialized");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<?>> handleMcpRequest(
            @RequestBody JsonNode requestBody,
            @RequestHeader HttpHeaders headers) {

        String acceptHeader = headers.getFirst(HttpHeaders.ACCEPT);
        boolean supportsSSE = acceptHeader != null &&
                             acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE);

        messageLogger.info("Incoming MCP request: {}", requestBody);
        logger.debug("Accept header: {}, supports SSE: {}", acceptHeader, supportsSSE);

        try {
            McpMessage request = objectMapper.treeToValue(requestBody, McpMessage.class);

            if (request == null) {
                return CompletableFuture.completedFuture(
                    createJsonResponse(McpMessage.createErrorResponse(null,
                        McpError.parseError("Invalid JSON structure")))
                );
            }

            return mcpServer.processMessage(request)
                .thenApply(response -> {
                    if (response == null) {
                        return ResponseEntity.noContent().build();
                    }

                    messageLogger.info("Outgoing MCP response: {}", response);

                    if (supportsSSE && shouldUseSSE(request)) {
                        return createSseResponse(response);
                    } else {
                        return createJsonResponse(response);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Error processing MCP request: {}", throwable.getMessage(), throwable);
                    McpMessage errorResponse = McpMessage.createErrorResponse(
                        request != null ? request.getId() : null,
                        McpError.internalError(throwable.getMessage())
                    );
                    return createJsonResponse(errorResponse);
                });

        } catch (Exception e) {
            logger.error("Error parsing MCP request: {}", e.getMessage(), e);
            McpMessage errorResponse = McpMessage.createErrorResponse(null,
                McpError.parseError("Failed to parse JSON-RPC message: " + e.getMessage()));
            return CompletableFuture.completedFuture(createJsonResponse(errorResponse));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok().body(java.util.Map.of(
            "status", "healthy",
            "timestamp", System.currentTimeMillis(),
            "version", "1.0.0"
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Object> serverInfo() {
        return ResponseEntity.ok().body(mcpServer.getServerCapabilities());
    }

    private ResponseEntity<?> createJsonResponse(McpMessage message) {
        try {
            JsonNode responseJson = objectMapper.valueToTree(message);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-cache")
                    .body(responseJson);
        } catch (Exception e) {
            logger.error("Error serializing response: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}");
        }
    }

    private ResponseEntity<?> createSseResponse(McpMessage message) {
        try {
            SseEmitter emitter = new SseEmitter(30000L);

            CompletableFuture.runAsync(() -> {
                try {
                    String messageData = objectMapper.writeValueAsString(message);
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .id(String.valueOf(message.getId()))
                            .name("message")
                            .data(messageData, MediaType.APPLICATION_JSON);

                    emitter.send(event);
                    emitter.complete();

                    logger.debug("SSE response sent successfully");
                } catch (IOException e) {
                    logger.error("Error sending SSE response: {}", e.getMessage(), e);
                    emitter.completeWithError(e);
                }
            }, executorService);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .body(emitter);

        } catch (Exception e) {
            logger.error("Error creating SSE response: {}", e.getMessage(), e);
            return createJsonResponse(McpMessage.createErrorResponse(
                message.getId(), McpError.internalError("Failed to create SSE response")
            ));
        }
    }

    private boolean shouldUseSSE(McpMessage request) {
        if (request == null || !request.isRequest()) {
            return false;
        }

        String method = request.getMethod();
        return method != null && (
            method.startsWith("tools/") ||
            method.startsWith("resources/") ||
            method.startsWith("prompts/") ||
            method.equals("sampling/createMessage")
        );
    }
}