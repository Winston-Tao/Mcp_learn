#!/bin/bash

# MCP Java Server Startup Script

set -e

# Configuration
JAR_NAME="mcp-java-server-1.0.0.jar"
JAVA_OPTS="-Xmx512m -Xms256m"
LOG_DIR="logs"
PID_FILE="mcp-server.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# Check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        log_info "Please install Java 17 or later"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "([0-9]+)' | grep -oP '([0-9]+)')
    if [ "$JAVA_VERSION" -lt 17 ]; then
        log_error "Java 17 or later is required. Found version: $JAVA_VERSION"
        exit 1
    fi

    log_info "Java version: $JAVA_VERSION"
}

# Build the project
build_project() {
    log_info "Building project..."

    if [ ! -f "pom.xml" ]; then
        log_error "pom.xml not found. Please run this script from the project root."
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed or not in PATH"
        exit 1
    fi

    mvn clean package -DskipTests

    if [ $? -ne 0 ]; then
        log_error "Build failed"
        exit 1
    fi

    log_info "Build completed successfully"
}

# Create necessary directories
setup_directories() {
    log_info "Setting up directories..."

    mkdir -p "$LOG_DIR"
    mkdir -p "$HOME/mcp-files/samples"

    log_info "Directories created"
}

# Check if server is already running
check_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            log_warn "Server is already running (PID: $PID)"
            log_info "Use './scripts/stop-server.sh' to stop the server first"
            exit 1
        else
            log_warn "Stale PID file found, removing..."
            rm -f "$PID_FILE"
        fi
    fi
}

# Start the server
start_server() {
    log_info "Starting MCP Java Server..."

    JAR_PATH="target/$JAR_NAME"

    if [ ! -f "$JAR_PATH" ]; then
        log_error "JAR file not found: $JAR_PATH"
        log_info "Please build the project first"
        exit 1
    fi

    # Start the server
    nohup java $JAVA_OPTS -jar "$JAR_PATH" > "$LOG_DIR/server.out" 2>&1 &
    SERVER_PID=$!

    # Save PID
    echo $SERVER_PID > "$PID_FILE"

    # Wait a moment and check if the server started successfully
    sleep 2

    if ps -p $SERVER_PID > /dev/null 2>&1; then
        log_info "Server started successfully (PID: $SERVER_PID)"
        log_info "Log files:"
        log_info "  - Application log: $LOG_DIR/mcp-server.log"
        log_info "  - Startup log: $LOG_DIR/server.out"
        log_info ""
        log_info "To stop the server, run: ./scripts/stop-server.sh"
        log_info "To view logs, run: tail -f $LOG_DIR/mcp-server.log"
    else
        log_error "Server failed to start"
        log_info "Check the log files for details"
        rm -f "$PID_FILE"
        exit 1
    fi
}

# Main execution
main() {
    log_info "MCP Java Server Startup Script"
    log_info "=============================="

    check_java
    check_running
    build_project
    setup_directories
    start_server
}

# Handle script arguments
case "${1:-start}" in
    start)
        main
        ;;
    build)
        build_project
        ;;
    help|--help|-h)
        echo "Usage: $0 [start|build|help]"
        echo ""
        echo "Commands:"
        echo "  start (default) - Build and start the server"
        echo "  build          - Build the project only"
        echo "  help           - Show this help message"
        ;;
    *)
        log_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac