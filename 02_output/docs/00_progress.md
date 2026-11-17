# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-004
- Result: done
- Gate: DoD-S passed (DoD-S-03 Prettier finding in `frontend/README.md` predates this session; DoD-S-07 slips in D-63 (j))
- Notes for the next session:
  - Invitations: `InvitationService` (create, listOpen, revoke, view, accept), `InvitationToken` (Group id + 128 random bits, SHA-256 stored, D-63 (a)), `InvitationRepository`, `InvitationController`; `ForbiddenException(code)`; Audit `invitation.created|revoked|accepted`.
  - Frontend: `features/members/{MembersPage,InvitationsSection}.tsx` (WF-13 shell; US-005 adds the Member list to `MembersPage`), `features/invitations/{InvitationPage,InvitationLayout}.tsx`, `auth/returnTo.ts`, hooks in `api/invitations.ts`; Members nav for the Owner.
  - Frozen US-004: API 14/14, UI 8/8 (Chromium). Re-run of all suites: API failures 176 → 41, only US-005, US-013 EX-013-03 (needs US-018), US-017 … US-019, US-021 fail; UI 14 → 71 pass, failing: US-005, US-017 … US-019, US-021, EX-020-02 (D-51), US-012 AC-012-01 (D-64, same `stubGet` defect). No regressions (`out/logs/implement-us004-*`).
  - Run the check command before each commit and gate the commit on it (`check && git commit`); D-63 (j) lists this session's slips. `npm run check` fails on `frontend/README.md` Prettier (not in /implement's Writes).
  - Stack on 8090 rebuilt with this code; gitleaks via `zricethezav/gitleaks:v8.30.1` clean.

## Next
- Command: /implement US-005
- Waiting for: D-51, D-64 (approval of the frozen `stubGet` helper; not blocking US-005)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | done | met (DoD-S-01 met after US-004: API 10/10, UI 5/5) |
| US-003 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 6/6); DoD-S-07 5ef6708 spotless, b07874a … 7b687c6 checkstyle/SpotBugs (fixed in dc2c564, 896ccb9), bdb4dec subject 75 characters; others met |
| US-004 | INC-1 | done | met; DoD-S-07 see D-63 (j) |
| US-005 | INC-1 | ready | |
| US-006 | INC-1 | done | DoD-S-01 met after US-004 (API 15/15, UI 5/5); DoD-S-07 see notes of US-006; others met |
| US-007 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 4/4); others met |
| US-008 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 one commit over 400 lines (2ae5475); others met |
| US-009 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); others met |
| US-010 | INC-1 | done | DoD-S-01 met after US-004 (API 8/8, UI 3/3); others met |
| US-011 | INC-1 | done | DoD-S-01 met after US-004 (API 7/7, UI 3/3); DoD-S-07 one subject over 72 characters (24e39f9); others met |
| US-012 | INC-1 | in progress | DoD-S-01 open: API 10/10, UI 3/4, AC-012-01 details waits for D-64 (frozen `stubGet`); others met |
| US-013 | INC-1 | in progress | DoD-S-01 open until US-018 (API 5/6, EX-013-03 needs Merge; UI 2/2); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); DoD-S-07 see notes of US-014; others met |
| US-015 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 5/5); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 b7b36b7 554 lines, 5168f66 and daf3e85 subjects over 72 characters; others met |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until D-51 (approval); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
