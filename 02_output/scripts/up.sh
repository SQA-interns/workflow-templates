#!/usr/bin/env bash
# Builds both components and starts the local stack (environments.md "local") at http://localhost:8080.
# Secrets come from the repository's .env without being printed (general/rules/secrets.md).
set -euo pipefail
cd "$(dirname "$0")/.."
(cd backend && ./mvnw -B -q -DskipTests package)
(cd frontend && npm ci --no-audit --no-fund --ignore-scripts && npm run build)
docker compose --env-file ../.env up -d --build --wait
echo "Local stack is up: http://localhost:${APP_HOST_PORT:-8080}"
