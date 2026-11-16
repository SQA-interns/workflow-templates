# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-009
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-009 wait for US-004, US-010, D-55)
- Notes for the next session:
  - T-16 backend: `ContactContent(fields, attributes, tags)` (Tag names A–Z) feeds `VersionWriter`: items `tag:<name>`, snapshot `tags`. `Tag`/`TagRepository` (`add`, `findInGroup` case-insensitive, `suggest`), `ContactTag`/`ContactTagRepository` (`add`, `discardOfContact(groupId, contactId, tagIds)`). `TagService.resolve` (existing spelling, else created) and `retag` (puts on added, takes off removed). A Contact's Tags are read with `ContactQueryRepository.findTags`.
  - US-011 (restore) re-creates missing Tags (R-011-04): `TagService.resolve` with the snapshot's names does that.
  - T-17: `GET /groups/{groupId}/tags?q=` (`TagController`, `TagService.suggest`); frontend `api/tags.ts`, `TagsField` (combobox, 300 ms), `tags.ts`; `ContactForm` takes `initialTags`, `onSave(fields, attributes, tags)`; `problemsOfError` returns `tags`.
  - Frozen US-009: API 0/12, UI 0/15 (5 × 3 browsers), all stop in `team()` at Invitations (US-004); UI AC-009-01/04 then read the History page (US-010). Checked on the stack with that data by SQL: all pass (D-55).
  - Stack images copy the backend jar and `frontend/dist/`: `./mvnw -DskipTests package`, `npm run build`, then `APP_HOST_PORT=8090 docker compose --env-file ../.env up -d --build`.
  - No regressions: backend 225 unit green, acceptance per class as before (`implement-us009-backend-full.txt`); frontend 207 tests, lint/types/prettier on `src` (README warning as before); UI acceptance of US-001/002/006/007/014/020 as before; US-008 stack check still passes; gitleaks clean.

## Next
- Command: /implement US-010
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
| US-009 | INC-1 | in progress | DoD-S-01 open until US-004, US-010 (D-55); others met |
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
