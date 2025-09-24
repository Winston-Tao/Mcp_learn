package com.example.mcp.config;

import com.example.mcp.server.McpServerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpComponentRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(McpComponentRegistrar.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public McpServerImpl mcpServer() {
        logger.info("Creating MCP Server instance");
        return new McpServerImpl(objectMapper);
    }
}