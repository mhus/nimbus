#!/bin/bash

set -e

# Default values
IMAGE_NAME="nimbus-editor"
IMAGE_TAG="latest"
CONTAINER_NAME="nimbus-editor"
HOST_PORT="3001"
CONTAINER_PORT="3001"
DETACH=""
EXTRA_ARGS=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        --name)
            CONTAINER_NAME="$2"
            shift 2
            ;;
        --port)
            HOST_PORT="$2"
            shift 2
            ;;
        -d|--detach)
            DETACH="-d"
            shift
            ;;
        --network)
            EXTRA_ARGS="${EXTRA_ARGS} --network $2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --tag TAG           Image tag to run (default: latest)"
            echo "  --name NAME         Container name (default: nimbus-editor)"
            echo "  --port PORT         Host port to bind (default: 3001)"
            echo "  -d, --detach        Run container in background"
            echo "  --network NETWORK   Connect to network"
            echo "  --help              Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Run interactively on port 3001"
            echo "  $0 -d                 # Run in background"
            echo "  $0 --port 8001        # Run on port 8001"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Stop and remove existing container if it exists
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Stopping and removing existing container: ${CONTAINER_NAME}"
    docker stop "${CONTAINER_NAME}" >/dev/null 2>&1 || true
    docker rm "${CONTAINER_NAME}" >/dev/null 2>&1 || true
fi

echo "Starting Docker container..."
echo "  Image: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "  Container: ${CONTAINER_NAME}"
echo "  Port mapping: ${HOST_PORT}:${CONTAINER_PORT}"
if [ -n "$DETACH" ]; then
    echo "  Mode: Detached (background)"
else
    echo "  Mode: Interactive"
fi
echo ""

# Run Docker container
docker run \
    ${DETACH} \
    --name "${CONTAINER_NAME}" \
    -p "${HOST_PORT}:${CONTAINER_PORT}" \
    ${EXTRA_ARGS} \
    "${IMAGE_NAME}:${IMAGE_TAG}"

if [ -n "$DETACH" ]; then
    echo ""
    echo "Container started successfully!"
    echo ""
    echo "Useful commands:"
    echo "  docker logs ${CONTAINER_NAME}           # View logs"
    echo "  docker logs -f ${CONTAINER_NAME}        # Follow logs"
    echo "  docker stop ${CONTAINER_NAME}           # Stop container"
    echo "  docker restart ${CONTAINER_NAME}        # Restart container"
    echo ""
    echo "Application should be available at: http://localhost:${HOST_PORT}"
    echo "Health check: http://localhost:${HOST_PORT}/health"
fi
