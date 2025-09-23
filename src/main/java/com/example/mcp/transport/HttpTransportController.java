package com.example.mcp.transport;

import com.example.mcp.server.McpMessage;
import com.example.mcp.server.McpServerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/mcp/simple")
public class HttpTransportController {

    private static final Logger logger = LoggerFactory.getLogger(HttpTransportController.class);

    private final McpServerImpl mcpServer;
    private final ObjectMapper objectMapper;

    @Autowired
    public HttpTransportController(McpServerImpl mcpServer, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<McpMessage>> handleMcpRequest(@RequestBody McpMessage request) {
        logger.debug("Received simple HTTP MCP request: {}", request);

        return mcpServer.handleMessage(request)
            .map(response -> {
                logger.debug("Sending simple HTTP MCP response: {}", response);
                return ResponseEntity.ok(response);
            })
            .defaultIfEmpty(ResponseEntity.ok().build())
            .onErrorResume(throwable -> {
                logger.error("Error handling MCP request via simple HTTP", throwable);
                McpMessage errorResponse = McpMessage.error(
                    request.getId(),
                    com.example.mcp.server.McpError.internalError(throwable.getMessage())
                );
                return Mono.just(ResponseEntity.ok(errorResponse));
            });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
            "status", "OK",
            "transport", "HTTP",
            "timestamp", System.currentTimeMillis()
        );
        return Mono.just(ResponseEntity.ok(health));
    }

    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        Map<String, Object> info = Map.of(
            "name", "MCP Java Server",
            "transport", "HTTP",
            "version", "1.0.0",
            "description", "HTTP transport for Model Context Protocol"
        );
        return Mono.just(ResponseEntity.ok(info));
    }
}