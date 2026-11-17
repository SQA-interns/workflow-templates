# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-018
- Result: blocked
- Gate: phase 4 (US-018 DoD-S) not passed (DoD-S-01, DoD-S-05: Docker stopped, D-73)
- Notes for the next session:
  - Built test-first, unit tests green: `MergedContent`, `MergeService`, `POST /groups/{id}/merges`, `mergedFrom`/`mergedInto` on Contact, D-21 delete of merged Contacts (`PermanentDeleteService`), WF-17 Merge button, WF-18 `MergePage`, WF-07 "Merged from" (D-72). Frontend unit 340/340 (`out/logs/implement-us018-frontend-test.txt`).
  - Not run (Docker down, D-73): `MergeRepositoryIT` (committed, never run; covers `findByGroupIdAndPrimaryIdOrderByMergedAt`, `findByGroupIdAndMergedInto`), the full backend suite, the frozen API and UI tests of US-018, US-013 EX-013-03, US-017 EX-017-04, WF-18 screenshots and axe.
  - Baseline before the change: 212 tests, 21 failures (INC-2 frozen tests, EX-013-03, EX-017-04) and 9 repository-IT errors from Docker (`out/logs/implement-us018-baseline-full.txt`).
  - Check under `sl-x-icu` whether "J. Novak" sorts before "Janez Novak" (pair order, D-25 (b)); if so AC-018-02/05 (UI) expect another Primary contact: approval decision (D-73).
  - Resume: run `MergeRepositoryIT`, `./mvnw -B verify`, rebuild the stack 8090 (`npm run build`, then `docker compose --env-file ../.env up -d --build`, `APP_HOST_PORT=8090`), run `acceptance/us018-merge.spec.ts`, screenshot WF-18 at three widths, then DoD-S.
  - `frontend/README.md` fails prettier --check; it was like that before this session (also in the US-017 log).

## Next
- Command: /implement US-018
- Waiting for: D-73

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
| US-017 | INC-2 | done | DoD-S-01 API 11/12, UI 4/4; EX-017-04 after US-018 (D-71); DoD-S-07 see D-70 (i); others met |
| US-018 | INC-2 | in progress | open: DoD-S-01, DoD-S-05 wait for Docker (D-73); DoD-S-07 see D-72 (j) |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | done | DoD-S-01 met after D-65 (UI 4/4, D-69); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
