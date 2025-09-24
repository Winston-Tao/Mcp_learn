package com.example.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class McpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpServerApplication.class);

    public static void main(String[] args) {
        logger.info("Starting MCP Server Application...");

        try {
            SpringApplication.run(McpServerApplication.class, args);
            logger.info("MCP Server started successfully");
        } catch (Exception e) {
            logger.error("Failed to start MCP Server: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}