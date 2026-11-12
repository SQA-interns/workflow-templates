# Environments

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

## Environments

| Environment | Purpose | How it runs |
|---|---|---|
| test | Automated tests in the agent's sessions and CI. | Backend tests with Testcontainers (`postgres` image) and the Firebase Auth emulator; frontend unit tests with mocked API (msw); browser tests against the local stack. No TLS, no real Firebase project. |
| local | Development and the end-to-end tests. | `02_output/docker-compose.yml`: `postgres`, Firebase Auth emulator, `backend`, `frontend` (nginx); product at `http://localhost:8080` [ASSUMPTION port]. HTTP on localhost only. |
| staging | Customer acceptance (DoD-P02, DoD-P03, DoD-P05). | The same container images with Docker Compose on a Linux VPS or a cloud the customer chooses; HTTPS through a reverse proxy; a real Firebase project for staging. Host and operator: OPEN — needs customer (Q-39). |
| production | Live use. | As staging, with its own Firebase project and database. Where it runs and who operates it: OPEN — needs customer (Q-39). |

Persistent data lives in a named Docker volume for PostgreSQL that survives container recreation; in test, the database is created per test run and discarded. Backups on staging and production: daily `pg_dump` at 02:00 Europe/Ljubljana [ASSUMPTION time], kept 30 days, stored outside the database host [ASSUMPTION]; one restore tested before handover (Q-39, DoD-P05).

## Configuration

Settings that differ between environments (ES-01). Secrets are listed in `project/00_setup/secrets.env.example`, not here.

| Setting | Environments | Default allowed? |
|---|---|---|
| `DB_URL`, `DB_USER` | all | yes for test and local (`jdbc:postgresql://postgres:5432/contacts`, `contacts`); no for staging and production |
| `FIREBASE_PROJECT_ID` | all | yes for test and local (`demo-contacts`, emulator); no for staging and production |
| `FIREBASE_AUTH_EMULATOR_HOST` | test, local | yes (`firebase-emulator:9099`); must be unset in staging and production |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_AUTH_DOMAIN` (frontend build) | all | yes for test and local (emulator values); no for staging and production; not secret, but per environment |
| `APP_PUBLIC_URL` (CORS origin, Invitation links) | all | yes for test and local (`http://localhost:8080`); no for staging and production |
| `RATE_LIMIT_PER_MINUTE` | all | yes (`100`, SR-03) |
| `LOG_LEVEL` | all | yes (`INFO`) |

## External services

| Service | Production | Local/test substitute | Must be tested manually by a human? |
|---|---|---|---|
| Firebase Authentication | A Firebase project per environment (staging, production); e-mail/password and Google providers enabled | Firebase Auth emulator (`firebase-tools`), project id `demo-contacts` | Yes: sign-in with a real Google account on staging (the emulator fakes Google sign-in). The Firebase project must be created or provided by the developer before `/preflight` (Q-36 OPEN). |
