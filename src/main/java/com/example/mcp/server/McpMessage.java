package com.example.mcp.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Map<String, Object> params;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private McpError error;

    public McpMessage() {}

    public McpMessage(Object id, String method, Map<String, Object> params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public static McpMessage request(Object id, String method, Map<String, Object> params) {
        return new McpMessage(id, method, params);
    }

    public static McpMessage response(Object id, Object result) {
        McpMessage message = new McpMessage();
        message.id = id;
        message.result = result;
        return message;
    }

    public static McpMessage error(Object id, McpError error) {
        McpMessage message = new McpMessage();
        message.id = id;
        message.error = error;
        return message;
    }

    public static McpMessage notification(String method, Map<String, Object> params) {
        McpMessage message = new McpMessage();
        message.method = method;
        message.params = params;
        return message;
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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
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

    public boolean isRequest() {
        return method != null && id != null;
    }

    public boolean isResponse() {
        return id != null && (result != null || error != null);
    }

    public boolean isNotification() {
        return method != null && id == null;
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