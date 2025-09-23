package com.example.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mcp.server")
public class ServerConfig {

    private String name = "mcp-java-server";
    private String version = "1.0.0";
    private String description = "MCP Java Server Implementation";

    private Transport transport = new Transport();
    private Capabilities capabilities = new Capabilities();

    public static class Transport {
        private Type type = Type.STDIO;
        private int port = 8080;
        private String host = "localhost";

        public enum Type {
            STDIO, HTTP, WEBSOCKET
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }
    }

    public static class Capabilities {
        private boolean tools = true;
        private boolean resources = true;
        private boolean prompts = true;
        private boolean logging = true;
        private boolean resourceSubscriptions = false;

        public boolean isTools() {
            return tools;
        }

        public void setTools(boolean tools) {
            this.tools = tools;
        }

        public boolean isResources() {
            return resources;
        }

        public void setResources(boolean resources) {
            this.resources = resources;
        }

        public boolean isPrompts() {
            return prompts;
        }

        public void setPrompts(boolean prompts) {
            this.prompts = prompts;
        }

        public boolean isLogging() {
            return logging;
        }

        public void setLogging(boolean logging) {
            this.logging = logging;
        }

        public boolean isResourceSubscriptions() {
            return resourceSubscriptions;
        }

        public void setResourceSubscriptions(boolean resourceSubscriptions) {
            this.resourceSubscriptions = resourceSubscriptions;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }
}