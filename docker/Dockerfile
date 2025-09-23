# Multi-stage Docker build for MCP Java Server

# Build stage
FROM maven:3.9-openjdk-17 AS builder

WORKDIR /app

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

# Set metadata
LABEL maintainer="MCP Learning Project"
LABEL description="MCP Java Server Implementation"
LABEL version="1.0.0"

# Create application user
RUN useradd -r -u 1000 -m -c "MCP Server" -d /app -s /bin/bash mcpuser

# Set working directory
WORKDIR /app

# Create necessary directories
RUN mkdir -p logs mcp-files/samples \
    && chown -R mcpuser:mcpuser /app

# Copy the JAR file from builder stage
COPY --from=builder /app/target/mcp-java-server-*.jar app.jar

# Copy configuration files
COPY src/main/resources/application.yml ./config/
COPY src/main/resources/mcp-config.json ./config/

# Create sample files
RUN echo "This is a sample text file for MCP Server demonstration." > mcp-files/samples/sample.txt \
    && echo '{"message": "Hello from MCP Server!", "timestamp": "2024-01-01T00:00:00Z"}' > mcp-files/samples/sample.json \
    && chown -R mcpuser:mcpuser /app

# Switch to non-root user
USER mcpuser

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Expose port (if needed for HTTP transport)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD pgrep -f "mcp-java-server" > /dev/null || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Default command (can be overridden)
CMD []