# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-008
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-008 wait for US-004, US-010, D-54)
- Notes for the next session:
  - T-15 backend: `ContactValidator.checkAttributes`; `ContactContent` (Fields + Attributes by key A–Z) is what `VersionWriter.record(contact, kind, before, after)` diffs: Attributes are items `attribute:<key>` and sit in the snapshot. `Attribute` entity, `AttributeRepository` (`add`, `discardOfContact`; reads stay in `ContactQueryRepository.findAttributes`). `ContactService.create/update(…, attributes)`: `null` on update keeps the stored ones. US-009 adds `tags` to `ContactContent`, the requests and the snapshot the same way.
  - SpotBugs reports EI_EXPOSE_REP2 for any injected repository that declares a delete/remove/clear method (hence `discardOfContact`).
  - T-15 frontend: `attributes.ts` (browser checks), `AttributesField` (WF-08 section) in `ContactForm` (`initialAttributes`, `onSave(fields, attributes)`); `problemsOfError` now also returns `attributes`; `useCreateContact` takes `{ fields, attributes }`.
  - Frozen US-008: API 0/11, UI 0/12 (4 × 3 browsers), all stop in `team()` at Invitations (US-004); UI AC-008-01/02/04 then read the History page (US-010). Checked on the stack with that data by SQL: all pass (D-54).
  - The stack images copy `backend/target/contacts-backend.jar` and `frontend/dist/`: run `./mvnw -DskipTests package` and `npm run build` before `docker compose --env-file ../.env up -d --build` (stack still on `APP_HOST_PORT=8090`).
  - Commit 2ae5475 has 433 changed lines (limit 400; history not rewritten).
  - No regressions: backend 195 unit green, acceptance per class as before (`implement-us008-backend-full.txt`); frontend 185 tests, lint/types/prettier on `src` (README warning as before); UI acceptance of US-001/002/006/007/014/020 as before; gitleaks clean.

## Next
- Command: /implement US-009
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
| US-008 | INC-1 | in progress | DoD-S-01 open until US-004, US-010 (D-54); DoD-S-07 one commit over 400 lines (2ae5475); others met |
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
