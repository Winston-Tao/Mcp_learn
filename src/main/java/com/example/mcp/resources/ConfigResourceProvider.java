package com.example.mcp.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ConfigResourceProvider extends AbstractResourceProvider {

    @Autowired
    private Environment environment;

    @Override
    public String getUri() {
        return "config://server";
    }

    @Override
    public String getName() {
        return "Server Configuration";
    }

    @Override
    public String getDescription() {
        return "Current server configuration and environment properties";
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    protected Object doRead() throws Exception {
        validateResource();

        logger.info("Reading server configuration");

        Map<String, Object> mcpConfig = new TreeMap<>();
        mcpConfig.put("version", environment.getProperty("mcp.version", "2025-06-18"));
        mcpConfig.put("server.name", environment.getProperty("mcp.server.name", "Java MCP Server"));
        mcpConfig.put("server.version", environment.getProperty("mcp.server.version", "1.0.0"));
        mcpConfig.put("transport.http.enabled", environment.getProperty("mcp.transport.http.enabled", "true"));
        mcpConfig.put("transport.http.endpoint", environment.getProperty("mcp.transport.http.endpoint", "/api/mcp"));
        mcpConfig.put("capabilities.tools", environment.getProperty("mcp.capabilities.tools", "true"));
        mcpConfig.put("capabilities.resources", environment.getProperty("mcp.capabilities.resources", "true"));
        mcpConfig.put("capabilities.prompts", environment.getProperty("mcp.capabilities.prompts", "true"));

        Map<String, Object> serverConfig = new TreeMap<>();
        serverConfig.put("port", environment.getProperty("server.port", "8080"));
        serverConfig.put("context-path", environment.getProperty("server.servlet.context-path", "/mcp"));

        Map<String, Object> springConfig = new TreeMap<>();
        springConfig.put("application.name", environment.getProperty("spring.application.name", "mcp-server"));
        springConfig.put("profiles.active", environment.getProperty("spring.profiles.active", "default"));

        Map<String, Object> systemInfo = new TreeMap<>();
        systemInfo.put("java.version", System.getProperty("java.version"));
        systemInfo.put("java.vendor", System.getProperty("java.vendor"));
        systemInfo.put("os.name", System.getProperty("os.name"));
        systemInfo.put("os.version", System.getProperty("os.version"));
        systemInfo.put("user.dir", System.getProperty("user.dir"));

        return Map.of(
                "uri", getUri(),
                "name", getName(),
                "description", getDescription(),
                "mcp", mcpConfig,
                "server", serverConfig,
                "spring", springConfig,
                "system", systemInfo,
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    @Override
    public Object getMetadata() {
        return Map.of(
                "configSources", "application.yml, environment variables, system properties",
                "refreshable", false,
                "sensitive", "passwords and secrets are excluded"
        );
    }
}