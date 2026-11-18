# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-021 D-77: 1
- Result: done
- Gate: phase 4 (US-021 DoD-S) passed
- Notes for the next session:
  - D-77 option 1 applied (one locator of `acceptance/us021-delete-account.spec.ts`, manifest regenerated); US-021 API 5/5, UI 3/3 (`out/logs/implement-us021-ui-acceptance.txt`).
  - Backend unit 467/467, integration 270/270 (`out/logs/implement-us021-full-verify.txt`); frontend 355/355 without coverage (TagsField times out with coverage, D-67); static checks and gitleaks clean (`frontend/README.md` still fails prettier, older).
  - Browser run (before the D-77 change) 95/97: AC-021-01 (now passes) and E2E-08 (D-75 (j), US-002/US-003 code, for `/review INC-2`); E2E-01 passes.
  - D-76 (non-blocking, pending review): US-021 assumptions; `UserService.closeAccount` is the specification's `deleteAccount` (D-76 (h)); DoD-S-07 notes in D-76 (i).
  - T-53 (SR-02 matrix and contract test for the INC-2 endpoints, incl. `DELETE /me`) is not done yet.
  - Stack on 8090 is built from 81733ed (no code change since).

## Next
- Command: /review INC-2
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
| US-013 | INC-1 | done | DoD-S-01 met after US-018 (API 6/6, UI 2/2; EX-013-03 needed 4657746); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); DoD-S-07 see notes of US-014; others met |
| US-015 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 5/5); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 b7b36b7 554 lines, 5168f66 and daf3e85 subjects over 72 characters; others met |
| US-017 | INC-2 | done | DoD-S-01 met after US-018 (API 12/12, UI 4/4; D-71); DoD-S-07 see D-70 (i); others met |
| US-018 | INC-2 | done | met (API 10/10, UI 4/4; WF-17, WF-18, WF-07 at three widths, axe clean); DoD-S-07 see D-72 (j), D-74 (c) |
| US-019 | INC-2 | done | met (API 5/5, UI 3/3; WF-07 Undo merge at three widths, axe clean); DoD-S-07 see D-75 (i) |
| US-020 | INC-1 | done | DoD-S-01 met after D-65 (UI 4/4, D-69); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | done | met (API 5/5, UI 3/3 after D-77; WF-19 at three widths, axe clean); DoD-S-07 see D-76 (i) |
