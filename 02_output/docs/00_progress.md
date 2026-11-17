# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-003
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-003 wait for US-004, D-62)
- Notes for the next session:
  - Group lifecycle: `GroupService.rename` (PATCH /groups/{id}), `moveToRecycleBin`, `listDeletedGroups`, `restore` (`DeletedGroupController`, /deleted-groups); `GroupRepository.findForUpdate`, `findInRecycleBinOwnedBy`, `permanentlyDeleteDue`; `RemovalJob.removeDueGroups`; Audit `group.movedToRecycleBin`, `group.restored`.
  - US-004 must answer 409 `invitation.noLongerValid` for an Invitation of a Group in the recycle bin (EX-003-05, view and accept), then re-run US-003 … US-016 suites.
  - Frontend: `features/groups/{GroupSettingsPage,MoveGroupToRecycleBin,DeletedGroupsPage}.tsx`, `groupName.ts`; hooks in `api/groups.ts`; Settings nav (Owner), Deleted Groups in the user menu.
  - Run `./mvnw test` and the check command (`spotless`, `checkstyle`, `spotbugs`) before each commit; D-62 (f) lists this session's slips. SpotBugs reads a repository method starting with `delete` as mutating (EI_EXPOSE_REP2).
  - Stack: `./mvnw -DskipTests package`, `npm run build`, then `APP_HOST_PORT=8090 docker compose --env-file ../.env up -d --build`; gitleaks via `zricethezav/gitleaks:v8.30.1`.
  - Frozen US-003: API 0/10, UI 0/6 (all stop in `team()` at Invitations); with SQL Memberships 8/10 API and 5/6 UI pass, the rest need `invite()` (D-62).
  - No regressions: backend 325 unit green, IT 257 run, failures per class unchanged or fewer (`Us002GroupsAcceptanceIT` now 10/10); frontend 294 tests, eslint/tsc/prettier clean; UI acceptance 14 pass (13 + EX-002-06); gitleaks clean. Stack still up on 8090.

## Next
- Command: /implement US-004
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | in progress | DoD-S-01 open until US-006, US-014, US-003 (D-46); others met |
| US-003 | INC-1 | in progress | DoD-S-01 open until US-004 (D-62); DoD-S-07 5ef6708 spotless, b07874a … 7b687c6 checkstyle/SpotBugs (fixed in dc2c564, 896ccb9), bdb4dec subject 75 characters; others met |
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
