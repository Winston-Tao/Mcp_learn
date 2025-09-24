package com.example.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.CompletableFuture;

public interface McpTool {

    String getName();

    String getDescription();

    JsonNode getInputSchema();

    CompletableFuture<Object> execute(JsonNode parameters);

    default boolean isEnabled() {
        return true;
    }

    default String getVersion() {
        return "1.0.0";
    }
}