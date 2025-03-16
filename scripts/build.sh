#!/bin/bash

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if tag is provided
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: Please provide a docker image tag${NC}"
    echo -e "${YELLOW}Usage: $0 <tag>${NC}"
    exit 1
fi

TAG=$1
IMAGE_NAME="cbartram/kraken-api:$TAG"

# Build Docker image
echo -e "${YELLOW}Building Docker image: ${IMAGE_NAME}${NC}"
docker build . -t "$IMAGE_NAME"

# Check if build was successful
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Docker build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker image built successfully${NC}"

# Push Docker image
echo -e "${YELLOW}Pushing Docker image: ${IMAGE_NAME}${NC}"
docker push "$IMAGE_NAME"

# Check if push was successful
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Docker push failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker image pushed successfully${NC}"
echo -e "${GREEN}Image pushed: ${IMAGE_NAME}${NC}"