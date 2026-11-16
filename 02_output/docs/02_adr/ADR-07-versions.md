# ADR-07: Storing Versions

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: BR-05, R-010-01 … R-010-03, R-011-01 … R-011-04, R-019-01, AR-08, SR-06

## Context
Every saved change creates one Version with who, when, the changed items and the full content after the change; History shows them, Restore and Undo merge rebuild the Contact from them.

## Options
1. Table `contact_version` with `changes jsonb` (list of item, old, new) and `snapshot jsonb` (Fields, Attributes, Tags after the change).
2. Event sourcing: only changes, content rebuilt by replay.
3. Shadow tables per entity.

## Decision
Option 1. One service, `VersionWriter`, computes the changed items between the previous and the new snapshot and writes the row in the same transaction (AR-08). Tags are stored in the snapshot by name; Restore re-creates missing Tags (R-011-04). No update or delete path exists while the Contact exists (R-010-03); permanent delete removes all rows of the Contact (SR-06).

## Consequences
- History and Version view are single reads; Restore and Undo merge are "write snapshot n as new content".
- Snapshots duplicate personal data, which is why permanent delete and BR-08 erase all Versions.
