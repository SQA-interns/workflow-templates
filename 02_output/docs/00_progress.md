# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S2 /clarify
- Result: blocked
- Gate: phase 1 not passed (D-15 … D-21 open; DoR-R-02, DoR-R-03)
- Notes for the next session:
  - Results and the rule → criteria map: `docs/01_requirements-check.md`; decisions D-15 … D-26 in `docs/decisions-log.md`.
  - D-22 … D-26 are non-blocking with defaults (pending review); apply them, do not re-ask.
  - On the answer: record `Human response` and resolution for each of D-15 … D-21, update the story table and DoR-R in the check, set every story ready if no Block remains.
  - The Stories table marks blocked stories "not ready"; increment 1 = MVP, 2 = US-017 … US-019, US-021 (intent.md).

## Next
- Command: /clarify
- Waiting for: D-15, D-16, D-17, D-18, D-19, D-20, D-21

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | not ready | |
| US-002 | 1 | not ready | |
| US-003 | 1 | not ready | |
| US-004 | 1 | not ready | |
| US-005 | 1 | not ready | |
| US-006 | 1 | not ready | |
| US-007 | 1 | not ready | |
| US-008 | 1 | not ready | |
| US-009 | 1 | not ready | |
| US-010 | 1 | not ready | |
| US-011 | 1 | not ready | |
| US-012 | 1 | not ready | |
| US-013 | 1 | not ready | |
| US-014 | 1 | not ready | |
| US-015 | 1 | not ready | |
| US-016 | 1 | not ready | |
| US-017 | 2 | not ready | |
| US-018 | 2 | not ready | |
| US-019 | 2 | not ready | |
| US-020 | 1 | ready | |
| US-021 | 2 | not ready | |
