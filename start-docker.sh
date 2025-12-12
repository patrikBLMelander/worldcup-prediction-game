#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting World Cup Application with Docker ===${NC}\n"

# Check if docker compose or docker-compose
if command -v docker &> /dev/null; then
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker compose"
    elif docker-compose version &> /dev/null; then
        DOCKER_COMPOSE="docker-compose"
    else
        echo -e "${YELLOW}Error: Docker Compose not found${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}Error: Docker not found${NC}"
    exit 1
fi

echo -e "${YELLOW}Building and starting containers...${NC}\n"

# Build and start all services
$DOCKER_COMPOSE up --build -d

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✓ Containers started successfully!${NC}\n"
    echo -e "${BLUE}Services:${NC}"
    echo -e "  Frontend:  ${GREEN}http://localhost:3000${NC}"
    echo -e "  Backend:   ${GREEN}http://localhost:8080${NC}"
    echo -e "  PostgreSQL: ${GREEN}localhost:5432${NC}\n"
    
    echo -e "${YELLOW}Waiting for services to be ready...${NC}"
    sleep 5
    
    echo -e "\n${BLUE}Checking service health...${NC}"
    
    # Check backend
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo -e "  ${GREEN}✓ Backend is ready${NC}"
    else
        echo -e "  ${YELLOW}⚠ Backend is starting... (may take 30-60 seconds)${NC}"
    fi
    
    # Check frontend
    if curl -s http://localhost:3000 > /dev/null; then
        echo -e "  ${GREEN}✓ Frontend is ready${NC}"
    else
        echo -e "  ${YELLOW}⚠ Frontend is starting...${NC}"
    fi
    
    echo -e "\n${BLUE}Useful commands:${NC}"
    echo -e "  View logs:     ${GREEN}$DOCKER_COMPOSE logs -f${NC}"
    echo -e "  Stop:          ${GREEN}$DOCKER_COMPOSE down${NC}"
    echo -e "  Stop & remove: ${GREEN}$DOCKER_COMPOSE down -v${NC}"
    echo -e "  Status:        ${GREEN}$DOCKER_COMPOSE ps${NC}\n"
else
    echo -e "\n${YELLOW}Failed to start containers. Check the logs with:${NC}"
    echo -e "  $DOCKER_COMPOSE logs\n"
    exit 1
fi


