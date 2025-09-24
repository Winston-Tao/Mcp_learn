package com.example.mcp.prompts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractPromptProvider implements McpPromptProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CompletableFuture<Object> getPrompt(JsonNode arguments) {
        logger.debug("Getting prompt: {} with arguments: {}", getName(), arguments);

        return CompletableFuture.supplyAsync(() -> {
            try {
                validateArguments(arguments);
                Object result = doGetPrompt(arguments);
                logger.debug("Prompt {} generated successfully", getName());
                return result;
            } catch (Exception e) {
                logger.error("Error generating prompt {}: {}", getName(), e.getMessage(), e);
                throw new RuntimeException("Prompt generation failed: " + e.getMessage(), e);
            }
        });
    }

    protected abstract Object doGetPrompt(JsonNode arguments) throws Exception;

    protected void validateArguments(JsonNode arguments) throws Exception {
        if (arguments == null) {
            arguments = objectMapper.createObjectNode();
        }
    }

    protected String getStringArgument(JsonNode arguments, String key) {
        return getStringArgument(arguments, key, null);
    }

    protected String getStringArgument(JsonNode arguments, String key, String defaultValue) {
        if (arguments != null && arguments.has(key) && !arguments.get(key).isNull()) {
            return arguments.get(key).asText();
        }
        return defaultValue;
    }

    protected int getIntArgument(JsonNode arguments, String key) {
        return getIntArgument(arguments, key, 0);
    }

    protected int getIntArgument(JsonNode arguments, String key, int defaultValue) {
        if (arguments != null && arguments.has(key) && !arguments.get(key).isNull()) {
            return arguments.get(key).asInt();
        }
        return defaultValue;
    }

    protected boolean getBooleanArgument(JsonNode arguments, String key) {
        return getBooleanArgument(arguments, key, false);
    }

    protected boolean getBooleanArgument(JsonNode arguments, String key, boolean defaultValue) {
        if (arguments != null && arguments.has(key) && !arguments.get(key).isNull()) {
            return arguments.get(key).asBoolean();
        }
        return defaultValue;
    }

    protected void requireArgument(JsonNode arguments, String key) throws IllegalArgumentException {
        if (arguments == null || !arguments.has(key) || arguments.get(key).isNull()) {
            throw new IllegalArgumentException("Required argument '" + key + "' is missing");
        }
    }
}