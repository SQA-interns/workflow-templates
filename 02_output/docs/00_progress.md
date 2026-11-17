# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-015
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-015 wait for US-004, D-60)
- Notes for the next session:
  - Backend: `ContactQuery(search, sort, dir, page, ContactQuery.Filters)` (as sent, from the controller); `ContactQueryService` checks them into `domain/ContactFilters` (trimmed, lengths, ISO country, Tag ids of the Group or 404) and `ContactQueryRepository.findPage/count(groupId, pattern, ContactFilters, …)` build the filtered query in `filtered(groupId, …)`. US-016 Export: reuse the same check and `filtered` query for its scope.
  - Frontend: `listQuery.ts` holds `filters` (URL and API path); `FilterPanel.tsx`, `FilterChips.tsx`, `FilterBar` in `ContactListPage.tsx`; `useGroupTags` (`api/tags.ts`).
  - Run the whole unit suite (`./mvnw test`, incl. `ArchitectureTest`: AR-03 web never touches `domain`, AR-04 every repository method, even private, takes `groupId`) before each commit; D-60 (f).
  - The stack's backend image copies `backend/target/contacts-backend.jar`: run `./mvnw -DskipTests package` (and `npm run build`) before `docker compose --env-file ../.env up -d --build`.
  - Frozen US-015: API 0/10, UI 0/5 chromium, all stop in `team()` at Invitations (US-004). On the stack with SQL Memberships every step passes (D-60).
  - No regressions: backend 284 unit green, IT 252 run with the same failures per class (+8 passing repository ITs); frontend 262 tests, eslint/tsc/prettier (src) clean; UI acceptance: the same 13 pass; checks and gitleaks clean. Stack still up with `APP_HOST_PORT=8090`.

## Next
- Command: /implement US-016
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
| US-012 | INC-1 | in progress | DoD-S-01 open until US-004, US-016 (D-58); others met |
| US-013 | INC-1 | in progress | DoD-S-01 open until US-004, US-018 (D-59); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | in progress | DoD-S-01 open until US-004 (D-52; US-008, US-009, US-012 built); DoD-S-07 see notes; others met |
| US-015 | INC-1 | in progress | DoD-S-01 open until US-004 (D-60); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until D-51 (approval); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
