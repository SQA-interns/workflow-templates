# ADR-10: Removal after the recycle bin period

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: BR-08, R-012-05, R-003-06, R-012-04, D-18, D-19, D-21 (via D-27), SR-06, NFR-05

## Context
Contacts and Groups in the recycle bin must be erased after 30 days (UTC, from `deletedAt`), at least hourly (D-18), within 1 minute for 10,000 Contacts (NFR-05). Restore must be refused from the removal date even if the job has not run yet.

## Options
1. Spring `@Scheduled` job in the backend every 15 minutes, batch deletes in one transaction per Group; restore compares with `deletedAt + 30 days` itself.
2. A separate worker or database cron.

## Decision
Option 1. `RemovalJob` runs every 15 minutes (`fixedDelay`): for each Contact due → erase it, its Versions, Attributes, Tag assignments, and Contacts merged into it (D-21), with an Audit entry each; for each Group due → erase the Group with everything it owns, including its Audit entries and Merge records (D-19). The period is a constant from the project-wide facts. A clock (`java.time.Clock` bean) makes boundaries testable (EX-003-02, EX-012-02).

## Consequences
- One backend instance (ADR-11); several instances would need a lock (new ADR).
