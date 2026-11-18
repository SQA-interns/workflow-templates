# Test strategy (as executed)

> For: agent (writes), human (reads) · Written in: phase 3 (`/acceptance-tests`), phase 5 (`/quality-gate`) · Source: `general/testing/writing-tests.md`, `general/testing/measurement.md`, `project/02_design/quality-requirements.md` · Agent: writes

## Test levels and harness

| Level | Location (frozen) | Runs against | Command |
|---|---|---|---|
| Acceptance, API | `backend/src/test/java/app/contacts/acceptance/` (`Us0nn…AcceptanceIT`, 21 classes, helpers in `acceptance/support/`) | the backend on a random port (`@SpringBootTest`), PostgreSQL `postgres:16.15` and the Firebase Auth emulator (image built from `docker/firebase-emulator`) through Testcontainers | `cd backend && ./mvnw -B verify -Dit.test='app/contacts/acceptance/**/*IT'` |
| Acceptance, UI | `frontend/acceptance/` (`us0nn-*.spec.ts`, helpers in `acceptance/support/`) | the local stack (`docker-compose.yml`, project `kontakti`) and a browser (Playwright) | `cd frontend && E2E_BASE_URL=http://localhost:8180 E2E_EMULATOR_URL=http://localhost:9199 npx playwright test acceptance` |
| End-to-end | `frontend/e2e/` (`e2e-0n-*.spec.ts`, one per scenario E2E-01 … E2E-08) | the local stack and a browser | `… npx playwright test e2e` |

Harness rules (D-36, D-37, D-38):

- Every API test starts from an empty database (all tables truncated) and no emulator accounts; every browser test empties the local stack the same way (`docker exec kontakti-postgres-1 psql`, emulator REST), so browser tests run one at a time (`workers: 1`).
- People are emulator accounts (password `Kontakti#2026`, e-mail verified unless the test is about verification); Members join only through Invitations (R-004-01, R-004-04).
- Time: the backend's `java.time.Clock` bean (ADR-10) is replaced by a settable clock marked `@Primary`; the scheduler is simulated by invoking every `@Scheduled` method of the backend's beans. Browser tests cannot set the stack's clock: the AC examples with fixed times (AC-003-03, AC-004-01, AC-010-01, AC-012-01, AC-020-03, EX-010-01, EX-020-02) stub that one GET answer in the contract's shape; everything else uses the real stack.
- Audit entries (no UI or API, D-19) are read from `audit_entry` (D-26); erasure (SR-06, BR-08) is checked with row counts per `contact_id`/`group_id` of the persistence contract.
- 10,000/10,001 Contacts (AC-016-03, EX-016-01) are inserted through the persistence contract as `seed/seed.sql` does; `RATE_LIMIT_PER_MINUTE=100000` in the backend tests (D-38).
- UI tests switch each User to English through `PATCH /api/v1/me`; WF-01/WF-02/WF-03 are used without their text (D-37). The emulator's own Google page is driven by its element ids (third-party UI).
- Browsers: Chromium here; Firefox and WebKit are configured and run where installed (D-38).

What the frozen tests rely on beyond the wording of the stories (the implementation must provide it; changing a test needs a decision):

| Point | Source |
|---|---|
| `FIREBASE_AUTH_EMULATOR_HOST` is passed as a Spring property (`app.firebase.auth-emulator-host`), not only as a process variable | ADR-05, `application.yml` |
| Error answers carry the §5 code either as `code` or in `errors[].code`; a stale Version answer has `params.changedBy` | specification §5 |
| `GET` of a Contact that is merged returns it with `state` and `mergedInto` (AC-018-02, E2E-04); its History is readable | openapi `Contact` |
| `DELETE /api/v1/me` with a non-matching e-mail answers 400 (EX-021-01) | openapi `deleteMyAccount` |
| A merge with a Contact in the recycle bin answers 409 `merge.notPossible` (EX-018-03) | specification §5 |
| `changedBy` of a deleted User's Version is the catalog key `user.deleted` | openapi `VersionSummary` |
| UI: Group switcher labelled "Group switcher", user menu button named by the display name, confirmation dialogs with role `dialog`/`alertdialog`, rows of tables with role `row`, the History change line "<Field label>: old → new" and the AC wordings "(empty)", "(removed)", "tag:<name> added/removed" | WF-06, WF-09, AC-007-01, AC-008-01, AC-008-04, AC-009-01, AC-009-04 |
| UI: select-like controls are a native `select` or a combobox/listbox with options; the per-Field choice on the Merge screen is a radio button named by the value (AC-018-05) | WF-06, WF-13, WF-18 |

