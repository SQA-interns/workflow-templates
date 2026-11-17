# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-012
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-012 wait for US-004 and US-016, D-58)
- Notes for the next session:
  - T-20 backend: `RecycleBinService` (move, list, restore; guard + missing `versionNumber` 400) calls package-private `ContactService.moveToRecycleBin` / `restoreFromRecycleBin`; `Contact.moveToRecycleBin`, `restoreFromRecycleBin`, `deletedAt`; Version kinds `MOVED_TO_RECYCLE_BIN`, `RESTORED_FROM_RECYCLE_BIN` (no changed items); `ContactRepository.findInRecycleBin`; `RecycleBinController` (POST `…/contacts/{id}/move-to-recycle-bin`, GET `…/recycle-bin`, POST `…/recycle-bin/{id}/restore`); not in recycle bin → 404.
  - T-21: `domain/RecycleBin` (PERIOD 30 days, `removalAt`, reuse for US-003); `RemovalJob.removeDueContacts` (`@Scheduled` PT15M, `@EnableScheduling`) loops `GroupRepository.findAllIds` → `ContactRepository.permanentlyDeleteDue(groupId, cutoff)`. No Audit entry yet: US-013 (T-23, `AuditService`) must add it to `RemovalJob` (ADR-10, D-58 (f)). Method names starting with `delete…` make SpotBugs flag the repository as mutable (EI_EXPOSE_REP2).
  - Frontend: `api/recycleBin.ts`; `features/contacts/MoveToRecycleBin.tsx` (WF-07); `features/recycle-bin/RecycleBinPage.tsx` (WF-11; US-013 adds "Delete permanently" there); route `recycle-bin`; header link for Owner/Editor.
  - Frozen US-012: API 0/10, UI 0/4 chromium, all stop in `team()` at Invitations (US-004); EX-012-05 also needs Export (US-016). On the stack with SQL Memberships and `deleted_at` shifted by SQL: all pass (D-58). US-007 EX-007-03 and US-011 EX-011-04 no longer need SQL for the recycle bin.
  - No regressions: backend 267 unit green, IT 241 run with the same failures per class (+2 passing repository ITs); frontend 245 tests, lint/types/prettier on `src` (README warning as before); UI acceptance: the same 13 pass; checks and gitleaks clean. Stack still up with `APP_HOST_PORT=8090`.

## Next
- Command: /implement US-013
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
| US-013 | INC-1 | ready | |
| US-014 | INC-1 | in progress | DoD-S-01 open until US-004 (D-52; US-008, US-009, US-012 built); DoD-S-07 see notes; others met |
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until D-51 (approval); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
