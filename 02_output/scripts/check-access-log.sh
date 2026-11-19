#!/usr/bin/env bash
# SB-07, SR-08, SR-05 runtime check: the frontend's nginx access log carries no Contact e-mail,
# search text or Invitation token (F-38). Sends requests with marker values through the running
# local stack (scripts/up.sh), then searches the frontend log written since then; exits non-zero
# when a marker is found. Sends no token, so every API request answers 401. Secrets come from
# ../.env without being printed.
set -euo pipefail
cd "$(dirname "$0")/.."
BASE="http://localhost:${APP_HOST_PORT:-8080}"
compose() { docker compose --env-file ../.env "$@"; }

since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
marker="logcheck$(date +%s)"
email="${marker}@example.si"
token="${marker}Tok3nAbCdEfGhIjKlMnOpQr"
group="00000000-0000-4000-8000-0000000b0001"

curl -s -o /dev/null "$BASE/api/v1/groups/$group/contacts/email-matches?email=$email"
curl -s -o /dev/null "$BASE/api/v1/groups/$group/contacts?q=${marker}name"
curl -s -o /dev/null "$BASE/api/v1/invitations/$token"
curl -s -o /dev/null -X POST "$BASE/api/v1/invitations/$token/accept"
curl -s -o /dev/null "$BASE/invitations/$token"
curl -s -o /dev/null -H "Referer: $BASE/invitations/$token?email=$email" "$BASE/"
sleep 1

log=$(compose logs --no-color --since "$since" frontend 2>&1)
lines=$(printf '%s\n' "$log" | grep -c '"GET \|"POST ' || true)
echo "access log lines since $since: $lines"
(( lines >= 6 )) || { echo "FAIL: the requests were not logged (is the stack up?)"; exit 1; }
found=0
for value in "$email" "${marker}name" "$token" "${marker}"; do
  if printf '%s\n' "$log" | grep -qF "$value"; then
    echo "FAIL: the access log contains ${value/$marker/<marker>}"
    found=1
  fi
done
(( found == 0 )) || exit 1
echo "PASS: no e-mail, search text or Invitation token in the access log"
