package com.example.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfig {

    private String name = "Java MCP Server";
    private String version = "1.0.0";
    private String description = "MCP Server implementation in Java using Spring Boot";
    private Map<String, Object> metadata = new HashMap<>();

    public ServerConfig() {
        metadata.put("language", "Java");
        metadata.put("framework", "Spring Boot");
        metadata.put("transport", "HTTP/SSE");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}