package com.example.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractMcpTool implements McpTool {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CompletableFuture<Object> execute(JsonNode parameters) {
        logger.debug("Executing tool: {} with parameters: {}", getName(), parameters);

        return CompletableFuture.supplyAsync(() -> {
            try {
                validateParameters(parameters);
                Object result = doExecute(parameters);
                logger.debug("Tool {} executed successfully", getName());
                return result;
            } catch (Exception e) {
                logger.error("Error executing tool {}: {}", getName(), e.getMessage(), e);
                throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
            }
        });
    }

    protected abstract Object doExecute(JsonNode parameters) throws Exception;

    protected void validateParameters(JsonNode parameters) throws Exception {
        if (parameters == null || parameters.isNull()) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
    }

    protected String getStringParameter(JsonNode parameters, String key) {
        return getStringParameter(parameters, key, null);
    }

    protected String getStringParameter(JsonNode parameters, String key, String defaultValue) {
        if (parameters.has(key) && !parameters.get(key).isNull()) {
            return parameters.get(key).asText();
        }
        return defaultValue;
    }

    protected int getIntParameter(JsonNode parameters, String key) {
        return getIntParameter(parameters, key, 0);
    }

    protected int getIntParameter(JsonNode parameters, String key, int defaultValue) {
        if (parameters.has(key) && !parameters.get(key).isNull()) {
            return parameters.get(key).asInt();
        }
        return defaultValue;
    }

    protected double getDoubleParameter(JsonNode parameters, String key) {
        return getDoubleParameter(parameters, key, 0.0);
    }

    protected double getDoubleParameter(JsonNode parameters, String key, double defaultValue) {
        if (parameters.has(key) && !parameters.get(key).isNull()) {
            return parameters.get(key).asDouble();
        }
        return defaultValue;
    }

    protected boolean getBooleanParameter(JsonNode parameters, String key) {
        return getBooleanParameter(parameters, key, false);
    }

    protected boolean getBooleanParameter(JsonNode parameters, String key, boolean defaultValue) {
        if (parameters.has(key) && !parameters.get(key).isNull()) {
            return parameters.get(key).asBoolean();
        }
        return defaultValue;
    }

    protected void requireParameter(JsonNode parameters, String key) throws IllegalArgumentException {
        if (!parameters.has(key) || parameters.get(key).isNull()) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
    }
}