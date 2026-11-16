# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-001
- Result: blocked
- Gate: DoD-S not passed (DoD-S-01 frozen tests, DoD-S-03 spotbugs:check)
- Notes for the next session:
  - T-01 … T-05 built test-first (commits `f4a078b` … `4c88016`): token filter, User provisioning, e-mail gate, deleted User 401, rate limit, ProblemHandler, apiClient, WF-01/02/03/05, user menu with Sign out, routes. Unit tests green (backend 38, frontend 57).
  - Frozen US-001 now passing: API AC-001-05, EX-001-03 (+ US-020 AC-020-01 by GET /me); UI AC-001-02. The rest fail only at `POST/GET /groups` (US-002) or `PATCH /me` (US-020), D-39, and on the e-mail locator, D-40.
  - D-40 (approval): frozen `getByRole("textbox")` also matches the password field in Playwright; blocks almost every UI test. D-41 (approval): 3 SpotBugs Low findings need exclusions; adding them was refused by the tool's permission check.
  - Flows checked by script on the stack: AC-001-01 (until PATCH /me), AC-001-05, AC-001-06, EX-001-01, EX-001-05 pass with an `input[type="email"]` locator; screenshots and axe (0 violations) in `out/logs/screens/WF-0*`.
  - Fixed on the way: CSP `frame-src` now includes `AUTH_EMULATOR_URL` (Google popup with the emulator), Slovenian Google button keeps the word "Google".
  - Not drawn yet: WF-05 illustration placeholder (no asset before `/design`); the skeleton nav link stays until US-002.
  - Commit `ffa2f0f` has 16 files / 584 lines, above the 400-line limit (history not rewritten).
  - The `kontakti` stack runs on 8180/9199 with this session's build.

## Next
- Command: /implement US-001 D-40: 1, D-41: 1
- Waiting for: D-40, D-41

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | in progress | DoD-S-01, -03 open (D-39, D-40, D-41) |
| US-002 | INC-1 | ready | |
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
| US-020 | INC-1 | ready | |
| US-021 | INC-2 | ready | |
