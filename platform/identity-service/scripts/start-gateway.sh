#!/bin/bash
# Start InsightTube Gateway for local development
# Usage: ./scripts/start-gateway.sh
#
# Prerequisites: Identity Service must be running on port 8081

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SERVICE_DIR="$PROJECT_ROOT/applications/insighttube/gateway"

echo "Starting InsightTube Gateway..."
echo "  Directory: $SERVICE_DIR"
echo "  Port:      8080"
echo "  Backend:   http://localhost:8081 (Identity Service)"
echo ""

cd "$SERVICE_DIR"

export JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"

./mvnw spring-boot:run
