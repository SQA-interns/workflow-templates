#!/usr/bin/env bash
# Empties the local stack and loads the data of the NFR checks (specification §8, perf/README.md):
#   perf/load-seed.sh            seed/seed.sql + perf/seed-10k.sql            (NFR-01, NFR-02)
#   perf/load-seed.sh capacity   the same + perf/seed-capacity.sql             (NFR-03)
# Deletes every row and every emulator account of the local stack first, as the browser tests do
# (D-38). Local stack only; secrets come from ../.env without being printed.
set -euo pipefail
cd "$(dirname "$0")/.."
EMU="http://localhost:${EMULATOR_HOST_PORT:-9199}"
psql_local() { docker compose --env-file ../.env exec -T postgres psql -v ON_ERROR_STOP=1 -q -U contacts -d contacts "$@"; }

psql_local -c "DO \$\$ DECLARE t text; BEGIN SELECT string_agg(quote_ident(tablename), ', ') INTO t
  FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history';
  IF t IS NOT NULL THEN EXECUTE 'TRUNCATE ' || t || ' CASCADE'; END IF; END \$\$;"
curl -sf -X DELETE "$EMU/emulator/v1/projects/demo-contacts/accounts" > /dev/null

scripts/seed-local.sh
psql_local < perf/seed-10k.sql
if [[ "${1:-}" == "capacity" ]]; then
  psql_local < perf/seed-capacity.sql
fi
psql_local -c "SELECT (SELECT count(*) FROM contact_group) AS groups, (SELECT count(*) FROM app_user) AS users,
  (SELECT count(*) FROM contact WHERE group_id = '00000000-0000-4000-8000-0000000b0001') AS contacts_of_konferenca"
