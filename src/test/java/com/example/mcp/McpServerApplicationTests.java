package com.example.mcp;

import com.example.mcp.server.McpError;
import com.example.mcp.server.McpMessage;
import com.example.mcp.server.McpServerImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpServerApplicationTests {

    private McpServerImpl mcpServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mcpServer = new McpServerImpl(objectMapper);
    }

    @Test
    void testInitializeRequest() throws Exception {
        JsonNode params = objectMapper.createObjectNode();
        McpMessage request = McpMessage.createRequest("test-1", "initialize", params);

        CompletableFuture<McpMessage> responseFuture = mcpServer.processMessage(request);
        McpMessage response = responseFuture.get();

        assertNotNull(response);
        assertEquals("test-1", response.getId());
        assertTrue(response.isSuccessResponse());
        assertNotNull(response.getResult());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertTrue(result.containsKey("protocolVersion"));
        assertTrue(result.containsKey("capabilities"));
        assertTrue(result.containsKey("serverInfo"));
    }

    @Test
    void testPingRequest() throws Exception {
        JsonNode params = objectMapper.createObjectNode();
        McpMessage request = McpMessage.createRequest("test-2", "ping", params);

        CompletableFuture<McpMessage> responseFuture = mcpServer.processMessage(request);
        McpMessage response = responseFuture.get();

        assertNotNull(response);
        assertEquals("test-2", response.getId());
        assertTrue(response.isSuccessResponse());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals("pong", result.get("status"));
    }

    @Test
    void testMethodNotFound() throws Exception {
        JsonNode params = objectMapper.createObjectNode();
        McpMessage request = McpMessage.createRequest("test-3", "nonexistent", params);

        CompletableFuture<McpMessage> responseFuture = mcpServer.processMessage(request);
        McpMessage response = responseFuture.get();

        assertNotNull(response);
        assertEquals("test-3", response.getId());
        assertTrue(response.isErrorResponse());
        assertEquals(McpError.METHOD_NOT_FOUND, response.getError().getCode());
    }

    @Test
    void testInvalidJsonRpcVersion() throws Exception {
        JsonNode params = objectMapper.createObjectNode();
        McpMessage request = McpMessage.createRequest("test-4", "ping", params);
        request.setJsonrpc("1.0");

        CompletableFuture<McpMessage> responseFuture = mcpServer.processMessage(request);
        McpMessage response = responseFuture.get();

        assertNotNull(response);
        assertEquals("test-4", response.getId());
        assertTrue(response.isErrorResponse());
        assertEquals(McpError.INVALID_REQUEST, response.getError().getCode());
    }

    @Test
    void testNotification() throws Exception {
        JsonNode params = objectMapper.createObjectNode();
        McpMessage notification = McpMessage.createNotification("ping", params);

        CompletableFuture<McpMessage> responseFuture = mcpServer.processMessage(notification);
        McpMessage response = responseFuture.get();

        assertNull(response);
    }

    @Test
    void testMcpErrorCreation() {
        McpError parseError = McpError.parseError();
        assertEquals(McpError.PARSE_ERROR, parseError.getCode());
        assertEquals("Parse error", parseError.getMessage());

        McpError methodNotFound = McpError.methodNotFound("test_method");
        assertEquals(McpError.METHOD_NOT_FOUND, methodNotFound.getCode());
        assertTrue(methodNotFound.getData().toString().contains("test_method"));
    }

    @Test
    void testMcpMessageTypes() {
        JsonNode params = objectMapper.createObjectNode();

        McpMessage request = McpMessage.createRequest("1", "test", params);
        assertTrue(request.isRequest());
        assertFalse(request.isNotification());
        assertFalse(request.isResponse());

        McpMessage notification = McpMessage.createNotification("test", params);
        assertFalse(notification.isRequest());
        assertTrue(notification.isNotification());
        assertFalse(notification.isResponse());

        McpMessage response = McpMessage.createResponse("1", "result");
        assertFalse(response.isRequest());
        assertFalse(response.isNotification());
        assertTrue(response.isResponse());
        assertTrue(response.isSuccessResponse());

        McpMessage errorResponse = McpMessage.createErrorResponse("1", McpError.internalError());
        assertFalse(errorResponse.isRequest());
        assertFalse(errorResponse.isNotification());
        assertTrue(errorResponse.isResponse());
        assertTrue(errorResponse.isErrorResponse());
    }
}