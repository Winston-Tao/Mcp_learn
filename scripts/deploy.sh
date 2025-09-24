#!/bin/bash

# MCP Server Deploy Script
# This script builds and prepares the MCP Server for deployment

set -e

echo "=== MCP Server Deployment Script ==="

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Check if Java 21 is available
if ! java -version 2>&1 | grep -q "21\|22\|23"; then
    echo "Error: Java 21 or higher is required"
    exit 1
fi

echo "Building MCP Server..."

# Clean and build the project
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Build successful!"

# Create deployment directory structure
echo "Creating deployment structure..."
mkdir -p deploy/bin
mkdir -p deploy/conf
mkdir -p deploy/logs
mkdir -p deploy/data

# Copy JAR file
cp target/mcp-server-*.jar deploy/bin/

# Copy configuration files
cp src/main/resources/application.yml deploy/conf/
cp src/main/resources/mcp-config.json deploy/conf/

# Copy scripts
cp scripts/*.sh deploy/bin/
cp scripts/*.bat deploy/bin/

# Make scripts executable
chmod +x deploy/bin/*.sh

# Create systemd service file
cat > deploy/bin/mcp-server.service << EOF
[Unit]
Description=MCP Server
After=network.target

[Service]
Type=simple
User=mcp
Group=mcp
WorkingDirectory=/opt/mcp-server
ExecStart=/opt/mcp-server/bin/start-server.sh
ExecStop=/opt/mcp-server/bin/stop-server.sh
Restart=always
RestartSec=5
Environment=JAVA_HOME=/usr/lib/jvm/java-21-openjdk
Environment=SERVER_PORT=8080

[Install]
WantedBy=multi-user.target
EOF

# Create Docker files
cat > deploy/Dockerfile << EOF
FROM eclipse-temurin:21-jre

LABEL maintainer="MCP Server Team"
LABEL description="Model Context Protocol Server"

# Create app directory
WORKDIR /app

# Create non-root user
RUN groupadd -r mcpserver && useradd -r -g mcpserver mcpserver

# Copy application files
COPY bin/mcp-server-*.jar app.jar
COPY conf/ conf/

# Create directories
RUN mkdir -p logs data && chown -R mcpserver:mcpserver /app

# Switch to non-root user
USER mcpserver

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/mcp/health || exit 1

# Expose port
EXPOSE 8080

# Start application
CMD ["java", "-Xms512m", "-Xmx2g", "-XX:+UseG1GC", "-XX:+EnablePreview", "-jar", "app.jar"]
EOF

cat > deploy/docker-compose.yml << EOF
version: '3.8'

services:
  mcp-server:
    build: .
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    environment:
      - SERVER_PORT=8080
      - SPRING_PROFILES_ACTIVE=prod
      - MCP_LOG_LEVEL=INFO
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/mcp/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
EOF

echo "Deployment package created in 'deploy' directory"
echo ""
echo "Deployment files:"
echo "- deploy/bin/mcp-server-*.jar       - Application JAR"
echo "- deploy/bin/start-server.sh        - Start script (Linux)"
echo "- deploy/bin/start-server.bat       - Start script (Windows)"
echo "- deploy/bin/stop-server.sh         - Stop script"
echo "- deploy/bin/mcp-server.service     - Systemd service file"
echo "- deploy/conf/application.yml       - Configuration file"
echo "- deploy/conf/mcp-config.json       - MCP configuration"
echo "- deploy/Dockerfile                 - Docker image"
echo "- deploy/docker-compose.yml         - Docker Compose"
echo ""
echo "To deploy:"
echo "1. Copy 'deploy' directory to target server"
echo "2. Run './bin/start-server.sh' or use Docker"
echo "3. Access server at http://localhost:8080/mcp"