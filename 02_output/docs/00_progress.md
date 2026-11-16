# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-007
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-007 wait for US-004, US-010, US-012, D-53)
- Notes for the next session:
  - T-13 backend: `PUT …/contacts/{contactId}` → `ContactService.update` (Role → body → `ContactRepository.findForUpdate` (row lock) → Version number → state → no changes), `Contact.edit`, `ContactVersion.EDITED`, `ConflictException(code, params)` → 409 with `params` in `ProblemHandler`; `ContactUpdateRequest`. Body `attributes`/`tags` are ignored and Version snapshots hold none: US-008/US-009 must add both (`VersionWriter`, D-53 (e)).
  - T-14 frontend: `EditContactPage` (route `contacts/:contactId/edit`), `useUpdateContact` (sends Attributes and Tag names read, unchanged), `ContactForm` gains `initial`, `onReload` (conflict banner + Reload), "No changes to save." beside Save; `useCanChangeContacts` (api/groups) used by the list and WF-07's new Edit.
  - Frozen US-007: API 0/10, UI 0/4, all stop in `team()` at Invitations (US-004); then History (US-010) and move to recycle bin (US-012). Checked on the stack with that data by SQL: all pass (D-53; spec kept in `out/logs/implement-us007-stack-check.spec.ts.txt`).
  - Host port 8080 is taken by another project's stack (`contacts-*`); this stack now runs with `APP_HOST_PORT=8090` (`E2E_BASE_URL=http://localhost:8090`). Ask the human before stopping anything that is not `kontakti-*`.
  - Deleted Users (US-021) have no display name: `changedBy` of BR-06 needs "Deleted user" there (D-53 (c)).
  - No regressions: backend 172 unit green, acceptance per class as before (`implement-us007-backend-full.txt`); frontend 168 tests, lint/types/prettier on `src` (README warning as before); UI acceptance of US-001/002/006/014/020 as before; gitleaks clean. Screens + axe (none): `out/logs/screens/WF-08_contact-form/edit/`.

## Next
- Command: /implement US-008
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | in progress | DoD-S-01 open until US-006, US-014, US-003 (D-46); others met |
| US-003 | INC-1 | ready | |
| US-004 | INC-1 | ready | |
| US-005 | INC-1 | ready | |
| US-006 | INC-1 | in progress | DoD-S-01 open until US-004, US-014, US-010 (D-49); DoD-S-07 see notes; others met |
| US-007 | INC-1 | in progress | DoD-S-01 open until US-004, US-010, US-012 (D-53); others met |
| US-008 | INC-1 | ready | |
| US-009 | INC-1 | ready | |
| US-010 | INC-1 | ready | |
| US-011 | INC-1 | ready | |
| US-012 | INC-1 | ready | |
| US-013 | INC-1 | ready | |
| US-014 | INC-1 | in progress | DoD-S-01 open until US-004, US-008, US-009, US-012 (D-52); DoD-S-07 see notes; others met |
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until US-010 (D-44) and D-51 (approval); D-48 fixed; others met |
| US-021 | INC-2 | ready | |
