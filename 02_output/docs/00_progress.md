# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-005
- Result: done
- Gate: DoD-S passed (DoD-S-03 Prettier finding in `frontend/README.md` predates this session; DoD-S-07 slip in D-66 (i))
- Notes for the next session:
  - Members: `MembershipService` (list, changeRole, endMembership), `MemberController` (GET/PATCH/DELETE `/groups/{groupId}/members[/{userId}]`), `MembershipRepository.findMembers` (native, `sl-x-icu`); Audit `member.roleChanged|removed`; 400 code `member.roleInvalid` (D-66).
  - Frontend: `features/members/MemberList.tsx` in `MembersPage` above the Invitations; hooks in `api/members.ts`.
  - Frozen US-005: API 9/9, UI 2/2 (Chromium). All suites re-run: backend unit 393/393; API ITs failures 41 → 32, only US-013 EX-013-03 (needs US-018), US-017 … US-019, US-021; UI 71 → 73 pass, failing: US-017 … US-019, US-021, EX-020-02 and AC-012-01 (both wait for the `stubGet` change of D-65). No regressions (`out/logs/implement-us005-*`).
  - Frontend unit 325 tests: `TagsField.test.tsx` (US-009) times out now and then in the full run (D-67, pending review; for `/review INC-1`).
  - US-005 was the last story of INC-1; the `stubGet` change approved in D-65 (`/acceptance-tests`) is still to be applied.
  - Stack on 8090 rebuilt with this code (`APP_HOST_PORT=8090`); gitleaks `v8.30.1` clean.

## Next
- Command: /review INC-1
- Waiting for: nothing (D-66, D-67 pending review; D-65 `stubGet` change by `/acceptance-tests`, then US-012 and US-020 UI suites are re-run)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | done | met (DoD-S-01 met after US-004: API 10/10, UI 5/5) |
| US-003 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 6/6); DoD-S-07 5ef6708 spotless, b07874a … 7b687c6 checkstyle/SpotBugs (fixed in dc2c564, 896ccb9), bdb4dec subject 75 characters; others met |
| US-004 | INC-1 | done | met; DoD-S-07 see D-63 (j) |
| US-005 | INC-1 | done | met (API 9/9, UI 2/2); DoD-S-07 577e900 subject 74 characters (D-66 (i)); others met |
| US-006 | INC-1 | done | DoD-S-01 met after US-004 (API 15/15, UI 5/5); DoD-S-07 see notes of US-006; others met |
| US-007 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 4/4); others met |
| US-008 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 one commit over 400 lines (2ae5475); others met |
| US-009 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); others met |
| US-010 | INC-1 | done | DoD-S-01 met after US-004 (API 8/8, UI 3/3); others met |
| US-011 | INC-1 | done | DoD-S-01 met after US-004 (API 7/7, UI 3/3); DoD-S-07 one subject over 72 characters (24e39f9); others met |
| US-012 | INC-1 | in progress | DoD-S-01 open: API 10/10, UI 3/4, AC-012-01 waits for the approved `stubGet` change (D-64, D-65); others met |
| US-013 | INC-1 | in progress | DoD-S-01 open until US-018 (API 5/6, EX-013-03 needs Merge; UI 2/2); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); DoD-S-07 see notes of US-014; others met |
| US-015 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 5/5); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 b7b36b7 554 lines, 5168f66 and daf3e85 subjects over 72 characters; others met |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until the approved `stubGet` change (D-51, D-65); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
