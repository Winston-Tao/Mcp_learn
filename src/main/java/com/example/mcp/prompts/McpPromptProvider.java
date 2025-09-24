package com.example.mcp.prompts;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface McpPromptProvider {

    String getName();

    String getDescription();

    List<Map<String, Object>> getArguments();

    CompletableFuture<Object> getPrompt(JsonNode arguments);

    default boolean isEnabled() {
        return true;
    }

    default String getVersion() {
        return "1.0.0";
    }

    default Object getMetadata() {
        return null;
    }
}