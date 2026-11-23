#!/usr/bin/env bash
# Builds the three deployable images, tagged with the current git commit (RELEASE_TAG in deploy/<env>.env).
# Needs JDK 21, Node.js 22.23.3 and Docker. Move the images to a host without them with:
#   docker save contacts-backend:<tag> contacts-frontend:<tag> contacts-postgres:16.15 | ssh <host> docker load
set -euo pipefail
cd "$(dirname "$0")/.."
tag="${RELEASE_TAG:-$(git rev-parse --short HEAD)}"
(cd backend && ./mvnw -B -q -DskipTests package)
(cd frontend && npm ci --no-audit --no-fund --ignore-scripts && npm run build)
docker build -q -t "contacts-backend:$tag" backend
docker build -q -t "contacts-frontend:$tag" frontend
docker build -q -t contacts-postgres:16.15 docker/postgres
echo "Built contacts-backend:$tag, contacts-frontend:$tag, contacts-postgres:16.15 (RELEASE_TAG=$tag)"
