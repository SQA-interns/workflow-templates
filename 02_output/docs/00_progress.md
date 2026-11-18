# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-019
- Result: done
- Gate: phase 4 (US-019 DoD-S) passed
- Notes for the next session:
  - Backend unit 456/456; integration 263/268, only the 5 US-021 frozen tests fail (`out/logs/implement-us019-full-verify.txt`). Frontend unit 345/345; static checks clean (`frontend/README.md` still fails prettier, older).
  - Browser run now includes `e2e/` (97): 92 pass; fail the 3 US-021 UI tests, E2E-01 (waits for the language choice of WF-19 Account, built by US-021 T-52) and E2E-08 (reload of a Group in the recycle bin shows "This page cannot be found." instead of "You have no Groups yet."; US-002/US-003 code; D-75 (j), for `/review INC-2`) (`out/logs/implement-us019-ui-acceptance-all.txt`).
  - Undo merge: `POST /merges/{id}/undo` with the Primary contact's Version number; checks and Audit action `contacts.mergeUndone` in D-75.
  - SpotBugs counts private `ContactService` methods named replace…/delete… as mutators (D-59 (f), D-75 (i)); avoid such names there.
  - Stack on 8090 is built from a0149e8+ (rebuild after changes: `./mvnw -DskipTests package`, `npm run build`, `APP_HOST_PORT=8090 docker compose --env-file ../.env up -d --build`). gitleaks from `zricethezav/gitleaks:v8.28.0` (D-04).

## Next
- Command: /implement US-021
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
| US-021 | INC-2 | ready | |
