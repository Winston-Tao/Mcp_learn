#!/bin/bash

# Stop MCP Server Script
# This script stops the running MCP Server

PID_FILE="mcp-server.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")

    if ps -p $PID > /dev/null 2>&1; then
        echo "Stopping MCP Server (PID: $PID)..."
        kill $PID

        # Wait for graceful shutdown
        for i in {1..30}; do
            if ! ps -p $PID > /dev/null 2>&1; then
                echo "MCP Server stopped successfully"
                rm -f "$PID_FILE"
                exit 0
            fi
            sleep 1
        done

        # Force kill if still running
        echo "Force stopping MCP Server..."
        kill -9 $PID
        rm -f "$PID_FILE"
        echo "MCP Server force stopped"
    else
        echo "MCP Server is not running (stale PID file)"
        rm -f "$PID_FILE"
    fi
else
    echo "PID file not found. Checking for running processes..."

    # Try to find Java processes with mcp-server
    PIDS=$(ps aux | grep "[j]ava.*mcp-server" | awk '{print $2}')

    if [ -n "$PIDS" ]; then
        echo "Found MCP Server processes: $PIDS"
        for PID in $PIDS; do
            echo "Stopping process $PID..."
            kill $PID
        done
        echo "All MCP Server processes stopped"
    else
        echo "No MCP Server processes found"
    fi
fi