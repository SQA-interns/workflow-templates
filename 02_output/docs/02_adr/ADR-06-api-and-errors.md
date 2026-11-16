# ADR-06: REST API style and error model

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: AR-01, AR-02, BR-02, BR-03, BR-06, SB-07, ES-07, UI-30, R-006-02 … R-006-05

## Context
Every UI capability is in the REST API (AR-01), documented as OpenAPI (AR-02). User-facing messages exist in two languages (UI-30), so the API must not return finished sentences in one language.

## Options
1. RFC 9457 `application/problem+json` with a stable `code`, `params` and optional per-field `errors`; the frontend turns `code` into text from its catalog.
2. Plain status codes with English messages.

## Decision
Option 1.
- Resources under `/api/v1`, JSON, camelCase, glossary names (ES-13). Actions that are not CRUD use a verb sub-resource named as the glossary (`/move-to-recycle-bin`, `/restore`, `/merges/{id}/undo`).
- Error body: `{type, title, status, code, params?, errors?: [{field, code, params?}]}`; `title` is English for logs and API users; the UI shows the catalog text for `code`.
- Status use: 400 validation (`errors`), 401 `auth.unauthenticated`, 403 `auth.forbidden` / `auth.emailNotVerified`, 404 `notFound` (also for other Groups' data, BR-02), 405 for Version change attempts (EX-010-03), 409 state conflicts (`version.conflict` with `params.changedBy`, `contact.inRecycleBin`, …), 429 `rateLimited`.
- Errors never carry stack traces, SQL or personal data beyond what the caller sent (SB-07).
- The OpenAPI file in `docs/02_contracts/openapi.yaml` is the contract; springdoc generates the runtime document, and a contract test compares the two (DoD-P04).

## Consequences
- Each story's messages become catalog keys; NFR-08 checks both catalogs have every key.
