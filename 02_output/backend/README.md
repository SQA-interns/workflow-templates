# Backend (Contacts REST API)

> For: human, agent · Component `backend` of `project/02_design/architecture.md` · Design: `docs/02_specification.md`, ADR-01 … ADR-12

Spring Boot service under `/api/v1` (contract `docs/02_contracts/openapi.yaml`), root package `app.contacts`.

## Prerequisites

- JDK 21 (host 21.0.13.1, D-01, D-96; the image uses `eclipse-temurin:21.0.13_9-jre`); Maven comes through the wrapper (`./mvnw`, Maven 3.9.11).
- Docker (Testcontainers starts `postgres:16.15` for the integration tests).

## Configuration

From environment variables (environments.md, ADR-05). The `local` profile (`SPRING_PROFILES_ACTIVE=local`) supplies the defaults environments.md allows.

| Variable | Purpose | Source |
|---|---|---|
| `DB_URL`, `DB_USER` | JDBC URL and user | environment; local default `jdbc:postgresql://postgres:5432/contacts`, `contacts` |
| `DB_PASSWORD` | database password | `.env` (secret, no default) |
| `FIREBASE_PROJECT_ID` | Firebase project for token verification | environment; local `demo-contacts` |
| `FIREBASE_AUTH_EMULATOR_HOST` | Auth emulator; unset on staging and production | local `firebase-emulator:9099` |
| `FIREBASE_SERVICE_ACCOUNT_FILE` | Admin SDK credentials (US-021, increment 2) | `.env` |
| `APP_PUBLIC_URL` | CORS origin, Invitation links | environment; local `http://localhost:8080` |
| `RATE_LIMIT_PER_MINUTE` | SR-03 | default `100` |
| `LOG_LEVEL` | root log level | default `INFO` |

## Commands (ES-05)

| Task | Command |
|---|---|
| build | `./mvnw -B -DskipTests package` |
| test | `./mvnw -B verify` (unit tests, integration tests with Testcontainers, JaCoCo report in `target/site/jacoco/`) |
| check | `./mvnw -B -DskipTests test-compile spotless:check checkstyle:check spotbugs:check` |
| run | `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` (needs PostgreSQL and the emulator; easier: `docker compose up` in `02_output/`) |
| format | `./mvnw spotless:apply` |
| mutation | `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` |

Health: `GET /actuator/health` (public, ES-09). OpenAPI at runtime: `GET /api/v1/openapi.json` (authenticated).

## Deploy

The image `contacts-backend:<tag>` (`Dockerfile`, user `app`, health check on `/actuator/health`) is built by `../scripts/build-images.sh` and runs on staging and production through `../deploy/docker-compose.yml` (root `README.md`, "Deploy"). There it gets `DB_URL`, `DB_USER`, `DB_PASSWORD`, `FIREBASE_PROJECT_ID`, `APP_PUBLIC_URL` (`https://<APP_DOMAIN>`) and the service account file mounted at `/run/secrets/firebase-service-account.json`. `FIREBASE_AUTH_EMULATOR_HOST` stays unset, so only tokens of the real Firebase project are accepted. Flyway migrates the schema at start; the removal job (ADR-10) runs inside the one backend instance.

## Structure

`web` → `service` → `repository` → `domain`; `auth` for Firebase; `config` for settings (ADR-01, checked by `ArchitectureTest`). Schema: `src/main/resources/db/migration` (Flyway, AR-07).

## Troubleshooting

- Integration tests fail to start: Docker must be running and able to pull `postgres:16.15`.
- The backend does not start on staging: the service account file must exist and be readable by the container user; the backend reads it at start.
- `spotbugs:check` fails: every exclusion in `spotbugs-exclude.xml` needs a decision (D-30, D-41).
