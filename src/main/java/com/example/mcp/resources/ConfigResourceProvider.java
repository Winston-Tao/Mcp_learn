package com.example.mcp.resources;

import com.example.mcp.config.ServerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ConfigResourceProvider extends AbstractResourceProvider {

    private static final String SCHEME = "config://";

    private final ServerConfig serverConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConfigResourceProvider(ServerConfig serverConfig, ObjectMapper objectMapper) {
        this.serverConfig = serverConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> listResources() {
        return List.of(
            createResource(
                SCHEME + "server",
                "Server Configuration",
                "Current server configuration and status",
                "application/json"
            ),
            createResource(
                SCHEME + "runtime",
                "Runtime Information",
                "JVM and system runtime information",
                "application/json"
            ),
            createResource(
                SCHEME + "status",
                "Server Status",
                "Current server status and uptime",
                "text/plain"
            )
        );
    }

    @Override
    public boolean canHandle(String uri) {
        return uri != null && uri.startsWith(SCHEME);
    }

    @Override
    public Mono<List<Map<String, Object>>> readResource(String uri) {
        return Mono.fromCallable(() -> {
            if (!canHandle(uri)) {
                throw new IllegalArgumentException("Cannot handle URI: " + uri);
            }

            String resourceType = uri.substring(SCHEME.length());

            return switch (resourceType) {
                case "server" -> getServerConfig();
                case "runtime" -> getRuntimeInfo();
                case "status" -> getServerStatus();
                default -> throw new IllegalArgumentException("Unknown config resource: " + resourceType);
            };
        });
    }

    private List<Map<String, Object>> getServerConfig() {
        try {
            String configJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(serverConfig);

            Map<String, Object> content = Map.of(
                "uri", SCHEME + "server",
                "mimeType", "application/json",
                "text", configJson
            );

            return List.of(content);

        } catch (Exception e) {
            logger.error("Error serializing server config", e);
            throw new RuntimeException("Failed to get server configuration", e);
        }
    }

    private List<Map<String, Object>> getRuntimeInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> runtimeInfo = Map.of(
                "jvm", Map.of(
                    "version", System.getProperty("java.version"),
                    "vendor", System.getProperty("java.vendor"),
                    "home", System.getProperty("java.home")
                ),
                "memory", Map.of(
                    "total", runtime.totalMemory(),
                    "free", runtime.freeMemory(),
                    "max", runtime.maxMemory(),
                    "used", runtime.totalMemory() - runtime.freeMemory()
                ),
                "system", Map.of(
                    "os", System.getProperty("os.name"),
                    "arch", System.getProperty("os.arch"),
                    "version", System.getProperty("os.version"),
                    "processors", runtime.availableProcessors()
                ),
                "environment", Map.of(
                    "user", System.getProperty("user.name"),
                    "home", System.getProperty("user.home"),
                    "workingDir", System.getProperty("user.dir")
                )
            );

            String runtimeJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(runtimeInfo);

            Map<String, Object> content = Map.of(
                "uri", SCHEME + "runtime",
                "mimeType", "application/json",
                "text", runtimeJson
            );

            return List.of(content);

        } catch (Exception e) {
            logger.error("Error getting runtime info", e);
            throw new RuntimeException("Failed to get runtime information", e);
        }
    }

    private List<Map<String, Object>> getServerStatus() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String status = String.format("""
            📊 MCP Java Server Status

            🏃 Status: Running
            📅 Current Time: %s
            🔧 Server Name: %s
            📦 Version: %s
            🌐 Transport: %s

            🔧 Capabilities:
            ⚙️  Tools: %s
            📁 Resources: %s
            💬 Prompts: %s
            📝 Logging: %s

            💾 Memory Usage:
            📊 Total: %,d KB
            🔄 Used: %,d KB
            💰 Free: %,d KB
            """,
            now.format(formatter),
            serverConfig.getName(),
            serverConfig.getVersion(),
            serverConfig.getTransport().getType(),
            serverConfig.getCapabilities().isTools() ? "✅" : "❌",
            serverConfig.getCapabilities().isResources() ? "✅" : "❌",
            serverConfig.getCapabilities().isPrompts() ? "✅" : "❌",
            serverConfig.getCapabilities().isLogging() ? "✅" : "❌",
            Runtime.getRuntime().totalMemory() / 1024,
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024,
            Runtime.getRuntime().freeMemory() / 1024
        );

        Map<String, Object> content = Map.of(
            "uri", SCHEME + "status",
            "mimeType", "text/plain",
            "text", status
        );

        return List.of(content);
    }
}