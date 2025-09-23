package com.example.mcp.config;

import com.example.mcp.server.McpServerImpl;
import com.example.mcp.tools.McpTool;
import com.example.mcp.resources.McpResourceProvider;
import com.example.mcp.prompts.McpPromptProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
public class McpServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpServerImpl mcpServer;
    private final List<McpTool> tools;
    private final List<McpResourceProvider> resourceProviders;
    private final List<McpPromptProvider> promptProviders;

    public McpServerConfig(McpServerImpl mcpServer,
                          List<McpTool> tools,
                          List<McpResourceProvider> resourceProviders,
                          List<McpPromptProvider> promptProviders) {
        this.mcpServer = mcpServer;
        this.tools = tools;
        this.resourceProviders = resourceProviders;
        this.promptProviders = promptProviders;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    @PostConstruct
    public void registerComponents() {
        logger.info("Registering MCP components...");

        // Register tools
        for (McpTool tool : tools) {
            mcpServer.registerTool(tool);
        }

        // Register resource providers
        for (int i = 0; i < resourceProviders.size(); i++) {
            McpResourceProvider provider = resourceProviders.get(i);
            String name = provider.getClass().getSimpleName().replace("ResourceProvider", "").toLowerCase();
            mcpServer.registerResourceProvider(name, provider);
        }

        // Register prompt providers
        for (int i = 0; i < promptProviders.size(); i++) {
            McpPromptProvider provider = promptProviders.get(i);
            String name = provider.getClass().getSimpleName().replace("PromptProvider", "").toLowerCase();
            mcpServer.registerPromptProvider(name, provider);
        }

        logger.info("Registered {} tools, {} resource providers, {} prompt providers",
            tools.size(), resourceProviders.size(), promptProviders.size());
    }
}