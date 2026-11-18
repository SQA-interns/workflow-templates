# Performance and capacity checks

> For: human, agent · Written in: phase 4 (`/implement`, T-39, T-40) · Source: `docs/02_specification.md` §8

Scripts of NFR-01, NFR-02 and NFR-03. They run against the **local stack** (`scripts/up.sh`), never against staging or production: they empty its database and its emulator accounts.

| File | Purpose |
|---|---|
| `load-seed.sh` | Empties the local stack, loads `seed/seed.sql` and `seed-10k.sql`; with `capacity` also `seed-capacity.sql` |
| `seed-10k.sql` | 10,000 Contacts in "Konferenca 2025", each with 5 Attributes, 3 Tags and Version 1 |
| `seed-capacity.sql` | 50 Groups and 200 Users in total; 48 extra Groups of 100 Contacts each |
| `support.js` | k6 helpers: emulator sign-in, API requests, one Viewer per virtual User (joins by Invitation) |
| `contact-list.js` | NFR-01: 20 virtual Users, 2 minutes, list, search and filter requests; `p(95) < 500 ms` |
| `export.js` | NFR-02: one timed Export of the 10,000 Contacts (search "priimek"); `< 10 s` |
| `export-rows.sh` | NFR-02: downloads the same Export and prints its data rows; `10000` |

## Prerequisites

- The local stack is up (`scripts/up.sh`); the app at `http://127.0.0.1:8090` and the emulator at `http://127.0.0.1:9199` (set `BASE_URL` and `EMULATOR_URL` otherwise; `APP_HOST_PORT`, `EMULATOR_HOST_PORT` of `docker-compose.yml`).
- Docker (k6 runs as the pinned image `grafana/k6:1.3.0`), `curl`, `unzip`, `python3`.
- Each virtual User is its own Member, so the rate limit of 100 requests a minute per User (SR-03) holds at one request a second.

## Run (from `02_output/`)

```sh
# NFR-01 and NFR-02
perf/load-seed.sh
docker run --rm --network host -v "$PWD/perf:/perf:ro" grafana/k6:1.3.0 run /perf/contact-list.js
docker run --rm --network host -v "$PWD/perf:/perf:ro" grafana/k6:1.3.0 run /perf/export.js
perf/export-rows.sh

# NFR-03: the same three commands after
perf/load-seed.sh capacity
```

k6 exits with 99 when a threshold fails. Leave the stack as the browser tests expect it with `scripts/seed-local.sh` or let the next test empty it.

## Results on the local stack (2026-11-18, one host)

| Check | Result | Log |
|---|---|---|
| NFR-01 | p(95) 135 ms, 100 % answers 200 | `out/logs/implement-t39-nfr01-contact-list.txt` |
| NFR-02 | 1.1 s, 10,000 data rows | `out/logs/implement-t40-nfr02-export.txt` |
| NFR-03 | list p(95) 153 ms; Export 3.3 s, 10,000 rows | `out/logs/implement-t39-nfr03-capacity.txt` |

NFR-03 first failed (list p(95) 3.71 s, `out/logs/implement-t39-nfr03-capacity-before-fix.txt`): the Tag filter under a generic plan of the prepared statement; fixed in the list query (D-82).

## Troubleshooting

- `no Group Konferenca 2025` or `perf Tags missing`: run `perf/load-seed.sh` first.
- `the search finds … Contacts, not 10,000`: other Contacts match "priimek"; reload with `perf/load-seed.sh`.
- Many `429`: fewer Users than virtual Users, or a lower `RATE_LIMIT_PER_MINUTE` on the backend.
