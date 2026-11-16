#!/usr/bin/env bash
# Loads seed data into the running local stack: emulator accounts plus seed/seed.sql.
# Fictitious people only; every account uses the password Kontakti#2026 (local emulator only).
set -euo pipefail
cd "$(dirname "$0")/.."
EMU="http://localhost:${EMULATOR_HOST_PORT:-9199}"
API="$EMU/identitytoolkit.googleapis.com/v1"
PASSWORD='Kontakti#2026'

uid_for() { # email display-name -> Firebase uid (creates the account if missing, marks e-mail verified)
  local email="$1" name="$2" body uid
  body=$(curl -sf -X POST "$API/accounts:signUp?key=fake-api-key" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$PASSWORD\",\"returnSecureToken\":true}" ||
    curl -sf -X POST "$API/accounts:signInWithPassword?key=fake-api-key" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$PASSWORD\",\"returnSecureToken\":true}")
  uid=$(printf '%s' "$body" | sed -n 's/.*"localId": *"\([^"]*\)".*/\1/p')
  curl -sf -X POST "$API/projects/demo-contacts/accounts:update" -H 'Authorization: Bearer owner' \
    -H 'Content-Type: application/json' \
    -d "{\"localId\":\"$uid\",\"emailVerified\":true,\"displayName\":\"$name\"}" > /dev/null
  printf '%s' "$uid"
}

ANA=$(uid_for ana.kovac@example.si 'Ana Kovač')
MARKO=$(uid_for marko.kranjc@example.si 'Marko Kranjc')
PETRA=$(uid_for petra.horvat@example.si 'Petra Horvat')

docker compose --env-file ../.env exec -T postgres psql -v ON_ERROR_STOP=1 -q -U contacts -d contacts \
  -v ana_uid="$ANA" -v marko_uid="$MARKO" -v petra_uid="$PETRA" < seed/seed.sql
echo "Seed loaded: ana.kovac@example.si (Owner), marko.kranjc@example.si (Editor), petra.horvat@example.si (Viewer)"
