#!/usr/bin/env bash
set -euo pipefail

# Colors for output
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}Starting integration test for containers...${NC}"

# Bring up the stack
echo -e "${GREEN}Building and starting containers...${NC}"
docker compose down -v
docker compose up -d --build

# Wait and poll backend health
RETRIES=20
BACKEND_READY=false
echo -e "${YELLOW}Polling backend health endpoint (http://127.0.0.1:8085/actuator/health)...${NC}"

for ((i=1; i<=RETRIES; i++)); do
    # Try querying health endpoint and check if status is UP
    if RESPONSE=$(curl -sSf --max-time 3 http://127.0.0.1:8085/actuator/health 2>/dev/null); then
        if echo "$RESPONSE" | grep -q '"status":"UP"'; then
            echo -e "${GREEN}Backend is UP and healthy!${NC}"
            BACKEND_READY=true
            break
        fi
    fi
    echo "Backend not ready yet, retrying in 5 seconds ($i/$RETRIES)..."
    sleep 5
done

# Wait and poll frontend health
FRONTEND_READY=false
if [ "$BACKEND_READY" = true ]; then
    echo -e "${YELLOW}Polling frontend service (http://127.0.0.1:8086/health)...${NC}"
    for ((i=1; i<=5; i++)); do
        if curl -sSf --max-time 3 http://127.0.0.1:8086/health >/dev/null 2>&1; then
            echo -e "${GREEN}Frontend is responding with 200 OK at /health!${NC}"
            FRONTEND_READY=true
            break
        fi
        echo "Frontend not ready yet, retrying in 2 seconds ($i/5)..."
        sleep 2
    done
fi

# Wait and poll worker health
WORKER_READY=false
if [ "$BACKEND_READY" = true ]; then
    echo -e "${YELLOW}Polling import-worker service (http://127.0.0.1:8087/actuator/health)...${NC}"
    for ((i=1; i<=10; i++)); do
        if RESPONSE=$(curl -sSf --max-time 3 http://127.0.0.1:8087/actuator/health 2>/dev/null); then
            if echo "$RESPONSE" | grep -q '"status":"UP"'; then
                echo -e "${GREEN}Import worker is UP and healthy!${NC}"
                WORKER_READY=true
                break
            fi
        fi
        echo "Worker not ready yet, retrying in 3 seconds ($i/10)..."
        sleep 3
    done
fi

# Tear down the stack
echo -e "${GREEN}Tearing down containers...${NC}"
docker compose down -v

# Evaluate success
if [ "$BACKEND_READY" = true ] && [ "$FRONTEND_READY" = true ] && [ "$WORKER_READY" = true ]; then
    echo -e "${GREEN}Integration tests PASSED successfully!${NC}"
    exit 0
else
    echo -e "${RED}Integration tests FAILED!${NC}"
    exit 1
fi
