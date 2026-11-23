# Kontakti: shared contact management

> For: agent (writes), human (reads) · Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

A web application where a Group of people keeps its contacts together. Members sign in with Firebase Authentication (e-mail/password or Google), are invited by the Group's Owner and work as Owner, Editor or Viewer. Contacts have Fields, free Attributes and Tags; every change is kept as a Version that can be restored. The product also has search and filters, duplicate detection and Merge, a recycle bin with automatic removal after 30 days, `.xlsx` Export, and account deletion. The UI is in Slovenian and English.

Delivered stories, known limitations and manual tests: `docs/release-notes.md`.

## Components

| Component | What it is                                                                         | README                                   |
| --------- | ---------------------------------------------------------------------------------- | ---------------------------------------- |
| backend   | Spring Boot REST API under `/api/v1`, PostgreSQL with Flyway, Firebase Admin SDK   | [backend/README.md](backend/README.md)   |
| frontend  | React single-page application served by nginx, which proxies `/api` to the backend | [frontend/README.md](frontend/README.md) |

Supporting folders: `docker/` (PostgreSQL image; Firebase Auth emulator image for local and test only), `deploy/` (staging and production), `scripts/` (start, seed, image build, backup, restore), `perf/` (NFR load checks, [perf/README.md](perf/README.md)), `seed/` (fictitious demo data).

## Quick start (local stack)

Prerequisites: Docker with Compose v2, JDK 21, Node.js 22.23.3 first on `PATH` (with nvm: `nvm use 22.23.3`), `curl`.

1. In the repository root, create `.env` from the template and set `DB_PASSWORD` to any value (the local stack needs no other secret):
   `cp 01_input/01_project/00_setup/secrets.env.example .env`
2. Build both components and start the stack:
   `02_output/scripts/up.sh`
   The product runs at `http://localhost:8080` and the Auth emulator at `http://localhost:9199`. Other host ports: `APP_HOST_PORT=8090 EMULATOR_HOST_PORT=9299 02_output/scripts/up.sh`.
3. Optional demo data: `02_output/scripts/seed-local.sh`. It creates `ana.kovac@example.si` (Owner), `marko.kranjc@example.si` (Editor) and `petra.horvat@example.si` (Viewer), all with the password `Kontakti#2026` (emulator only).
4. Stop with `docker compose -f 02_output/docker-compose.yml --env-file .env down`. Add `-v` to delete the database volume too.

Build, test and check commands of each component (ES-05) are in its README. CI runs them on every push: `.github/workflows/ci.yml`.

## Deploy

Staging and production run the same images as the local stack, without the emulator, behind a TLS reverse proxy (ADR-11, D-97). The host and operator are not decided yet (Q-39), so the files do not depend on any provider: any Linux host with Docker Compose v2 and ports 80 and 443 open.

| File                                      | Purpose                                                                                                    |
| ----------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `deploy/docker-compose.yml`               | `postgres` (named volume), `backend`, `frontend`, `proxy` (nginx 1.30.7, TLS 1.2/1.3, HSTS, HTTP → HTTPS)  |
| `deploy/proxy/nginx.conf`                 | Proxy configuration. It writes no access log: the frontend container writes the redacted one (F-38)        |
| `deploy/env.example`                      | Every setting of one environment; copy to `deploy/staging.env` or `deploy/production.env` (ignored by git) |
| `scripts/build-images.sh`                 | Builds `contacts-backend:<commit>`, `contacts-frontend:<commit>` and `contacts-postgres:16.15`             |
| `scripts/backup.sh`, `scripts/restore.sh` | `pg_dump` backup with 30-day retention; restore of one backup (DoD-P05)                                    |

Steps for one environment (shown for staging):

1. **Firebase:** create a Firebase project for the environment. Enable the e-mail/password and Google providers and add the domain under Authentication → Settings → Authorized domains. Note the web API key and auth domain. Create a service account key (Project settings → Service accounts) and store the JSON file on the host, readable by the container user (e.g. `chmod 0444` inside a directory only root can enter).
2. **Certificate:** put `fullchain.pem` and `privkey.pem` for the domain in a directory on the host (e.g. from certbot or the hosting provider), readable by uid 101. The operator renews them and then runs `docker compose --env-file staging.env restart proxy`.
3. **Images:** on a machine with the prerequisites above, run `02_output/scripts/build-images.sh`; it prints the `RELEASE_TAG`. If the host is another machine: `docker save contacts-backend:<tag> contacts-frontend:<tag> contacts-postgres:16.15 | ssh <host> docker load`. Also copy `02_output/deploy/` and `02_output/scripts/` to the host.
4. **Settings:** `cd 02_output/deploy && cp env.example staging.env`, fill every value and `chmod 600 staging.env`. Never set `FIREBASE_AUTH_EMULATOR_HOST` or `AUTH_EMULATOR_URL` here.
5. **Start:** `docker compose --env-file staging.env up -d --wait`. Check: `https://<APP_DOMAIN>/` shows the sign-in page, and `curl -s -o /dev/null -w '%{http_code}' https://<APP_DOMAIN>/api/v1/groups` prints `401`.
6. **Backups:** mount storage that is not on the database host's disk, set `BACKUP_DIR` to it and add a cron entry:
   `CRON_TZ=Europe/Ljubljana` and `0 2 * * * /path/to/02_output/scripts/backup.sh /path/to/02_output/deploy/staging.env`
7. **Restore** (once before handover, DoD-P05; replaces the whole database): `02_output/scripts/restore.sh 02_output/deploy/staging.env <BACKUP_DIR>/contacts-<time>.dump`.

Update to a new release: build the images with the new tag, set `RELEASE_TAG` and run step 5 again. Flyway migrates the schema when the backend starts. Logs: `docker compose --env-file staging.env logs backend frontend`.

These steps were rehearsed on the build host with a self-signed certificate (`out/logs/s10-deploy-smoke.txt`, `s10-backup-restore.txt`). On a real staging host they are still a manual test (`docs/release-notes.md`).

## Documentation

| Document                                                         | Content                                                                                                  |
| ---------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `docs/release-notes.md`                                          | Delivered stories, known limitations, manual tests, decisions pending review, data retention, deployment |
| `docs/02_specification.md`                                       | Specification: components, data model, API, security, UI                                                 |
| `docs/02_adr/`                                                   | Architecture decisions ADR-01 … ADR-12                                                                   |
| `docs/02_contracts/`                                             | OpenAPI contract of the REST API (`openapi.yaml`, DoD-P04), runtime configuration and Export schemas     |
| `docs/02_tasks.md`                                               | Stories and tasks by increment                                                                           |
| `docs/wireframes/`                                               | Wireframes WF-01 … WF-20                                                                                 |
| `docs/03_test-strategy.md`                                       | Test levels, tools, coverage and mutation thresholds                                                     |
| `docs/04_review-INC-1.md`, `docs/04_review-INC-2.md`             | Increment reviews                                                                                        |
| `docs/06_verification-report.md`, `docs/06_hidden-evaluation.md` | Verification (DoD, NFR, threats) and held-out test results                                               |
| `docs/decisions-log.md`                                          | Every decision D-01 … D-98                                                                               |
| `docs/07_retrospective.md`                                       | Retrospective of the run                                                                                 |
