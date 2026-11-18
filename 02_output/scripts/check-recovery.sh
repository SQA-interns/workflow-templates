#!/usr/bin/env bash
# NFR-04 runtime demonstration (specification §8): the backend reports UP on /actuator/health, runs
# with the restart policy unless-stopped, and after `docker compose restart backend` reports UP
# again within 60 seconds. Run from anywhere against the local stack (scripts/up.sh); prints each
# step and exits non-zero when a check fails. Secrets come from ../.env without being printed.
set -euo pipefail
cd "$(dirname "$0")/.."
LIMIT_SECONDS=60
compose() { docker compose --env-file ../.env "$@"; }

# The image has no curl: the same probe as the image's HEALTHCHECK (bash /dev/tcp, ES-09).
health() {
  compose exec -T backend bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 &&
    printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && tail -n 1 <&3' 2>/dev/null || true
}

container=$(compose ps -q backend)
[[ -n "$container" ]] || { echo "FAIL: the backend is not running (scripts/up.sh)"; exit 1; }

policy=$(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$container")
echo "restart policy: $policy"
[[ "$policy" == "unless-stopped" ]] || { echo "FAIL: restart policy is not unless-stopped"; exit 1; }

before=$(health)
echo "health before: $before"
[[ "$before" == *'"UP"'* ]] || { echo "FAIL: backend is not UP"; exit 1; }

start=$(date +%s)
compose restart backend > /dev/null
echo "restarted at $(date -u +%H:%M:%SZ)"
while true; do
  elapsed=$(( $(date +%s) - start ))
  after=$(health)
  if [[ "$after" == *'"UP"'* ]]; then
    echo "health after: $after"
    echo "recovery: ${elapsed} s (limit ${LIMIT_SECONDS} s)"
    break
  fi
  if (( elapsed >= LIMIT_SECONDS )); then
    echo "FAIL: not UP ${LIMIT_SECONDS} s after the restart (last answer: ${after:-none})"
    exit 1
  fi
  sleep 1
done
echo "PASS: NFR-04"
