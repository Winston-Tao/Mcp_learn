package com.example.mcp.resources;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public interface McpResourceProvider {

    String getUri();

    String getName();

    String getDescription();

    String getMimeType();

    CompletableFuture<Object> read();

    default boolean isReadable() {
        return true;
    }

    default String getVersion() {
        return "1.0.0";
    }

    default Object getMetadata() {
        return null;
    }
}