## Measurement exclusions

<!-- Before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, style and token files, …). Rules: `general/testing/measurement.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/app/contacts/ContactsApplication.java` | application entry point (`main` only) |
| `backend/src/main/java/app/contacts/**/package-info.java` | package documentation, no code |
| `backend/src/main/resources/**` | configuration and Flyway migrations (SQL), no Java logic; the schema is checked by `ApplicationIT` |
| `frontend/src/main.tsx` | application entry point (mounts the app) |
| `frontend/src/components/ui/**` | generated by the shadcn CLI (UI-01) |
| `frontend/src/styles/**` | style and design-token files (presentation, no logic) |
| `frontend/src/i18n/*.json` | message catalogs (static text data, UI-30); the catalog code in `src/i18n/*.ts` stays measured |
| `frontend/src/test/**` | test setup, not production code |

## Hidden suite

<!-- Only the run command and the number of hidden tests per story; never the cases. -->

| Story | Hidden tests |
|---|---|
| US-001 | 7 |
| US-002 | 5 |
| US-003 | 6 |
| US-004 | 9 |
| US-005 | 5 |
| US-006 | 7 |
| US-007 | 4 |
| US-008 | 6 |
| US-009 | 6 |
| US-010 | 3 |
| US-011 | 4 |
| US-012 | 5 |
| US-013 | 4 |
| US-014 | 6 |
| US-015 | 7 |
| US-016 | 7 |
| US-017 | 5 |
| US-018 | 7 |
| US-019 | 3 |
| US-020 | 3 |
| US-021 | 3 |
| Shared rules (BR-01 … BR-11) | 11 |
| Total | 123 |

Run command: `bash 02_output/hidden-tests/run.sh` from the project root (`hidden.json`); first run 2026-11-16: 123 run, 123 failed, every failure behavioural (the API answers 401: authentication not built). Manifest: `docs/03_hidden-manifest.sha256`.

## First run of the frozen tests (before any implementation)

Run 2026-11-16 on the skeleton (no behaviour built): API tests with `./mvnw verify`, browser tests on Chromium against the local stack at `http://localhost:8180` (D-32). Every test fails because the behaviour is missing.

| Story | Tests | Failed | Reason (one line per group) |
|---|---|---|---|
| US-001 | 9 API + 6 UI | 15 | API: GET /me 401 (authentication not built); UI: no sign-up form, no Google button, API setup 401 |
| US-002 | 10 API + 5 UI | 15 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-003 | 10 API + 6 UI | 16 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-004 | 14 API + 8 UI | 22 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-005 | 9 API + 2 UI | 11 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-006 | 15 API + 5 UI | 20 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-007 | 10 API + 4 UI | 14 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-008 | 11 API + 4 UI | 15 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-009 | 12 API + 5 UI | 17 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-010 | 8 API + 3 UI | 11 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-011 | 7 API + 3 UI | 10 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-012 | 10 API + 4 UI | 14 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-013 | 6 API + 2 UI | 8 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-014 | 12 API + 5 UI | 17 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-015 | 10 API + 5 UI | 15 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-016 | 11 API + 4 UI | 15 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-017 | 12 API + 4 UI | 16 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-018 | 10 API + 4 UI | 14 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-019 | 5 API + 3 UI | 8 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-020 | 3 API + 4 UI | 7 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| US-021 | 5 API + 3 UI | 8 | API: setup `GET /me` answers 401 (authentication not built); UI: setup through the API answers 401 |
| E2E-01 … E2E-08 | 8 | 8 | E2E-01: no sign-up form on `/sign-up`; E2E-02 … E2E-08: setup through the API answers 401 |
| Total | 199 API + 89 UI + 8 e2e = 296 | 296 | no failure from the build or the harness: both containers start, the emulator answers, the stack is up; logs `out/logs/acceptance-tests-backend-first-run.txt`, `out/logs/acceptance-tests-browser-first-run.txt` |

## Approved changes to frozen tests

