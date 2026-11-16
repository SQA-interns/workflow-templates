# Contracts

> For: agent (writes), human (reads) · Written in: phase 2 (`/plan`) · Agent: writes

One contract per interface of `project/02_design/architecture.md` ("Interfaces"). Validation output: `out/logs/plan-contracts-validation.txt`, `out/logs/plan-openapi-lint.txt`.

| Interface | Contract | Validated by |
|---|---|---|
| Contacts API (frontend ↔ backend) | `openapi.yaml` (OpenAPI 3.1) | `npx @redocly/cli@2.76.0 lint` (D-28); later a contract test against springdoc's runtime document (DoD-P04) |
| Sign-in (frontend ↔ Firebase) | `runtime-config.schema.json` (what the app needs to start the Firebase SDK) | JSON Schema 2020-12 meta-validation + example |
| Token verification (backend ↔ Firebase) | `id-token-claims.schema.json` | JSON Schema 2020-12 meta-validation + example |
| Persistence (backend ↔ PostgreSQL) | `02_output/backend/src/main/resources/db/migration/V1__schema.sql` | Flyway migrate in the backend skeleton test (Testcontainers) |
| Export (backend → User) | `export-xlsx.schema.json` | JSON Schema 2020-12 meta-validation + example; Export tests validate every produced file |
| UI (User ↔ frontend) | `docs/02_specification.md` ("User interface") and `docs/wireframes/` | wireframe approval (`/wireframes`) |
