package com.example.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public abstract class AbstractMcpTool implements McpTool {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String name;
    protected final String description;
    protected final Map<String, Object> inputSchema;

    public AbstractMcpTool(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    @Override
    public Mono<List<Map<String, Object>>> execute(Map<String, Object> arguments) {
        logger.debug("Executing tool '{}' with arguments: {}", name, arguments);

        return validateArguments(arguments)
            .then(doExecute(arguments))
            .doOnSuccess(result -> logger.debug("Tool '{}' execution completed successfully", name))
            .doOnError(error -> logger.error("Tool '{}' execution failed", name, error));
    }

    protected Mono<Void> validateArguments(Map<String, Object> arguments) {
        return Mono.empty();
    }

    protected abstract Mono<List<Map<String, Object>>> doExecute(Map<String, Object> arguments);

    protected Map<String, Object> createTextContent(String text) {
        return Map.of("type", "text", "text", text);
    }

    protected Map<String, Object> createErrorContent(String error) {
        return Map.of("type", "text", "text", "Error: " + error);
    }
}