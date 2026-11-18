# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-021 T-36..T-43 T-53 (task group, D-78 (b))
- Result: done
- Gate: phase 4 (INC-2 DoD-I) passed; tasks T-36 … T-43, T-53 done, all checks clean
- Notes for the next session:
  - New tests: `AccessMatrixIT` (SR-02, INC-1 and INC-2 endpoints), SR-04 rules in `ArchitectureTest`, `OpenApiContractIT` (AR-02 against `/api/v1/openapi.json`, behind sign-in), `LogHygieneIT` (SR-08); `perf/` k6 checks with `perf/README.md`; `scripts/check-recovery.sh`; `frontend/e2e/accessibility.spec.ts`, `languages.spec.ts` (screens list in `e2e/support/screens.ts`).
  - Results: NFR-01 p(95) 135 ms, NFR-02 1.1 s / 10,000 rows, NFR-03 153 ms (after 4b86f6d, D-82), NFR-04 14 s, NFR-07 axe clean on 19 screens at three widths, keyboard walk of E2E-01 recorded, NFR-08 105 screenshots. Chromium only (D-38): NFR-06 not run in Firefox or WebKit.
  - Production changes the contract test needed: 201/204 via `@ResponseStatus` (c43b767), `ContactFieldsSchema` (1b80820), `excludeContactId` of email-matches (35b7d66, frontend does not send it). Tag filter rewritten for NFR-03 (4b86f6d, D-82).
  - For `/design` (D-83 (h), D-81 (c)): after "Add an Attribute" the focus stays on the button (new Key field 24 Tabs away); the user-menu trigger is icon-only from 768 to 1023 px.
  - Suites: backend verify 476 + 276, browser 152/152 (Chromium), frontend unit 362/362; checks and gitleaks clean. `perf/load-seed.sh` empties the local stack (port 8090). `frontend/README.md` still fails `prettier --check`.
  - DoD-S-07: 4e41200 has 416 lines (D-83 (j)).

## Next
- Command: /design
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | done | met (DoD-S-01 met after US-004: API 10/10, UI 5/5); F-05 fixed (a264259, E2E-08 passes), F-06 header fixed (089a5a8, D-81) |
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
| US-018 | INC-2 | done | met (API 10/10, UI 4/4; WF-17, WF-18, WF-07 at three widths, axe clean); DoD-S-07 see D-72 (j), D-74 (c); F-01 fixed (8021943, D-79) |
| US-019 | INC-2 | done | met (API 5/5, UI 3/3; WF-07 Undo merge at three widths, axe clean); DoD-S-07 see D-75 (i) |
| US-020 | INC-1 | done | DoD-S-01 met after D-65 (UI 4/4, D-69); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | done | met (API 5/5, UI 3/3 after D-77; WF-19 at three widths, axe clean); DoD-S-07 see D-76 (i); F-06 fixed (983123a, header part 089a5a8); F-02 fixed (58affcd, D-80); cross-cutting T-36 … T-43, T-53 done (D-82, D-83) |
