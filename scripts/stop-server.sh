#!/bin/bash

# MCP Java Server Stop Script

set -e

# Configuration
PID_FILE="mcp-server.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Stop the server
stop_server() {
    log_info "MCP Java Server Stop Script"
    log_info "============================"

    if [ ! -f "$PID_FILE" ]; then
        log_warn "PID file not found. Server might not be running."
        return 0
    fi

    PID=$(cat "$PID_FILE")

    if ! ps -p $PID > /dev/null 2>&1; then
        log_warn "Process $PID is not running. Removing stale PID file."
        rm -f "$PID_FILE"
        return 0
    fi

    log_info "Stopping server (PID: $PID)..."

    # Try graceful shutdown first
    kill -TERM $PID

    # Wait for the process to terminate
    local count=0
    while ps -p $PID > /dev/null 2>&1; do
        sleep 1
        count=$((count + 1))

        if [ $count -eq 10 ]; then
            log_warn "Graceful shutdown taking too long, forcing termination..."
            kill -KILL $PID
            break
        fi
    done

    # Check if process is still running
    if ps -p $PID > /dev/null 2>&1; then
        log_error "Failed to stop server process $PID"
        exit 1
    else
        log_info "Server stopped successfully"
        rm -f "$PID_FILE"
    fi
}

# Show server status
show_status() {
    log_info "MCP Java Server Status"
    log_info "======================"

    if [ ! -f "$PID_FILE" ]; then
        log_info "Status: STOPPED (no PID file)"
        return 0
    fi

    PID=$(cat "$PID_FILE")

    if ps -p $PID > /dev/null 2>&1; then
        log_info "Status: RUNNING (PID: $PID)"

        # Show process info
        ps -p $PID -o pid,ppid,cmd,etime,pmem,pcpu

        # Show log file info if it exists
        if [ -f "logs/mcp-server.log" ]; then
            log_info ""
            log_info "Log file: logs/mcp-server.log"
            log_info "Last 5 lines:"
            tail -n 5 "logs/mcp-server.log"
        fi
    else
        log_warn "Status: STOPPED (stale PID file)"
        log_info "Removing stale PID file..."
        rm -f "$PID_FILE"
    fi
}

# Handle script arguments
case "${1:-stop}" in
    stop)
        stop_server
        ;;
    status)
        show_status
        ;;
    restart)
        log_info "Restarting MCP Java Server..."
        stop_server
        sleep 2
        ./scripts/start-server.sh
        ;;
    help|--help|-h)
        echo "Usage: $0 [stop|status|restart|help]"
        echo ""
        echo "Commands:"
        echo "  stop (default) - Stop the server"
        echo "  status         - Show server status"
        echo "  restart        - Restart the server"
        echo "  help           - Show this help message"
        ;;
    *)
        log_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac