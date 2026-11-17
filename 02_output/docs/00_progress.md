# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-016
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-016 wait for US-004, D-61)
- Notes for the next session:
  - Export: `POST /groups/{groupId}/exports` → `ExportController` → `ExportService` (EXPORT; `ContactQueryService.scope` checks the list query once, shared with the Page; `ContactQueryRepository.findExport/countExport(groupId, pattern, filters, contactIds, …)`; `ExportWorkbook` SXSSF, SR-07; Audit `contacts.exported`, target the Group).
  - Frontend: `api/apiClient.ts` `apiDownload`, `api/exports.ts` `useExport`; `features/export/ExportButtons.tsx`; `features/contacts/useSelection.ts`; `ContactTable` takes `selection` (checkbox column first).
  - Run `./mvnw test` (incl. `ArchitectureTest`, AR-04 covers private repository methods) before each commit; D-61 (f) lists this session's commit-rule slips.
  - Backend image copies `backend/target/contacts-backend.jar`: `./mvnw -DskipTests package` and `npm run build` before `APP_HOST_PORT=8090 docker compose --env-file ../.env up -d --build`; gitleaks runs via the `zricethezav/gitleaks:v8.30.1` image.
  - Frozen US-016: API 1/11 (EX-016-05), UI 0/4 chromium; the rest stop in `team()` at Invitations (US-004). With SQL Memberships every step passes (D-61).
  - No regressions: backend 309 unit green, IT 255 run, failures per class unchanged or fewer; frontend 281 tests (TagsField timing flake, D-61 (g)), eslint/tsc clean; UI acceptance: the same 13 pass; gitleaks clean. Stack still up on 8090.

## Next
- Command: /implement US-003
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | in progress | DoD-S-01 open until US-006, US-014, US-003 (D-46); others met |
| US-003 | INC-1 | ready | |
| US-004 | INC-1 | ready | |
| US-005 | INC-1 | ready | |
| US-006 | INC-1 | in progress | DoD-S-01 open until US-004, US-014 (D-49; US-010 built); DoD-S-07 see notes; others met |
| US-007 | INC-1 | in progress | DoD-S-01 open until US-004 (D-53; US-010, US-012 built); others met |
| US-008 | INC-1 | in progress | DoD-S-01 open until US-004 (D-54; US-010 built); DoD-S-07 one commit over 400 lines (2ae5475); others met |
| US-009 | INC-1 | in progress | DoD-S-01 open until US-004 (D-55; US-010 built); others met |
| US-010 | INC-1 | in progress | DoD-S-01 open until US-004 (D-56; US-011 built); others met |
| US-011 | INC-1 | in progress | DoD-S-01 open until US-004 (D-57; US-012 built); DoD-S-07 one subject over 72 characters (24e39f9); others met |
| US-012 | INC-1 | in progress | DoD-S-01 open until US-004 (D-58; US-016 built); others met |
| US-013 | INC-1 | in progress | DoD-S-01 open until US-004, US-018 (D-59); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | in progress | DoD-S-01 open until US-004 (D-52; US-008, US-009, US-012 built); DoD-S-07 see notes; others met |
| US-015 | INC-1 | in progress | DoD-S-01 open until US-004 (D-60); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | in progress | DoD-S-01 open until US-004 (D-61); DoD-S-07 b7b36b7 554 lines, 5168f66 and daf3e85 subjects over 72 characters; others met |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until D-51 (approval); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
