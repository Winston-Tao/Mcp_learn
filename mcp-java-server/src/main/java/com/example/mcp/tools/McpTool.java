package com.example.mcp.tools;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface McpTool {

    String getName();

    String getDescription();

    Map<String, Object> getInputSchema();

    Mono<List<Map<String, Object>>> execute(Map<String, Object> arguments);
}