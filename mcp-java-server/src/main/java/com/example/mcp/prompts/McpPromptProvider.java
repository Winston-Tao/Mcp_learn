package com.example.mcp.prompts;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface McpPromptProvider {

    List<Map<String, Object>> listPrompts();

    boolean hasPrompt(String name);

    Mono<Map<String, Object>> getPrompt(String name, Map<String, Object> arguments);
}