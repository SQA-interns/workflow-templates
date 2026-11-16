# ADR-09: Export to .xlsx

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: R-016-01 … R-016-07, SR-07, AR-09, NFR-02, D-24

## Context
Up to 10,000 Contacts in < 10 s, built in the backend (AR-09), formula-injection safe (SR-07).

## Options
1. Apache POI `SXSSFWorkbook` (streaming, 100-row window), every cell written as a string cell; `POST /api/v1/groups/{groupId}/exports` with the list query or the Checked contacts' ids.
2. Client-side generation.

## Decision
Option 1. The scope is resolved with the same query service as the list (BR-09), counted first (> 10,000 → 409 `export.tooMany`, R-016-05). Every value is a string cell; a value starting with `=`, `+`, `-`, `@`, tab or carriage return is written with a leading apostrophe-style quote prefix (`CellStyle.setQuotePrefixed(true)`), so Excel shows it as text (SR-07). File name per R-016-06 and D-24, sent as `Content-Disposition` with RFC 5987 UTF-8 encoding. One Audit entry per Export (BR-10). Layout contract: `docs/02_contracts/export-xlsx.md`.

## Consequences
- Memory stays flat; NFR-02 measured by the timed test in `docs/02_tasks.md`.
