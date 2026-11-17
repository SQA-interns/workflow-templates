# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-011
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-011 wait for US-004 and US-012, D-57)
- Notes for the next session:
  - T-19 backend: `HistoryService.restoreVersion` (CHANGE_CONTACT, Version n → 404, missing `versionNumber` → 400) calls package-private `ContactService.restore` (lock, BR-06, active, `version.sameAsCurrent`, `TagService.resolve` re-creates Tags); `VersionWriter.recordRestored`; `ContactVersion.RESTORED`, `restored(…)`, `restoredFrom` mapped; `restoredFrom` in `VersionSummaryView`/`VersionView`; `HistoryController` POST `…/versions/{number}/restore` with `VersionNumberRequest`.
  - `ContactService.contentOf`/`change` are shared by update and restore; US-012/US-018/US-019 can reuse them for their Versions.
  - Frontend: `api/history.ts` `useRestoreVersion`; `components/ui/alert-dialog.tsx` (shadcn AlertDialog on `radix-ui`; style Radix Trigger/Cancel with `buttonVariants`, since `Button` has no forwardRef under React 18); WF-10 button and dialog, WF-09 chip "restored · from Version n".
  - Frozen US-011: API 0/7, UI 0/3 chromium, all stop in `team()` at Invitations (US-004); EX-011-04 also needs the recycle bin API (US-012). On the stack with SQL Memberships and SQL recycle-bin state: all pass (D-57).
  - One commit subject has 73 characters (24e39f9, DoD-S-07); 7 unit tests were renamed for Checkstyle after their `feat` commits (218f540 maps old → new names).
  - No regressions: backend 247 unit green, IT 239 run with the same failures per class (+1 passing repository IT); frontend 231 tests, lint/types/prettier on `src` (README warning as before); UI acceptance: the same 13 pass; checks and gitleaks clean. Stack: `APP_HOST_PORT=8090` as before.

## Next
- Command: /implement US-012
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
| US-010 | INC-1 | in progress | DoD-S-01 open until US-004 (D-56; US-011 built); others met |
| US-011 | INC-1 | in progress | DoD-S-01 open until US-004, US-012 (D-57); DoD-S-07 one subject over 72 characters (24e39f9); others met |
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
