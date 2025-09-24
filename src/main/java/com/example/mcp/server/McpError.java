package com.example.mcp.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError {

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    public static final int RESOURCE_NOT_FOUND = -32000;
    public static final int TOOL_ERROR = -32001;
    public static final int PROMPT_ERROR = -32002;
    public static final int CAPABILITY_NOT_SUPPORTED = -32003;

    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;

    public McpError() {}

    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static McpError parseError() {
        return new McpError(PARSE_ERROR, "Parse error");
    }

    public static McpError parseError(String detail) {
        return new McpError(PARSE_ERROR, "Parse error", detail);
    }

    public static McpError invalidRequest() {
        return new McpError(INVALID_REQUEST, "Invalid Request");
    }

    public static McpError invalidRequest(String detail) {
        return new McpError(INVALID_REQUEST, "Invalid Request", detail);
    }

    public static McpError methodNotFound() {
        return new McpError(METHOD_NOT_FOUND, "Method not found");
    }

    public static McpError methodNotFound(String method) {
        return new McpError(METHOD_NOT_FOUND, "Method not found",
                "Method '" + method + "' is not supported");
    }

    public static McpError invalidParams() {
        return new McpError(INVALID_PARAMS, "Invalid params");
    }

    public static McpError invalidParams(String detail) {
        return new McpError(INVALID_PARAMS, "Invalid params", detail);
    }

    public static McpError internalError() {
        return new McpError(INTERNAL_ERROR, "Internal error");
    }

    public static McpError internalError(String detail) {
        return new McpError(INTERNAL_ERROR, "Internal error", detail);
    }

    public static McpError resourceNotFound(String resource) {
        return new McpError(RESOURCE_NOT_FOUND, "Resource not found",
                "Resource '" + resource + "' not found");
    }

    public static McpError toolError(String tool, String detail) {
        return new McpError(TOOL_ERROR, "Tool execution error",
                "Tool '" + tool + "' failed: " + detail);
    }

    public static McpError promptError(String prompt, String detail) {
        return new McpError(PROMPT_ERROR, "Prompt error",
                "Prompt '" + prompt + "' failed: " + detail);
    }

    public static McpError capabilityNotSupported(String capability) {
        return new McpError(CAPABILITY_NOT_SUPPORTED, "Capability not supported",
                "Capability '" + capability + "' is not supported by this server");
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpError mcpError)) return false;
        return code == mcpError.code &&
                Objects.equals(message, mcpError.message) &&
                Objects.equals(data, mcpError.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, data);
    }

    @Override
    public String toString() {
        return "McpError{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}