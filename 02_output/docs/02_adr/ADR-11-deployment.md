# ADR-11: Containers and local stack

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: architecture.md (deploys_as), environments.md, SB-04, SB-10, SB-11, NFR-04, D-11/D-12, D-28, D-29

## Context
The same images run locally, on staging and in production. The postgres and nginx images must be upgraded at build time (D-12). Host port 9099 is taken (preflight).

## Options
1. One Dockerfile per component plus project images for `postgres` and the Auth emulator; one `docker-compose.yml` for the local stack.
2. Kubernetes manifests.

## Decision
Option 1.
- `backend/Dockerfile`: `eclipse-temurin:21.0.13_9-jre`, non-root user, copies the jar built by `./mvnw package`; health check on `/actuator/health`.
- `frontend/Dockerfile`: `nginx:1.30.6-alpine` + `apk upgrade`, copies `dist/`, non-root, security headers (CSP, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`), proxies `/api/` to the backend, writes `config.json` at start (ADR-05).
- `docker/postgres/Dockerfile`: `postgres:16.15` + `apt-get upgrade` (D-12).
- `docker/firebase-emulator/Dockerfile`: local and test only, never deployed (D-29).
- `docker-compose.yml`: `postgres` (named volume), `firebase-emulator` (host port 9199 for the browser, 9099 inside the network), `backend`, `frontend` on `http://localhost:8080`; `restart: unless-stopped` (NFR-04).
- TLS is terminated by the staging/production reverse proxy (SB-04, SR-09).

## Consequences
- One backend instance per environment (ADR-03, ADR-10).
- Staging compose and reverse proxy are written at `/release` once the host is known (Q-39).
