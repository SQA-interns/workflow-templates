# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-002
- Result: done
- Gate: DoD-S not passed (DoD-S-01: frozen tests of US-002 wait for US-006, US-014, US-003, D-46)
- Notes for the next session:
  - T-07 backend: `Group`, `Membership`, `Role`, `GroupRepository.findActiveGroupsOf` (sl-x-icu), `MembershipRepository.findActiveRole`, `AccessGuard.require(groupId, userId, Permission)` with the ADR-04 table (`Permission`), `GroupService`, `GroupController`. Use `AccessGuard` first in every Group-owned service method.
  - T-08 frontend: `api/groups.ts` (`useGroups`, `useCreateGroup`), `features/groups/{CreateGroupPage,GroupSwitcher,GroupStart,GroupRoute,currentGroup}`; `/groups/:groupId/*` routes nest under `GroupRoute` (non-Member → not found, remembers the Group). `/groups/:groupId/contacts` shows `ContactListPlaceholder` (`app/pages.tsx`): US-014 replaces it with WF-06 (D-46).
  - Frozen API US-002 7/10, UI 2/5; the rest fail only at Contacts (US-006), the Contact list (US-014) or the Group recycle bin (US-003). Re-run both US-002 suites there; set US-002 done when they pass.
  - US-001 now passes all frozen tests (API 9/9, UI 6/6): done. US-020 UI 1/4: AC-020-02 also needs the user menu to close after choosing a language (D-48, a `fix:` in US-020's scope).
  - Full backend run: no regressions (`out/logs/implement-us002-backend-full.txt`; Us004 ac00406 now errors later, at Invitations); backend check, frontend lint/types/prettier on `src`, 89 unit tests, gitleaks clean. Screens + axe: `out/logs/screens/WF-04_create-group/`, `WF-06_contact-list/`, `WF-05_no-groups/groups-error/`.
  - Commit `ec5a95d` lacks the blank line after its subject (subject reads over 72 characters); not rewritten (commits.md).
  - Stack: `npm run build`, then `APP_HOST_PORT=8180 docker compose -p kontakti --env-file ../.env up -d --build`; UI tests with `E2E_BASE_URL=http://localhost:8180`.

## Next
- Command: /implement US-006
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | in progress | DoD-S-01 open until US-006, US-014, US-003 (D-46); others met |
| US-003 | INC-1 | ready | |
| US-004 | INC-1 | ready | |
| US-005 | INC-1 | ready | |
| US-006 | INC-1 | ready | |
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
