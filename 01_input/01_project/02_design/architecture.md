# Architecture

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

## Components

Each component gets its own folder and README in `02_output/`. Versions come only from `tech-stack.md`; reference its ids, never repeat a version here.

```yaml
components:
  - name: backend
    type: api
    folder: 02_output/backend
    platform: java
    build: maven
    bootstrap: Spring Initializr-equivalent hand-written pom.xml with the Maven wrapper; root package app.contacts   # [ASSUMPTION] package name not given
    commands:          # ES-05; filled in phase 2 if left empty
      build:
      test:
      check:
      run:
    depends_on: []
    deploys_as: container image (Dockerfile in 02_output/backend)
  - name: frontend
    type: web-app
    folder: 02_output/frontend
    platform: node
    build: vite
    bootstrap: Vite React TypeScript template; Tailwind CSS and shadcn/ui added per UI-01; root module src/
    commands:
      build:
      test:
      check:
      run:
    depends_on: [backend]
    deploys_as: container image serving the static build (nginx) that proxies /api to the backend   # [ASSUMPTION]
```

Local stack: `02_output/docker-compose.yml` starts `postgres`, the Firebase Auth emulator, `backend` and `frontend`; `docker compose up` gives the product at `http://localhost:8080` [ASSUMPTION port]. The same images run on staging and production (Q-39).

## Constraints

Checkable rules the code must respect (layering, allowed dependencies, patterns). Checked automatically in phase 6 where the stack allows (DoD-04).

| ID | Constraint |
|---|---|
| AR-01 | The frontend reaches the backend only through the REST API under `/api/v1`; every capability of the UI is available there (Q-09 F10, Q-11). |
| AR-02 | The REST API is described by an OpenAPI 3 document generated from the backend and published with the release (DoD-P04). |
| AR-03 | Backend layering: `web` (controllers, DTOs) → `service` → `repository`; controllers never use repositories; entities never leave the service layer. Checked with ArchUnit. |
| AR-04 | Every repository method on Group data takes the Group id and filters by it; no query reads Contacts, Tags, Versions, Invitations or Audit entries without a Group id (BR-01). Checked with ArchUnit and a test per endpoint. |
| AR-05 | Authorisation is decided in the backend service layer from the Membership and Role, never from data sent by the client (Q-11). |
| AR-06 | Only the `auth` package uses the Firebase Admin SDK; the rest of the backend sees an authenticated User id. |
| AR-07 | The database schema changes only through Flyway migrations; timestamps are `timestamptz` in UTC (project-wide facts). |
| AR-08 | A Version is written in the same database transaction as the change it records (BR-05). |
| AR-09 | The `.xlsx` file is built in the backend (Apache POI streaming) and returned as a download; no external service (Q-36). |

## Interfaces

Every interface between components or with external systems is defined as a contract in `docs/02_contracts/` in phase 2.

| Interface | Between | Style (e.g. REST, gRPC, events, file, UI) |
|---|---|---|
| Contacts API | frontend ↔ backend | REST over HTTPS, JSON, `/api/v1`, Firebase ID token as Bearer token |
| Sign-in | frontend ↔ Firebase Authentication | Firebase JS SDK (emulator locally and in tests) |
| Token verification | backend ↔ Firebase Authentication | Firebase Admin SDK (public keys; emulator locally and in tests) |
| Persistence | backend ↔ PostgreSQL | JDBC (Spring Data JPA), Flyway |
| Export | backend → User | file download, `.xlsx` |
| UI | User ↔ frontend | web UI |
