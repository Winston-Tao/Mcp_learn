package com.example.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class McpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpServerApplication.class);

    public static void main(String[] args) {
        logger.info("Starting MCP Java Server...");

        ApplicationContext context = SpringApplication.run(McpServerApplication.class, args);

        logger.info("MCP Java Server started successfully");
        logger.info("Available beans: {}", context.getBeanDefinitionCount());
    }
}