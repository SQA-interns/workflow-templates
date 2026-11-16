# ADR-02: Persistence, tenancy and concurrency

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: BR-01, BR-02, BR-06, AR-04, AR-07, AR-08, NFR-01, NFR-03, R-014-02, R-014-04, ES-08

## Context
PostgreSQL 16 (tech-stack), Spring Data JPA and Flyway. Every Group's data must stay inside the Group (P-1); concurrent edits must be detected (BR-06); the Contact list must answer search, sort and filters on 10,000 Contacts in < 500 ms p95 (NFR-01).

## Options
1. Shared schema, `group_id` column on every Group-owned table, repository methods that take the Group id; optimistic concurrency on `contact.version_number`.
2. Schema per Group.
3. PostgreSQL row-level security with a session variable.

## Decision
Option 1.
- Every Group-owned table has `group_id`; every repository method on Group data takes it as a parameter (AR-04, ArchUnit + endpoint tests).
- Concurrency: the client sends the `versionNumber` it read; the service compares it with the current one inside the transaction, with `SELECT … FOR UPDATE` on the Contact row; mismatch → 409 `version.conflict` with the display name of the User of the current Version (BR-06). The Version row is written in the same transaction (AR-08).
- Search: a stored column `contact.search_text` = Normalised text (US-014) of first name, last name, e-mail, phone, company, city, maintained by the service on every save; `LIKE '%…%'` with escaped `%`/`_` (EX-014-05) on a `pg_trgm` GIN index.
- Sort: ICU collation `sl-x-icu` on `sort_name` = last name, else first name, else company (R-014-02, EX-014-04), then first name, then company.
- Schema only through Flyway (`db/migration/V<n>__*.sql`), timestamps `timestamptz` in UTC (AR-07).

## Consequences
- One database, simple backups (DoD-P05); tenant isolation relies on code discipline, guarded by ArchUnit and a cross-Group test for every endpoint (SR-02).
- `pg_trgm` extension created in the first migration; the `postgres` image ships it.
