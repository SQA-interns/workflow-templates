# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-013
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-013 wait for US-004 and US-018, D-59)
- Notes for the next session:
  - T-23 backend: `domain/AuditEntry` (actions `contact.permanentlyDeleted`, `contact.removed`; add a constant per BR-10 action), `AuditEntryRepository` (add only, SR-04), `AuditService.record(groupId, userId, action, targetId)` at the clock's UTC time, in the caller's transaction: reuse it for US-003, US-004, US-005, US-016, US-018, US-019.
  - `PermanentDeleteService.permanentlyDelete` (Owner; 404; active or merged → 409 `contact.notInRecycleBin`; `ContactRepository.permanentlyDelete` with the DB cascade) and `permanentlyDeleteDue(groupId, cutoff)` (Audit `contact.removed` by the User who moved it, then bulk delete); `RemovalJob` now calls the latter. `DELETE /groups/{id}/recycle-bin/{contactId}` → 204. Merged Contacts (D-21) are T-47.
  - SpotBugs EI_EXPOSE_REP2: service or repository methods starting with `remove…`/`delete…` make every class that stores the bean fail the check; run the check before committing.
  - Frontend: `useIsOwner` (`api/groups.ts`), `usePermanentlyDelete` (`api/recycleBin.ts`), `features/recycle-bin/DeletePermanently.tsx` (WF-11, Owner only). The stack's frontend image copies `frontend/dist/`: run `npm run build` before `docker compose --env-file ../.env up -d --build`.
  - Frozen US-013: API 0/6, UI 0/2 chromium, all stop in `team()` at Invitations (US-004); EX-013-03 also needs Merge (US-018). On the stack with SQL Memberships: all other steps pass (D-59).
  - No regressions: backend 274 unit green, IT 244 run with the same failures per class (+3 passing repository ITs); frontend 249 tests, prettier/eslint/tsc clean; UI acceptance: the same 13 pass; checks and gitleaks clean. Stack still up with `APP_HOST_PORT=8090`.

## Next
- Command: /implement US-015
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
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until D-51 (approval); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
