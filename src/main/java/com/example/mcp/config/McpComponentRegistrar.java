package com.example.mcp.config;

import com.example.mcp.server.McpServerImpl;
import com.example.mcp.tools.McpTool;
import com.example.mcp.resources.McpResourceProvider;
import com.example.mcp.prompts.McpPromptProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class McpComponentRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(McpComponentRegistrar.class);

    @Autowired
    private McpServerImpl mcpServer;

    @Autowired
    private List<McpTool> tools;

    @Autowired
    private List<McpResourceProvider> resourceProviders;

    @Autowired
    private List<McpPromptProvider> promptProviders;

    @EventListener(ApplicationReadyEvent.class)
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