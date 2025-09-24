#!/bin/bash

# MCP Server Start Script
# This script starts the MCP Server with appropriate Java settings

# Check if Java 21 is available
if ! java -version 2>&1 | grep -q "21\|22\|23"; then
    echo "Error: Java 21 or higher is required"
    exit 1
fi

# Set JVM options for optimal performance
export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+EnablePreview"

# Set application properties
export SPRING_PROFILES_ACTIVE="default"
export SERVER_PORT="${SERVER_PORT:-8080}"
export MCP_LOG_LEVEL="${MCP_LOG_LEVEL:-INFO}"

# Create logs directory
mkdir -p logs

echo "Starting MCP Server..."
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Server Port: $SERVER_PORT"
echo "Log Level: $MCP_LOG_LEVEL"
echo "Profile: $SPRING_PROFILES_ACTIVE"

# Start the application
java $JAVA_OPTS -jar target/mcp-server-1.0.0-SNAPSHOT.jar \
    --server.port=$SERVER_PORT \
    --logging.level.com.example.mcp=$MCP_LOG_LEVEL \
    --spring.profiles.active=$SPRING_PROFILES_ACTIVE \
    "$@"