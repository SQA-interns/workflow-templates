# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-001 (resumed with D-40: 1, D-41: 1, recorded as D-43)
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests waiting for US-020 and US-002, D-39)
- Notes for the next session:
  - T-01 … T-05 built test-first (`f4a078b` … `3ee361e`); unit tests green (backend 38, frontend 57); backend check (spotless, checkstyle, spotbugs) clean after the D-41 exclusions (`d03e50c`); gitleaks clean.
  - D-40 applied (`28bf9aa`): five sign-in locators use `input[type="email"]`; acceptance manifest regenerated and valid.
  - Frozen US-001 passing: API AC-001-05, EX-001-03; UI AC-001-02, AC-001-05. All others fail only at `PATCH /me` (US-020: UI AC-001-01, 06, EX-001-01, 05) or `POST/GET /groups` (US-002: API AC-001-01 … 04, 06, EX-001-02, 04). Re-run them in `/implement US-020` and `/implement US-002`; set US-001 to done when they pass (D-39).
  - `UserMenu.tsx` has only Sign out; US-020 adds the language items. `/` always shows WF-05 until US-002 decides by `GET /groups`.
  - WF-05 illustration placeholder not drawn (no asset before `/design`). Screens and axe results: `out/logs/screens/WF-0*`.
  - Commit `ffa2f0f` has 16 files / 584 lines, above the 400-line limit (history not rewritten).
  - The `kontakti` stack runs on 8180/9199 with this session's build.

## Next
- Command: /implement US-020
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | in progress | DoD-S-01 open until US-020 and US-002 (D-39); others met |
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
