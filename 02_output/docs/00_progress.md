# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S4 /acceptance-tests
- Result: done
- Gate: phase 3 passed
- Notes for the next session:
  - Frozen at `983e512`: 199 API acceptance tests (`backend/src/test/java/app/contacts/acceptance/`), 89 UI acceptance tests (`frontend/acceptance/`), 8 e2e tests (`frontend/e2e/`); every AC/EX covered, all fail for missing behaviour. Commands and first run: `docs/03_test-strategy.md`.
  - Read the table "What the frozen tests rely on" in `docs/03_test-strategy.md` first: `FIREBASE_AUTH_EMULATOR_HOST` arrives as a Spring property, the `Clock` bean must be replaceable (`@Primary` test clock), BR-08 runs from a `@Scheduled` method, §5 codes, UI roles and labels.
  - Browser tests need the `kontakti` stack: `APP_HOST_PORT=8180 EMULATOR_HOST_PORT=9199 bash scripts/up.sh`, then `E2E_BASE_URL=http://localhost:8180 E2E_EMULATOR_URL=http://localhost:9199 npx playwright test --project=chromium`. The stack is still running.
  - Hidden suite (123 cases, every R and BR) is in `02_output/hidden-tests/` (git-ignored) for the runner's `--vault`; only `docs/03_hidden-manifest.sha256` is committed.
  - New non-blocking decisions D-36 (signature check vs emulator), D-37 (Slovenian wording, E2E-01 step order), D-38 (test settings) await review.
  - `ApplicationIT` starts a second Spring context without the emulator; the Firebase default app is JVM-global, so the implementation must allow both contexts.
  - `npm run check` fails on the pre-existing formatting of `frontend/README.md` (not a test file).
  - Commits `ac8f959` (458 lines) and `3ef4001` (575 lines) exceed the 400-line limit of `general/rules/commits.md`; history is not rewritten.

## Next
- Command: /implement US-001
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | ready | |
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
