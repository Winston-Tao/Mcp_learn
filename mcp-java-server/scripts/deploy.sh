#!/bin/bash

# MCP Java Server Deployment Script

set -e

# Configuration
DOCKER_IMAGE="mcp-java-server"
DOCKER_TAG="latest"
COMPOSE_FILE="docker/docker-compose.yml"

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

# Check dependencies
check_dependencies() {
    log_info "Checking dependencies..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi

    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        exit 1
    fi

    log_info "Dependencies check passed"
}

# Build Docker image
build_image() {
    log_info "Building Docker image..."

    if [ ! -f "docker/Dockerfile" ]; then
        log_error "Dockerfile not found"
        exit 1
    fi

    docker build -f docker/Dockerfile -t "$DOCKER_IMAGE:$DOCKER_TAG" .

    if [ $? -eq 0 ]; then
        log_info "Docker image built successfully"
    else
        log_error "Failed to build Docker image"
        exit 1
    fi
}

# Deploy with Docker Compose
deploy_compose() {
    log_info "Deploying with Docker Compose..."

    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Docker Compose file not found: $COMPOSE_FILE"
        exit 1
    fi

    # Create necessary directories
    mkdir -p logs mcp-files/samples

    # Start services
    if command -v docker-compose &> /dev/null; then
        docker-compose -f "$COMPOSE_FILE" up -d
    else
        docker compose -f "$COMPOSE_FILE" up -d
    fi

    if [ $? -eq 0 ]; then
        log_info "Services deployed successfully"
    else
        log_error "Failed to deploy services"
        exit 1
    fi
}

# Deploy standalone container
deploy_standalone() {
    log_info "Deploying standalone container..."

    # Stop and remove existing container
    docker stop mcp-java-server 2>/dev/null || true
    docker rm mcp-java-server 2>/dev/null || true

    # Create necessary directories
    mkdir -p logs mcp-files/samples

    # Run container
    docker run -d \
        --name mcp-java-server \
        --restart unless-stopped \
        -v "$(pwd)/logs:/app/logs" \
        -v "$(pwd)/mcp-files:/app/mcp-files" \
        -e JAVA_OPTS="-Xmx512m -Xms256m" \
        "$DOCKER_IMAGE:$DOCKER_TAG"

    if [ $? -eq 0 ]; then
        log_info "Container deployed successfully"
    else
        log_error "Failed to deploy container"
        exit 1
    fi
}

# Show deployment status
show_status() {
    log_info "Deployment Status"
    log_info "=================="

    echo
    log_info "Docker Images:"
    docker images | grep mcp-java-server || log_warn "No MCP images found"

    echo
    log_info "Running Containers:"
    docker ps | grep mcp || log_warn "No MCP containers running"

    echo
    log_info "Container Logs (last 10 lines):"
    docker logs --tail 10 mcp-java-server 2>/dev/null || log_warn "No logs available"
}

# Clean up deployment
cleanup() {
    log_info "Cleaning up deployment..."

    # Stop and remove containers
    if command -v docker-compose &> /dev/null; then
        docker-compose -f "$COMPOSE_FILE" down 2>/dev/null || true
    else
        docker compose -f "$COMPOSE_FILE" down 2>/dev/null || true
    fi

    docker stop mcp-java-server 2>/dev/null || true
    docker rm mcp-java-server 2>/dev/null || true

    # Remove images
    docker rmi "$DOCKER_IMAGE:$DOCKER_TAG" 2>/dev/null || true

    log_info "Cleanup completed"
}

# Show help
show_help() {
    echo "MCP Java Server Deployment Script"
    echo "=================================="
    echo
    echo "Usage: $0 [COMMAND]"
    echo
    echo "Commands:"
    echo "  build              - Build Docker image only"
    echo "  deploy-compose     - Build and deploy with Docker Compose"
    echo "  deploy-standalone  - Build and deploy standalone container"
    echo "  status             - Show deployment status"
    echo "  cleanup            - Clean up all containers and images"
    echo "  help               - Show this help message"
    echo
    echo "Examples:"
    echo "  $0 deploy-compose     # Full deployment with Docker Compose"
    echo "  $0 deploy-standalone  # Simple standalone deployment"
    echo "  $0 status            # Check deployment status"
    echo "  $0 cleanup           # Remove all containers and images"
}

# Main execution
main() {
    local command="${1:-deploy-compose}"

    case "$command" in
        build)
            check_dependencies
            build_image
            ;;
        deploy-compose)
            check_dependencies
            build_image
            deploy_compose
            show_status
            ;;
        deploy-standalone)
            check_dependencies
            build_image
            deploy_standalone
            show_status
            ;;
        status)
            show_status
            ;;
        cleanup)
            cleanup
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"