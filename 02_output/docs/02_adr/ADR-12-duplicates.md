# ADR-12: Finding Possible duplicates

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: R-017-01 … R-017-05, D-25, NFR-03

## Context
Pairs by same e-mail, same phone (≥ 6 digits) or Name similarity ≥ 0.8 with the same company, among up to 10,000 active Contacts; Name similarity is Sørensen–Dice over character trigrams of the Normalised full name (D-25).

## Options
1. In the service: load the active Contacts' key columns, bucket by normalised e-mail, by normalised phone and by normalised company, compare pairs only inside a bucket.
2. SQL self-join with `pg_trgm`.

## Decision
Option 1: buckets keep the work near-linear; the Dice function is plain Java and unit-tested against the D-25 values (0.84 for AC-017-03). Pairs are merged per Contact pair with all reasons (EX-017-03), sorted by the first Contact in R-014-02 order (D-25).

## Consequences
- No new extension or index; the computation runs per request (no stored results to keep consistent).
