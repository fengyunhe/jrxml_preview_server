#!/bin/bash
set -e

IMAGE_NAME="jrxml_preview_server"
IMAGE_TAG="latest"
CONTAINER_PORT=8084
HOST_PORT=8084

echo "🔨 Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .

echo ""
echo "✅ Build complete!"
echo ""
echo "📦 To run the container:"
echo "   docker run -d -p ${HOST_PORT}:${CONTAINER_PORT} --name ${IMAGE_NAME} ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
echo "📋 Or use docker-compose:"
echo "   docker-compose up -d"
