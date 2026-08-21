#!/bin/bash
# Start Identity Service for local development
# Usage: ./scripts/start-identity.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SERVICE_DIR="$PROJECT_ROOT/platform/identity-service"

echo "Starting Identity Service..."
echo "  Directory: $SERVICE_DIR"
echo "  Profile:   local"
echo "  Timezone:  UTC"
echo "  Port:      8081"
echo ""

cd "$SERVICE_DIR"

# CRITICAL: PostgreSQL rejects Asia/Calcutta timezone
export JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"

./mvnw spring-boot:run -Dspring-boot.run.profiles=local
