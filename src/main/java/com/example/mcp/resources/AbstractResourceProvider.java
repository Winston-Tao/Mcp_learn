package com.example.mcp.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractResourceProvider implements McpResourceProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CompletableFuture<Object> read() {
        logger.debug("Reading resource: {}", getUri());

        return CompletableFuture.supplyAsync(() -> {
            try {
                Object data = doRead();
                logger.debug("Resource {} read successfully", getUri());
                return data;
            } catch (Exception e) {
                logger.error("Error reading resource {}: {}", getUri(), e.getMessage(), e);
                throw new RuntimeException("Resource read failed: " + e.getMessage(), e);
            }
        });
    }

    protected abstract Object doRead() throws Exception;

    protected void validateResource() throws Exception {
        if (getUri() == null || getUri().trim().isEmpty()) {
            throw new IllegalStateException("Resource URI cannot be null or empty");
        }
        if (getName() == null || getName().trim().isEmpty()) {
            throw new IllegalStateException("Resource name cannot be null or empty");
        }
    }
}