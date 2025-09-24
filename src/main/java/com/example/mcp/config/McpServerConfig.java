package com.example.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "mcp")
public class McpServerConfig {

    private String version = "2025-06-18";
    private ServerInfo server = new ServerInfo();
    private Map<String, Boolean> capabilities = new HashMap<>();
    private TransportConfig transport = new TransportConfig();

    public McpServerConfig() {
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        capabilities.put("prompts", true);
        capabilities.put("sampling", false);
        capabilities.put("roots", false);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        this.server = server;
    }

    public Map<String, Boolean> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, Boolean> capabilities) {
        this.capabilities = capabilities;
    }

    public TransportConfig getTransport() {
        return transport;
    }

    public void setTransport(TransportConfig transport) {
        this.transport = transport;
    }

    public static class ServerInfo {
        private String name = "Java MCP Server";
        private String version = "1.0.0";

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
    }

    public static class TransportConfig {
        private HttpConfig http = new HttpConfig();

        public HttpConfig getHttp() {
            return http;
        }

        public void setHttp(HttpConfig http) {
            this.http = http;
        }

        public static class HttpConfig {
            private boolean enabled = true;
            private String endpoint = "/api/mcp";
            private SseConfig sse = new SseConfig();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }

            public SseConfig getSse() {
                return sse;
            }

            public void setSse(SseConfig sse) {
                this.sse = sse;
            }

            public static class SseConfig {
                private boolean enabled = true;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
            }
        }
    }
}