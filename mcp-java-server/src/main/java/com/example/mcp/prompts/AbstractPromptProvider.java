package com.example.mcp.prompts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class AbstractPromptProvider implements McpPromptProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Map<String, Object> createPromptInfo(String name, String description, List<Map<String, Object>> arguments) {
        return Map.of(
            "name", name,
            "description", description,
            "arguments", arguments
        );
    }

    protected Map<String, Object> createArgument(String name, String description, String type, boolean required) {
        return Map.of(
            "name", name,
            "description", description,
            "type", type,
            "required", required
        );
    }

    protected Map<String, Object> createTextMessage(String role, String content) {
        return Map.of(
            "role", role,
            "content", Map.of(
                "type", "text",
                "text", content
            )
        );
    }

    protected Map<String, Object> createPromptResponse(String description, List<Map<String, Object>> messages) {
        return Map.of(
            "description", description,
            "messages", messages
        );
    }
}