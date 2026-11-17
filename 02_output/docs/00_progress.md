# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-010
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-010 wait for US-004, D-56)
- Notes for the next session:
  - T-18 backend: `ContactVersionRepository.findHistory`/`findVersion` (with groupId); `HistoryService.history`/`version` (READ; empty History or missing Version → 404); `HistoryController` GET `…/versions`, `…/versions/{number}`; views `VersionSummaryView`, `VersionView`, `ChangeView`, `SnapshotView` (also used by `VersionWriter`). `ProblemHandler` maps an unsupported method to 405 `methodNotAllowed`.
  - US-011 adds `restoredFrom` (column `restored_from` not mapped in `ContactVersion` yet), "Restore this Version" on `VersionPage` and the chip "restored · from Version n" on `HistoryPage` (WF-09/WF-10).
  - Frontend: `api/history.ts` (`useHistory`, `useVersion`), `features/history/{HistoryPage,VersionPage,changes}.ts(x)`; WF-07 sections moved to `features/contacts/ContactContent.tsx` (`FieldsSection`, `AttributesSection`, `TagsSection`).
  - Frozen US-010: API 0/8, UI 0/3 chromium, all stop in `team()` at Invitations (US-004); checked on the stack with SQL Memberships: all pass (D-56). US-020 AC-020-03 now passes; US-020 waits only for D-51.
  - `TagsField.test.tsx` (US-009) tests take ~5 s each and time out when the host is loaded (seen while Maven ran in parallel); alone and in a quiet full run all 225 frontend tests pass. Run the frontend suite without other load; worth a look in `/review`.
  - No regressions: backend 234 unit green, acceptance per class as before; frontend 225 tests, lint/types/prettier on `src` (README warning as before); UI acceptance of US-001/002/006/007/014/020 as before plus AC-020-03; gitleaks clean. Stack: `APP_HOST_PORT=8090` as before.

## Next
- Command: /implement US-011
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
| US-007 | INC-1 | in progress | DoD-S-01 open until US-004, US-012 (D-53; US-010 built); others met |
| US-008 | INC-1 | in progress | DoD-S-01 open until US-004 (D-54; US-010 built); DoD-S-07 one commit over 400 lines (2ae5475); others met |
| US-009 | INC-1 | in progress | DoD-S-01 open until US-004 (D-55; US-010 built); others met |
| US-010 | INC-1 | in progress | DoD-S-01 open until US-004 (D-56); others met |
| US-011 | INC-1 | ready | |
| US-012 | INC-1 | ready | |
| US-013 | INC-1 | ready | |
| US-014 | INC-1 | in progress | DoD-S-01 open until US-004, US-008, US-009, US-012 (D-52); DoD-S-07 see notes; others met |
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until D-51 (approval); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
