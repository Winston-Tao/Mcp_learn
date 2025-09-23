package com.example.mcp.transport;

import com.example.mcp.server.McpMessage;
import com.example.mcp.server.McpServerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

@Component
public class StdioTransportProvider implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StdioTransportProvider.class);

    private final McpServerImpl mcpServer;
    private final ObjectMapper objectMapper;

    @Autowired
    public StdioTransportProvider(McpServerImpl mcpServer, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting STDIO transport provider...");

        // Check if we're running in an interactive environment
        boolean isInteractive = System.console() != null || isStdinAvailable();

        if (!isInteractive) {
            logger.info("Running in non-interactive mode (background/daemon)");
            logger.info("MCP Server is ready and waiting for connections");

            // Keep the server alive for background execution
            keepServerAlive();
        } else {
            logger.info("Running in interactive mode, reading from STDIN");
            runInteractiveMode();
        }

        logger.info("STDIO transport provider stopped");
    }

    private boolean isStdinAvailable() {
        try {
            // Check if we have an actual console
            if (System.console() != null) {
                return true;
            }

            // Check if STDIN is redirected/closed
            // In background mode, System.in.available() typically returns 0
            // and attempting to read will immediately return null
            return System.in.available() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void runInteractiveMode() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(System.out)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                final String currentLine = line;
                CompletableFuture.runAsync(() -> {
                    try {
                        processMessage(currentLine, writer);
                    } catch (Exception e) {
                        logger.error("Error processing message: {}", currentLine, e);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error in STDIO transport", e);
        }
    }

    private void keepServerAlive() {
        try {
            logger.info("Server is running in background mode");
            logger.info("To connect to this server, use MCP client tools");
            logger.info("Server will continue running until explicitly stopped");

            // Create a shutdown hook to handle graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Received shutdown signal, stopping MCP server...");
            }));

            // Keep the server alive
            Object lock = new Object();
            synchronized (lock) {
                lock.wait(); // Wait indefinitely until interrupted
            }
        } catch (InterruptedException e) {
            logger.info("Server interrupted, shutting down gracefully");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error keeping server alive", e);
        }
    }

    private void processMessage(String line, PrintWriter writer) {
        try {
            logger.debug("Received: {}", line);

            McpMessage request = objectMapper.readValue(line, McpMessage.class);

            mcpServer.handleMessage(request)
                .subscribe(
                    response -> {
                        if (response != null) {
                            try {
                                String responseJson = objectMapper.writeValueAsString(response);
                                logger.debug("Sending: {}", responseJson);

                                synchronized (writer) {
                                    writer.println(responseJson);
                                    writer.flush();
                                }
                            } catch (Exception e) {
                                logger.error("Error serializing response", e);
                            }
                        }
                    },
                    error -> {
                        logger.error("Error handling message", error);

                        try {
                            McpMessage errorResponse = McpMessage.error(
                                request.getId(),
                                com.example.mcp.server.McpError.internalError(error.getMessage())
                            );

                            String errorJson = objectMapper.writeValueAsString(errorResponse);

                            synchronized (writer) {
                                writer.println(errorJson);
                                writer.flush();
                            }
                        } catch (Exception e) {
                            logger.error("Error sending error response", e);
                        }
                    }
                );

        } catch (Exception e) {
            logger.error("Error parsing message: {}", line, e);

            try {
                McpMessage errorResponse = McpMessage.error(
                    null,
                    com.example.mcp.server.McpError.parseError()
                );

                String errorJson = objectMapper.writeValueAsString(errorResponse);

                synchronized (writer) {
                    writer.println(errorJson);
                    writer.flush();
                }
            } catch (Exception ex) {
                logger.error("Error sending parse error response", ex);
            }
        }
    }
}