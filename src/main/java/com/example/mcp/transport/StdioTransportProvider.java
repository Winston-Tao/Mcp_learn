package com.example.mcp.transport;

import com.example.mcp.server.McpError;
import com.example.mcp.server.McpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class StdioTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(StdioTransportProvider.class);

    private final ObjectMapper objectMapper;
    private final BlockingQueue<String> inputQueue;
    private final BlockingQueue<String> outputQueue;
    private volatile boolean running = false;

    @Autowired
    public StdioTransportProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueue = new LinkedBlockingQueue<>();

        logger.info("STDIO Transport Provider initialized");
    }

    public void start() {
        if (running) {
            logger.warn("STDIO transport is already running");
            return;
        }

        running = true;
        logger.info("Starting STDIO transport...");

        CompletableFuture.runAsync(this::handleInput);
        CompletableFuture.runAsync(this::handleOutput);

        logger.info("STDIO transport started");
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        logger.info("STDIO transport stopped");
    }

    public void sendMessage(McpMessage message) {
        if (!running) {
            logger.warn("Cannot send message - STDIO transport is not running");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            outputQueue.offer(json);
            logger.debug("Message queued for output: {}", message.getId());
        } catch (Exception e) {
            logger.error("Error serializing message: {}", e.getMessage(), e);
        }
    }

    public CompletableFuture<McpMessage> receiveMessage() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = inputQueue.take();
                JsonNode jsonNode = objectMapper.readTree(json);
                return objectMapper.treeToValue(jsonNode, McpMessage.class);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Message receive interrupted");
                return null;
            } catch (Exception e) {
                logger.error("Error deserializing message: {}", e.getMessage(), e);
                return McpMessage.createErrorResponse(null,
                    McpError.parseError("Failed to parse JSON-RPC message"));
            }
        });
    }

    private void handleInput() {
        logger.debug("Starting STDIO input handler");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    inputQueue.offer(line);
                    logger.debug("Message received from STDIN");
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error reading from STDIN: {}", e.getMessage(), e);
            }
        }

        logger.debug("STDIO input handler stopped");
    }

    private void handleOutput() {
        logger.debug("Starting STDIO output handler");

        try (PrintWriter writer = new PrintWriter(System.out, true)) {
            while (running) {
                try {
                    String message = outputQueue.take();
                    writer.println(message);
                    writer.flush();
                    logger.debug("Message sent to STDOUT");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            if (running) {
                logger.error("Error writing to STDOUT: {}", e.getMessage(), e);
            }
        }

        logger.debug("STDIO output handler stopped");
    }

    public boolean isRunning() {
        return running;
    }
}