# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S4 /acceptance-tests D-65
- Result: done
- Gate: phase 3 gate passed (manifest re-frozen in 7d602b3); DoD-I-01 of INC-1 now met (US-012, US-020 DoD-S-01 met; US-013 by D-68)
- Notes for the next session:
  - D-65 applied (D-69): frozen `stubGet` answers only `fetch`/`xhr`; helper be08cb8, manifest alone 7d602b3.
  - Re-run Chromium, all specs using `stubGet`: 25/25 pass, AC-012-01 and EX-020-02 included (`out/logs/acceptance-tests-d65-ui.txt`).
  - Review findings F-01 … F-13, F-15 … F-34 of `docs/04_review-INC-1.md` are still open (Medium/Low); F-01 … F-05 touch INC-2 code paths and are worth fixing first.
  - US-013 EX-013-03 must pass after US-018 (T-47); `/review INC-2` checks it (D-68).
  - Local stack 8090 still holds review data; UI tests empty it themselves, but `scripts/seed-local.sh` fails on it: reset the volumes before a run that needs the seed state.

## Next
- Command: /implement US-017
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | done | met (DoD-S-01 met after US-004: API 10/10, UI 5/5) |
| US-003 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 6/6); DoD-S-07 5ef6708 spotless, b07874a … 7b687c6 checkstyle/SpotBugs (fixed in dc2c564, 896ccb9), bdb4dec subject 75 characters; others met |
| US-004 | INC-1 | done | met; DoD-S-07 see D-63 (j) |
| US-005 | INC-1 | done | met (API 9/9, UI 2/2); DoD-S-07 577e900 subject 74 characters (D-66 (i)); others met |
| US-006 | INC-1 | done | DoD-S-01 met after US-004 (API 15/15, UI 5/5); DoD-S-07 see notes of US-006; others met |
| US-007 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 4/4); others met |
| US-008 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 one commit over 400 lines (2ae5475); others met |
| US-009 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); others met |
| US-010 | INC-1 | done | DoD-S-01 met after US-004 (API 8/8, UI 3/3); others met |
| US-011 | INC-1 | done | DoD-S-01 met after US-004 (API 7/7, UI 3/3); DoD-S-07 one subject over 72 characters (24e39f9); others met |
| US-012 | INC-1 | done | DoD-S-01 met after D-65 (API 10/10, UI 4/4, D-69); others met |
| US-013 | INC-1 | in progress | DoD-S-01 open until US-018 (API 5/6, EX-013-03 needs Merge; UI 2/2); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); DoD-S-07 see notes of US-014; others met |
| US-015 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 5/5); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 b7b36b7 554 lines, 5168f66 and daf3e85 subjects over 72 characters; others met |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | done | DoD-S-01 met after D-65 (UI 4/4, D-69); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
