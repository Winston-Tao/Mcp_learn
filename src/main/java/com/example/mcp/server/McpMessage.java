package com.example.mcp.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private JsonNode params;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private McpError error;

    public McpMessage() {}

    public McpMessage(Object id, String method, JsonNode params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public McpMessage(Object id, Object result) {
        this.id = id;
        this.result = result;
    }

    public McpMessage(Object id, McpError error) {
        this.id = id;
        this.error = error;
    }

    public static McpMessage createRequest(Object id, String method, JsonNode params) {
        return new McpMessage(id, method, params);
    }

    public static McpMessage createNotification(String method, JsonNode params) {
        return new McpMessage(null, method, params);
    }

    public static McpMessage createResponse(Object id, Object result) {
        return new McpMessage(id, result);
    }

    public static McpMessage createErrorResponse(Object id, McpError error) {
        return new McpMessage(id, error);
    }

    public boolean isRequest() {
        return method != null && id != null;
    }

    public boolean isNotification() {
        return method != null && id == null;
    }

    public boolean isResponse() {
        return method == null && (result != null || error != null);
    }

    public boolean isSuccessResponse() {
        return isResponse() && error == null;
    }

    public boolean isErrorResponse() {
        return isResponse() && error != null;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public McpError getError() {
        return error;
    }

    public void setError(McpError error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpMessage that)) return false;
        return Objects.equals(jsonrpc, that.jsonrpc) &&
               Objects.equals(id, that.id) &&
               Objects.equals(method, that.method) &&
               Objects.equals(params, that.params) &&
               Objects.equals(result, that.result) &&
               Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonrpc, id, method, params, result, error);
    }

    @Override
    public String toString() {
        return "McpMessage{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", id=" + id +
                ", method='" + method + '\'' +
                ", params=" + params +
                ", result=" + result +
                ", error=" + error +
                '}';
    }
}