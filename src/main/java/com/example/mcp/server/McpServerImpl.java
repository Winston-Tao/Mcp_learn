package com.example.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class McpServerImpl {

    private static final Logger logger = LoggerFactory.getLogger(McpServerImpl.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Function<JsonNode, CompletableFuture<Object>>> methodHandlers;
    private final Map<String, Object> serverCapabilities;
    private volatile String currentLogLevel = "INFO";

    @Value("${mcp.version:2025-06-18}")
    private String protocolVersion;

    @Value("${mcp.server.name:Java MCP Server}")
    private String serverName;

    @Value("${mcp.server.version:1.0.0}")
    private String serverVersion;

    @Autowired
    public McpServerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.methodHandlers = new ConcurrentHashMap<>();
        this.serverCapabilities = new HashMap<>();

        initializeMethodHandlers();
        initializeCapabilities();

        logger.info("MCP Server initialized with protocol version: {}", protocolVersion);
    }

    private void initializeMethodHandlers() {
        methodHandlers.put("initialize", this::handleInitialize);
        methodHandlers.put("ping", this::handlePing);
        methodHandlers.put("tools/list", this::handleToolsList);
        methodHandlers.put("tools/call", this::handleToolsCall);
        methodHandlers.put("resources/list", this::handleResourcesList);
        methodHandlers.put("resources/read", this::handleResourcesRead);
        methodHandlers.put("prompts/list", this::handlePromptsList);
        methodHandlers.put("prompts/get", this::handlePromptsGet);
        methodHandlers.put("logging/setLevel", this::handleLoggingSetLevel);

        logger.debug("Initialized {} method handlers", methodHandlers.size());
    }

    private void initializeCapabilities() {
        serverCapabilities.put("tools", true);
        serverCapabilities.put("resources", true);
        serverCapabilities.put("prompts", true);
        serverCapabilities.put("logging", true);
        serverCapabilities.put("sampling", false);
        serverCapabilities.put("roots", false);

        logger.debug("Server capabilities: {}", serverCapabilities);
    }

    public CompletableFuture<McpMessage> processMessage(McpMessage message) {
        logger.debug("Processing message: {}", message);

        if (message == null) {
            return CompletableFuture.completedFuture(
                McpMessage.createErrorResponse(null, McpError.invalidRequest("Message is null"))
            );
        }

        if (!"2.0".equals(message.getJsonrpc())) {
            return CompletableFuture.completedFuture(
                McpMessage.createErrorResponse(message.getId(),
                    McpError.invalidRequest("Invalid JSON-RPC version"))
            );
        }

        if (message.isNotification()) {
            handleNotification(message);
            return CompletableFuture.completedFuture(null);
        }

        if (message.isRequest()) {
            return handleRequest(message);
        }

        return CompletableFuture.completedFuture(
            McpMessage.createErrorResponse(message.getId(),
                McpError.invalidRequest("Invalid message format"))
        );
    }

    private CompletableFuture<McpMessage> handleRequest(McpMessage request) {
        String method = request.getMethod();
        Object id = request.getId();

        logger.debug("Handling request: method={}, id={}", method, id);

        Function<JsonNode, CompletableFuture<Object>> handler = methodHandlers.get(method);
        if (handler == null) {
            logger.warn("Method not found: {}", method);
            return CompletableFuture.completedFuture(
                McpMessage.createErrorResponse(id, McpError.methodNotFound(method))
            );
        }

        try {
            return handler.apply(request.getParams())
                .thenApply(result -> McpMessage.createResponse(id, result))
                .exceptionally(throwable -> {
                    logger.error("Error handling request {}: {}", method, throwable.getMessage(), throwable);
                    return McpMessage.createErrorResponse(id,
                        McpError.internalError(throwable.getMessage()));
                });
        } catch (Exception e) {
            logger.error("Exception handling request {}: {}", method, e.getMessage(), e);
            return CompletableFuture.completedFuture(
                McpMessage.createErrorResponse(id, McpError.internalError(e.getMessage()))
            );
        }
    }

    private void handleNotification(McpMessage notification) {
        String method = notification.getMethod();
        logger.debug("Handling notification: method={}", method);

        Function<JsonNode, CompletableFuture<Object>> handler = methodHandlers.get(method);
        if (handler != null) {
            handler.apply(notification.getParams())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.warn("Error handling notification {}: {}", method, throwable.getMessage());
                    } else {
                        logger.debug("Notification {} handled successfully", method);
                    }
                });
        } else {
            logger.debug("No handler for notification method: {}", method);
        }
    }

    private CompletableFuture<Object> handleInitialize(JsonNode params) {
        logger.info("Handling initialize request");

        Map<String, Object> response = new HashMap<>();
        response.put("protocolVersion", protocolVersion);
        response.put("capabilities", serverCapabilities);
        response.put("serverInfo", Map.of(
            "name", serverName,
            "version", serverVersion
        ));

        logger.info("Server initialized successfully");
        return CompletableFuture.completedFuture(response);
    }

    private CompletableFuture<Object> handlePing(JsonNode params) {
        logger.debug("Handling ping request");
        return CompletableFuture.completedFuture(Map.of("status", "pong"));
    }

    private CompletableFuture<Object> handleToolsList(JsonNode params) {
        logger.debug("Handling tools/list request");
        return CompletableFuture.completedFuture(
            Map.of("tools", java.util.List.of())
        );
    }

    private CompletableFuture<Object> handleToolsCall(JsonNode params) {
        logger.debug("Handling tools/call request");
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Tools not implemented yet")
        );
    }

    private CompletableFuture<Object> handleResourcesList(JsonNode params) {
        logger.debug("Handling resources/list request");
        return CompletableFuture.completedFuture(
            Map.of("resources", java.util.List.of())
        );
    }

    private CompletableFuture<Object> handleResourcesRead(JsonNode params) {
        logger.debug("Handling resources/read request");
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Resources not implemented yet")
        );
    }

    private CompletableFuture<Object> handlePromptsList(JsonNode params) {
        logger.debug("Handling prompts/list request");
        return CompletableFuture.completedFuture(
            Map.of("prompts", java.util.List.of())
        );
    }

    private CompletableFuture<Object> handlePromptsGet(JsonNode params) {
        logger.debug("Handling prompts/get request");
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Prompts not implemented yet")
        );
    }

    private CompletableFuture<Object> handleLoggingSetLevel(JsonNode params) {
        logger.debug("Handling logging/setLevel request");

        try {
            if (params == null || !params.has("level")) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Missing required parameter: level")
                );
            }

            String level = params.get("level").asText().toUpperCase();

            // Validate log level
            if (!isValidLogLevel(level)) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Invalid log level: " + level)
                );
            }

            String previousLevel = currentLogLevel;
            currentLogLevel = level;

            logger.info("Log level changed from {} to {}", previousLevel, level);

            // Send logging entry notification about level change
            sendLoggingEntry("INFO", "Log level changed to " + level,
                Map.of("previousLevel", previousLevel, "newLevel", level));

            return CompletableFuture.completedFuture(Map.of());

        } catch (Exception e) {
            logger.error("Error handling logging/setLevel: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private boolean isValidLogLevel(String level) {
        return level.matches("TRACE|DEBUG|INFO|WARN|ERROR");
    }

    public void sendLoggingEntry(String level, String message, Object data) {
        // This method would typically send a logging/entry notification to the client
        // For now, we'll just log it locally
        logger.info("Logging entry [{}]: {} - Data: {}", level, message, data);

        // In a complete implementation, you would send this as a notification:
        // McpMessage notification = McpMessage.createNotification("logging/entry",
        //     objectMapper.valueToTree(Map.of(
        //         "level", level,
        //         "message", message,
        //         "data", data,
        //         "timestamp", Instant.now().toString()
        //     )));
        // // Send notification to client...
    }

    public void registerMethodHandler(String method, Function<JsonNode, CompletableFuture<Object>> handler) {
        methodHandlers.put(method, handler);
        logger.debug("Registered handler for method: {}", method);
    }

    public Map<String, Object> getServerCapabilities() {
        return new HashMap<>(serverCapabilities);
    }
}