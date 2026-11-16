# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-006
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-006 wait for US-004, US-014, US-010, D-49; DoD-S-07: see notes)
- Notes for the next session:
  - T-09 backend: `ContactFields` (embedded record, `byName()`, `name()`), `Contact`, `ContactVersion`; Group-owned repositories extend `Repository` + `GroupOwnedStore.add(groupId, item)` (no generic save, AR-04); `ContactValidator`, `Normaliser` (search text space-joined as seed.sql), `VersionWriter.record(contact, kind, before)` (D-50 j), `ContactService.create`/`findByEmail`, `ContactController` (`POST …/contacts`, `GET …/contacts/email-matches`; `excludeContactId` is for US-007). `ContactResponse` returns empty attributes/tags/mergedFrom until US-008/009/018.
  - T-10 frontend: `api/contacts.ts` (`useCreateContact`, `useEmailMatches`), `features/contacts/{contactFields,fieldMessages,ContactFieldInput,ContactForm,NewContactPage}`; route `/groups/:groupId/contacts/new`; saving opens the Contact list (D-50 f). US-014 adds the "Add a Contact" action on WF-06; US-007 reuses `ContactForm` for edit.
  - Frozen US-006: API 0/15, UI 0/5, all stop in `team()` at Invitations (US-004) and then need the list, details and History (D-49). Checked instead on the stack with Roles set by SQL: `out/logs/implement-us006-api-check.txt`, `implement-us006-ui-check.txt`. Re-run both US-006 suites in /implement US-014, US-010, US-004; set US-006 done when they pass. US-002 UI AC-002-02 now gets past adding Contacts and stops at the list (US-014).
  - No regressions: backend acceptance per class identical to US-002's run, 137 unit/IT green (`implement-us006-backend-full.txt`); frontend 128 tests, lint/types/prettier on `src`; `prettier --check .` warns on `frontend/README.md` (untouched, as before); gitleaks clean. Screens + axe (no violation): `out/logs/screens/WF-08_contact-form/`.
  - DoD-S-07 deviations, not rewritten (commits.md): 596b51c has 418 lines (> 400) and its Red lines for 7 guard tests (accepted values) are wrong: those passed against the stub; 8bc5f54 left a checkstyle warning, fixed in 81af9e9. 47295a9 tests were written after its code (stated in the commit).

## Next
- Command: /implement US-014
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
| US-007 | INC-1 | ready | |
| US-008 | INC-1 | ready | |
| US-009 | INC-1 | ready | |
| US-010 | INC-1 | ready | |
| US-011 | INC-1 | ready | |
| US-012 | INC-1 | ready | |
| US-013 | INC-1 | ready | |
| US-014 | INC-1 | ready | |
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until US-014, US-010 (D-44) and D-48; others met |
| US-021 | INC-2 | ready | |
