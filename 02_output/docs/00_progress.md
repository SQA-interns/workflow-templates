# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S3 /plan
- Result: done
- Gate: phase 2 passed except the wireframes (they follow in `/wireframes`)
- Notes for the next session:
  - Screens to draw: `docs/02_specification.md` §9.1 (WF-01 … WF-20, no WF-12), navigation §9.2, tokens §9.3; wireframe theme is active in `frontend/src/styles/tokens.css`.
  - Decisions this session: D-28 … D-33 (D-30 CSRF and D-33 gitleaks approved by the human in session).
  - Local stack: `02_output/scripts/up.sh`, seed `scripts/seed-local.sh`. An unrelated stack `contacts` holds host 8080/9099: use `APP_HOST_PORT=8180`, `E2E_BASE_URL=http://localhost:8180` (D-32). Stack stopped at session end; volume `kontakti_pgdata` kept.
  - Backend commands in `backend/README.md`, frontend in `frontend/README.md`; both check, test and build pass; CI `.github/workflows/ci.yml` validated with actionlint (no runner pinned).
  - Tasks T-01 … T-53 in `docs/02_tasks.md`; implement order = INC-1 row.

## Next
- Command: /wireframes
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | ready | |
| US-002 | INC-1 | ready | |
| US-003 | INC-1 | ready | |
| US-004 | INC-1 | ready | |
| US-005 | INC-1 | ready | |
| US-006 | INC-1 | ready | |
| US-007 | INC-1 | ready | |
| US-008 | INC-1 | ready | |
| US-009 | INC-1 | ready | |
| US-010 | INC-1 | ready | |
| US-011 | INC-1 | ready | |
| US-012 | INC-1 | ready | |
| US-013 | INC-1 | ready | |
| US-014 | INC-1 | ready | |
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | ready | |
| US-021 | INC-2 | ready | |
