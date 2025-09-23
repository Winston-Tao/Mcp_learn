package com.example.mcp.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractResourceProvider implements McpResourceProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Map<String, Object> createResource(String uri, String name, String description, String mimeType) {
        return Map.of(
            "uri", uri,
            "name", name,
            "description", description,
            "mimeType", mimeType
        );
    }

    protected Map<String, Object> createTextContent(String text) {
        return Map.of(
            "uri", "",
            "mimeType", "text/plain",
            "text", text
        );
    }

    protected Map<String, Object> createJsonContent(Object data) {
        return Map.of(
            "uri", "",
            "mimeType", "application/json",
            "text", data.toString()
        );
    }
}