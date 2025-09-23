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

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(System.out)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        processMessage(line, writer);
                    } catch (Exception e) {
                        logger.error("Error processing message: {}", line, e);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error in STDIO transport", e);
        }

        logger.info("STDIO transport provider stopped");
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