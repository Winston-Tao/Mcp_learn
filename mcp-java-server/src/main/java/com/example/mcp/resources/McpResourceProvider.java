package com.example.mcp.resources;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface McpResourceProvider {

    List<Map<String, Object>> listResources();

    boolean canHandle(String uri);

    Mono<List<Map<String, Object>>> readResource(String uri);
}