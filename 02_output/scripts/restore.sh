#!/usr/bin/env bash
# Restores a backup made by backup.sh into a staging or production environment (DoD-P05).
# Replaces the whole database. Usage: scripts/restore.sh deploy/<environment>.env <file.dump>
set -euo pipefail
env_file="$(realpath "${1:?usage: restore.sh deploy/<environment>.env <file.dump>}")"
dump="$(realpath "${2:?usage: restore.sh deploy/<environment>.env <file.dump>}")"
cd "$(dirname "$0")/../deploy"
compose=(docker compose --env-file "$env_file")
"${compose[@]}" stop proxy frontend backend
"${compose[@]}" exec -T postgres pg_restore -U contacts -d contacts --clean --if-exists --no-owner --exit-on-error < "$dump"
"${compose[@]}" up -d --wait
echo "Restored $dump"
