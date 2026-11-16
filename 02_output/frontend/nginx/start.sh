#!/bin/sh
# Writes /config.json (contract runtime-config.schema.json) and the nginx configuration from the
# environment (ADR-05), then starts nginx.
set -eu
: "${FIREBASE_WEB_API_KEY:?}" "${FIREBASE_AUTH_DOMAIN:?}" "${FIREBASE_PROJECT_ID:?}"
export AUTH_EMULATOR_URL="${AUTH_EMULATOR_URL:-}"
export BACKEND_URL="${BACKEND_URL:-http://backend:8080}"
emulator=""
if [ -n "$AUTH_EMULATOR_URL" ]; then
  emulator=$(printf ',\n  "authEmulatorUrl": "%s"' "$AUTH_EMULATOR_URL")
fi
printf '{\n  "firebaseApiKey": "%s",\n  "firebaseAuthDomain": "%s",\n  "firebaseProjectId": "%s"%s\n}\n' \
  "$FIREBASE_WEB_API_KEY" "$FIREBASE_AUTH_DOMAIN" "$FIREBASE_PROJECT_ID" "$emulator" \
  > /usr/share/nginx/html/config.json
envsubst '${AUTH_EMULATOR_URL} ${FIREBASE_AUTH_DOMAIN} ${BACKEND_URL}' \
  < /etc/nginx/nginx.conf.template > /tmp/nginx.conf
exec nginx -c /tmp/nginx.conf -g 'daemon off;'