| Decision | Change | Commits | Re-run |
|---|---|---|---|
| D-65 (D-51, D-64 option 1) | `frontend/acceptance/support/stack.ts` `stubGet` fulfils only GET requests of resource type `fetch` or `xhr`; a page navigation with the same path goes to the app | be08cb8 (helper), 7d602b3 (manifest re-hashed, alone) | Chromium, every spec that calls `stubGet` (US-003, US-004, US-010, US-012, US-020): 25/25 pass, AC-012-01 and EX-020-02 included (`out/logs/acceptance-tests-d65-ui.txt`) |

## Test measures (phase 5)

<!-- Level rows: tests passed / failed. unit = unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all-levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (raw / adjusted, scope) | Tools |
|---|---|---|---|---|---|---|
| backend | unit (unit 466, architecture 10; surefire) | 476 / 0 | 92.54 % (1,762/1,904) | 86.73 % (438/505) | | JUnit 6, JaCoCo 0.8.13 (`target/site/jacoco-unit`) |
| backend | non-unit (integration 77, API acceptance 199; failsafe, Testcontainers) | 276 / 0 | 95.75 % (1,823/1,904) | 79.01 % (399/505) | | JUnit 6, JaCoCo 0.8.13 (`target/site/jacoco-nonunit`) |
| backend | all levels (merged) | 752 / 0 | **99.53 %** (1,895/1,904) | 95.25 % (481/505) | **95.29 % / 95.29 %**: 708 killed (0 timed out) of 743; 33 survived, 2 no coverage; covered 741, `scoreCovered` 95.55 %; equivalent 0. Scope `app.contacts` minus `ContactsApplication`; tests: the whole JUnit suite (236 test classes: unit, architecture, integration, API acceptance) | PIT 1.20.3 + pitest-junit5-plugin 1.2.3, 4 threads (`out/logs/quality-gate-pit-full-1.txt`, `…-summary.txt`) |
| frontend | unit (Vitest component and unit tests) | 622 / 0 | 99.95 % | 98.82 % | | Vitest 3.2.7, @vitest/coverage-v8 (`out/logs/quality-gate-frontend-unit-r1.txt`) |
| frontend | non-unit (UI acceptance 89, end-to-end 8, cross-cutting browser specs; Playwright, Chromium) | not re-run in S6; last run 152 / 0 (/design, 820480b; no production code changed since) | not measured (D-89) | not measured (D-89) | | Playwright 1.56.1 |
| frontend | all levels | 622 / 0 under coverage | **99.95 %** (= unit, D-89) | 98.82 % | **95.13 % / 95.13 %**: 3,035 killed + 9 timed out of 3,200; 128 survived, 28 no coverage; covered 3,172, `scoreCovered` 95.96 %; equivalent 0. Scope `src/**/*.{ts,tsx}` minus tests, `components/ui/**`, `styles/**`, `test/**`, `main.tsx`; tests: the Vitest suite (D-89) | Stryker 9.2.0, vitest runner, `perTest` coverage, 6 runners (`out/logs/quality-gate-stryker-r1.txt`, `.json`) |

Figures at d061901 (frontend) and 7d9582b (backend; JaCoCo measured at 08b3a05, the later commits change only `LogHygieneIT`). First frontend mutation run (before the survivor round, 362 tests): 74.13 % (2,368 killed + 4 timed out of 3,200; 723 survived, 105 no coverage), `out/logs/quality-gate-stryker-full-1.txt`. Stryker counts 618 of the 622 Vitest tests in its dry run (358 of 362 in the first run).

## Quality gate (phase 5)

| Component | Line threshold met | Mutation threshold met | Shortfall (points) | Survivor rounds | Tests at first mutation run | Decision |
|---|---|---|---|---|---|---|
| backend | yes (99.53 % ≥ 95 %) | yes (95.29 % ≥ 90 %) | none | 0 (gate met on the first full run) | 752 (476 surefire + 276 failsafe) | D-89 |
| frontend | yes (99.95 % ≥ 95 %) | yes (95.13 % ≥ 90 %) | none | 1 (all areas: API hooks and cache, auth, validation, business-rule screens; confirmed by a full run over the whole scope) | 362 | D-89 |

## Equivalent mutants (phase 5)

| Component | Class and method | Mutation | Why it cannot change behaviour |
|---|---|---|---|
| backend, frontend | none claimed | | Both components meet the gate on the raw score; `scoreAdjusted` = `score`. |
