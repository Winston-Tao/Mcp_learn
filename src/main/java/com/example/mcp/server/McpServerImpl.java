package com.example.mcp.server;

import com.example.mcp.config.ServerConfig;
import com.example.mcp.tools.McpTool;
import com.example.mcp.resources.McpResourceProvider;
import com.example.mcp.prompts.McpPromptProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class McpServerImpl {

    private static final Logger logger = LoggerFactory.getLogger(McpServerImpl.class);

    private final ServerConfig serverConfig;
    private final ObjectMapper objectMapper;
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    private final Map<String, McpResourceProvider> resourceProviders = new ConcurrentHashMap<>();
    private final Map<String, McpPromptProvider> promptProviders = new ConcurrentHashMap<>();
    private final AtomicLong requestIdCounter = new AtomicLong(1);

    @Autowired
    public McpServerImpl(ServerConfig serverConfig, ObjectMapper objectMapper) {
        this.serverConfig = serverConfig;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing MCP Server: {} v{}",
            serverConfig.getName(), serverConfig.getVersion());
        logger.info("Server capabilities: tools={}, resources={}, prompts={}, logging={}",
            serverConfig.getCapabilities().isTools(),
            serverConfig.getCapabilities().isResources(),
            serverConfig.getCapabilities().isPrompts(),
            serverConfig.getCapabilities().isLogging());
    }

    public Mono<McpMessage> handleMessage(McpMessage message) {
        try {
            if (message.isRequest()) {
                return handleRequest(message);
            } else if (message.isNotification()) {
                return handleNotification(message);
            } else {
                return Mono.just(McpMessage.error(message.getId(),
                    McpError.invalidRequest()));
            }
        } catch (Exception e) {
            logger.error("Error handling message: {}", message, e);
            return Mono.just(McpMessage.error(message.getId(),
                McpError.internalError(e.getMessage())));
        }
    }

    private Mono<McpMessage> handleRequest(McpMessage request) {
        String method = request.getMethod();

        switch (method) {
            case "initialize":
                return handleInitialize(request);
            case "tools/list":
                return handleToolsList(request);
            case "tools/call":
                return handleToolsCall(request);
            case "resources/list":
                return handleResourcesList(request);
            case "resources/read":
                return handleResourcesRead(request);
            case "prompts/list":
                return handlePromptsList(request);
            case "prompts/get":
                return handlePromptsGet(request);
            default:
                return Mono.just(McpMessage.error(request.getId(),
                    McpError.methodNotFound(method)));
        }
    }

    private Mono<McpMessage> handleNotification(McpMessage notification) {
        String method = notification.getMethod();
        logger.debug("Received notification: {}", method);

        switch (method) {
            case "notifications/initialized":
                logger.info("Client initialized");
                break;
            case "logging/setLevel":
                logger.info("Log level changed");
                break;
            default:
                logger.warn("Unknown notification method: {}", method);
        }

        return Mono.empty();
    }

    private Mono<McpMessage> handleInitialize(McpMessage request) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2025-06-18");

        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", serverConfig.getName());
        serverInfo.put("version", serverConfig.getVersion());
        result.put("serverInfo", serverInfo);

        Map<String, Object> capabilities = new HashMap<>();

        if (serverConfig.getCapabilities().isTools()) {
            capabilities.put("tools", Map.of("listChanged", false));
        }

        if (serverConfig.getCapabilities().isResources()) {
            Map<String, Object> resourcesCapability = new HashMap<>();
            resourcesCapability.put("subscribe", serverConfig.getCapabilities().isResourceSubscriptions());
            resourcesCapability.put("listChanged", false);
            capabilities.put("resources", resourcesCapability);
        }

        if (serverConfig.getCapabilities().isPrompts()) {
            capabilities.put("prompts", Map.of("listChanged", false));
        }

        if (serverConfig.getCapabilities().isLogging()) {
            capabilities.put("logging", Collections.emptyMap());
        }

        result.put("capabilities", capabilities);

        return Mono.just(McpMessage.response(request.getId(), result));
    }

    private Mono<McpMessage> handleToolsList(McpMessage request) {
        List<Map<String, Object>> toolList = new ArrayList<>();

        for (McpTool tool : tools.values()) {
            Map<String, Object> toolInfo = new HashMap<>();
            toolInfo.put("name", tool.getName());
            toolInfo.put("description", tool.getDescription());
            toolInfo.put("inputSchema", tool.getInputSchema());
            toolList.add(toolInfo);
        }

        Map<String, Object> result = Map.of("tools", toolList);
        return Mono.just(McpMessage.response(request.getId(), result));
    }

    private Mono<McpMessage> handleToolsCall(McpMessage request) {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            return Mono.just(McpMessage.error(request.getId(),
                McpError.invalidParams("Missing parameters")));
        }

        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        if (toolName == null) {
            return Mono.just(McpMessage.error(request.getId(),
                McpError.invalidParams("Missing tool name")));
        }

        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return Mono.just(McpMessage.error(request.getId(),
                McpError.methodNotFound("Tool not found: " + toolName)));
        }

        return tool.execute(arguments != null ? arguments : Collections.emptyMap())
            .map(result -> McpMessage.response(request.getId(), Map.of("content", result)))
            .onErrorReturn(McpMessage.error(request.getId(),
                McpError.internalError("Tool execution failed")));
    }

    private Mono<McpMessage> handleResourcesList(McpMessage request) {
        List<Map<String, Object>> resourceList = new ArrayList<>();

        for (McpResourceProvider provider : resourceProviders.values()) {
            resourceList.addAll(provider.listResources());
        }

        Map<String, Object> result = Map.of("resources", resourceList);
        return Mono.just(McpMessage.response(request.getId(), result));
    }

    private Mono<McpMessage> handleResourcesRead(McpMessage request) {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            return Mono.just(McpMessage.error(request.getId(),
                McpError.invalidParams("Missing parameters")));
        }

        String uri = (String) params.get("uri");
        if (uri == null) {
            return Mono.just(McpMessage.error(request.getId(),
                McpError.invalidParams("Missing URI")));
        }

        for (McpResourceProvider provider : resourceProviders.values()) {
            if (provider.canHandle(uri)) {
                return provider.readResource(uri)
                    .map(content -> McpMessage.response(request.getId(), Map.of("contents", content)))
                    .onErrorReturn(McpMessage.error(request.getId(),
                        McpError.internalError("Resource read failed")));
            }
        }

        return Mono.just(McpMessage.error(request.getId(),
            McpError.methodNotFound("Resource not found: " + uri)));
    }

    private Mono<McpMessage> handlePromptsList(McpMessage request) {
        List<Map<String, Object>> promptList = new ArrayList<>();

        for (McpPromptProvider provider : promptProviders.values()) {
            promptList.addAll(provider.listPrompts());
        }

        Map<String, Object> result = Map.of("prompts", promptList);
        return Mono.just(McpMessage.response(request.getId(), result));
    }

    private Mono<McpMessage> handlePromptsGet(McpMessage request) {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            return Mono.just(McpMessage.error(request.getId(),
                McpError.invalidParams("Missing parameters")));
        }

        String name = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        if (name == null) {
            return Mono.just(McpMessage.error(request.getId(),
                McpError.invalidParams("Missing prompt name")));
        }

        for (McpPromptProvider provider : promptProviders.values()) {
            if (provider.hasPrompt(name)) {
                return provider.getPrompt(name, arguments != null ? arguments : Collections.emptyMap())
                    .map(result -> McpMessage.response(request.getId(), result))
                    .onErrorReturn(McpMessage.error(request.getId(),
                        McpError.internalError("Prompt generation failed")));
            }
        }

        return Mono.just(McpMessage.error(request.getId(),
            McpError.methodNotFound("Prompt not found: " + name)));
    }

    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
        logger.info("Registered tool: {}", tool.getName());
    }

    public void registerResourceProvider(String name, McpResourceProvider provider) {
        resourceProviders.put(name, provider);
        logger.info("Registered resource provider: {}", name);
    }

    public void registerPromptProvider(String name, McpPromptProvider provider) {
        promptProviders.put(name, provider);
        logger.info("Registered prompt provider: {}", name);
    }

    public long generateRequestId() {
        return requestIdCounter.getAndIncrement();
    }
}