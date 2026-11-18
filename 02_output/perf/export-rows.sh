#!/usr/bin/env bash
# NFR-02 second half (specification §8): downloads the Export of perf/export.js (search "priimek" in
# "Konferenca 2025") as Ana and prints its data rows (the sheet's rows without the header row).
# Pass value: 10000. Local stack only; needs curl, unzip and python3.
set -euo pipefail
BASE_URL="${BASE_URL:-http://127.0.0.1:8090}"
EMU="${EMULATOR_URL:-http://127.0.0.1:9199}"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

token=$(curl -sf -X POST "$EMU/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key" \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana.kovac@example.si","password":"Kontakti#2026","returnSecureToken":true}' |
  python3 -c 'import json, sys; print(json.load(sys.stdin)["idToken"])')
group=$(curl -sf "$BASE_URL/api/v1/groups" -H "Authorization: Bearer $token" |
  python3 -c 'import json, sys; print(next(g["id"] for g in json.load(sys.stdin) if g["name"] == "Konferenca 2025"))')
curl -sf -X POST "$BASE_URL/api/v1/groups/$group/exports" -H "Authorization: Bearer $token" \
  -H 'Content-Type: application/json' -d '{"q":"priimek"}' -o "$work/export.xlsx"
rows=$(unzip -p "$work/export.xlsx" 'xl/worksheets/sheet1.xml' | grep -o '<row ' | wc -l)
echo "data rows: $((rows - 1))"
