# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-014
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-014 wait for US-004, US-008, US-009, US-012, D-52; DoD-S-07: see notes)
- Notes for the next session:
  - T-11 backend: `ContactQueryRepository` (native SQL, constant ORDER BY per `ContactSort`; `findPage`, `count`, `findTags`, `findAttributes`), `ContactQueryService.page` + `ContactQuery`, views `ContactPageView`/`ContactListItemView`/`TagView`/`AttributeView`; `GET …/contacts` (answers the service views), `GET …/contacts/{contactId}` (`ContactService.get`, any state, with Attributes and Tags). `ProblemHandler`: a mistyped query parameter is 400, a mistyped path id stays 404.
  - T-12 frontend: `features/contacts/{listQuery,ContactTable,ContactListControls,ContactListPage,ContactDetailsPage}`, `api/contacts.ts` (`useContactList`, `useContact`), `api/retry.ts` (4xx never retried, set app-wide in `main.tsx`), `components/ui/badge.tsx`; routes `contacts` (replaces the D-46 placeholder) and `contacts/:contactId`. WF-07 has no Edit / Move to recycle bin yet (US-007, US-012); its History link leads to WF-09 (US-010).
  - Frozen US-014: API 0/12, UI 0/5, all stop in `team()` at Invitations (US-004), then need the recycle bin (US-012) and Tags/Attributes saved (US-008/009). Checked on the stack with that data set by SQL (D-52). Re-run US-002, US-006, US-014 and US-020 suites in later stories.
  - Other stories now: US-002 API 9/10, UI 4/5 (EX-002-06 needs US-003); US-020 UI 2/4: AC-020-02 passes after the D-48 fix (7395f5b), AC-020-03 needs US-010, EX-020-02 cannot pass as frozen (D-51, approval asked). US-001 6/6.
  - No regressions: backend 157 unit/IT green; acceptance per class as before except US-002 better (`implement-us014-backend-full.txt`); frontend 159 tests, lint/types/prettier on `src` (README warning as before); gitleaks clean. Screens + axe (none): `out/logs/screens/WF-06_contact-list/`, `WF-07_contact-details/`.
  - The stack enforces 100 requests/min (SR-03): bulk data for checks goes through `bulkContacts` (SQL).
  - DoD-S-07 deviation, not rewritten: c144848 was committed with two unit tests red (check piped through grep); fixed in 367cec7.

## Next
- Command: /implement US-007
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
| US-014 | INC-1 | in progress | DoD-S-01 open until US-004, US-008, US-009, US-012 (D-52); DoD-S-07 see notes; others met |
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until US-010 (D-44) and D-51 (approval); D-48 fixed; others met |
| US-021 | INC-2 | ready | |
