# Verification report

> For: agent (writes), human (reads) · Written in: phase 6 (`/verify`; `/fix` fills only the resolution column) · Source: `general/quality/*`, `general/security/*`, `general/design/ui-baseline.md`, `project/*` · Procedure: `skills/verify` · Agent: writes

One section per `/verify` run (`## Run 1`, `## Run 2`, …), each a self-check in a separate session, not an independent review.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|

## UI and accessibility

| Screen | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|

## NFR

| NFR | Measured | Pass value | Result |
|---|---|---|---|

## Threats and controls

| Threat | Control | How checked | Result |
|---|---|---|---|

## CI and deployment

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|

## Traceability

| R / AC / EX | Tests | Commits |
|---|---|---|

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|

## Run 1

Self-check in a separate session (S7 `/verify`, 2026-11-19), not an independent review. Commit under test `c56a1af` (code unchanged since the phase 5 gate `e5a42c5`). Local stack at `http://localhost:8090` (`APP_HOST_PORT=8090`; host port 8080 is taken), emulator at `9199`. Raw output in `out/logs/s7-*`.

Result: **phase 6 gate not passed** — 2 High, 3 Medium and 7 Low findings open (F-35 … F-46); D-90 waits for the human.

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 | not met (F-39, F-42) | backend `./mvnw -B verify` 476 unit + 276 integration, 0 failures (`s7-backend-verify.log`); frontend Vitest 621/622 in the first run (TagsField AC-009-02, F-42), 622/622 in the second (`s7-frontend-test.log`, `s7-frontend-test-2.log`); browser suites Chromium 151/152, the failure (AC-014-04: emulator closed the socket during `resetStack`) passes on re-run 5/5 (`s7-browser-all.txt`, `s7-browser-rerun-us014.txt`); Firefox and WebKit 0/200, browsers not installed (F-39). Input (166), acceptance (63) and hidden (10) manifests match (`sha256sum -c`) |
| DoD-R-02 | not met (F-40) | backend `spotless:check checkstyle:check spotbugs:check`: 0 Checkstyle violations, 0 SpotBugs bugs (`s7-backend-check.log`); frontend Prettier fails on `frontend/README.md`, ESLint 0 warnings, `tsc` 0 errors (`s7-frontend-check.log`, `s7-frontend-eslint.log`, `s7-frontend-tsc.log`) |
| DoD-R-03 | met | `docs/03_test-strategy.md` (phase 5), D-89; no production or test code changed since `e5a42c5`, so not re-measured |
| DoD-R-04 | met | `ArchitectureTest` 8/8 (AR rules, SR-04 no update/delete path, no cycles) |
| DoD-R-05 | not met (F-35, F-36) | review reports have no open Critical/High; image scans add two High findings |
| DoD-R-06 | met | runtime demonstration below: 36/36 checks (`s7-runtime-api.txt`) |
| DoD-R-07 | met | 324 R/AC/EX ids of US-001 … US-021: 301 named in a test, the 23 R ids that are not are covered by the AC/EX naming them (R-013-04 dropped, D-27); every id has a commit naming it or its story (`s7-traceability.txt`) |
| DoD-R-08 | not met (F-44, F-45, Low) | 19 screens × 3 widths in light and dark: axe no Critical/Serious in both modes, no sideways scrolling, both languages (`s7-browser-all.txt` light, `s7-browser-dark.txt` dark; screenshots `out/logs/screens/verify-run1-light/`, `verify-run1-dark/`, 105 each) |
| DoD-R-09 | partly; rest at `/release` | clean clone of `c56a1af`: backend and frontend build with the README commands; frontend `npm run check` fails (F-40) (`s7-clean-clone.txt`). Root `README.md` is still the template (written in phase 7) |
| DoD-R-10 | at `/release` | `docs/release-notes.md` is written in phase 7 |
| DoD-R-11 | met | D-01 … D-89: each has a resolution, a later record with the human answer (D-07, D-12, D-27, D-35, D-43, D-65) or is pending review; manifests match |
| DoD-R-12 | not met (F-46) | phase 3 commits `ac8f959` … `983e512` touch no production code; freeze commit `983e512` adds only the two manifests (re-freeze `7d602b3` per D-65); 14 of 21 stories have a `feat` commit naming the `US` id |
| DoD-R-13 | met | `docs/06_hidden-evaluation.md` Run 1: commit `c56a1af`, manifest ok, 123/123 passed |
| DoD-R-14 | not met (F-37, F-39) | NFR table below |
| DoD-R-15 | not met (F-40, F-41) | no CI run exists (branch not pushed, no local runner pinned); the same commands locally: backend job passes, frontend job fails at `check` (F-40), scan job: Trivy passes, gitleaks fails (F-41); `actionlint` clean (`s7-actionlint.txt`) |
| DoD-R-16 | partly | images build; backend image with production configuration (default profile, no emulator) starts healthy as user `app` and rejects emulator-signed tokens (`s7-prod-config.txt`); frontend image without `AUTH_EMULATOR_URL` serves no emulator setting, runs as `nginx` (`s7-prod-config-frontend.txt`). Staging/production compose files are written at `/release` (ADR-11, Q-39 OPEN). Threat table below: every threat checked except TLS (staging) |
| DoD-P01 | met in Chromium | E2E-01 … E2E-08 pass in Chromium (`s7-browser-all.txt`); Firefox, WebKit F-39 |
| DoD-P02 | human, staging | needs a staging host (Q-39 OPEN); listed for the release notes |
| DoD-P03 | partly | all 21 stories done (`docs/00_progress.md`); staging part needs a host (Q-39 OPEN) |
| DoD-P04 | partly; rest at `/release` | `docs/02_contracts/openapi.yaml`; `OpenApiContractIT` 2/2 against the running backend; deployment steps in the root README at `/release` |
| DoD-P05 | human, staging | restore of a backup on staging (Q-39 OPEN); listed for the release notes |

### UI and accessibility

| Screen | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|
| WF-01 … WF-05, WF-16, WF-20 (before or outside a Group) | 375, 768, 1440 px; light, dark; sl (en where signed in) | matches the approved wireframes and the final theme (D-84 … D-88); no sideways scrolling | none Critical/Serious |
| WF-06 Contact list | same | rows become cards below 768 px, navigation behind the menu button on mobile; **"Contacts" not marked as the current page** (F-44) | none Critical/Serious |
| WF-07 … WF-11, WF-13 … WF-15, WF-17 … WF-19 | same | as wireframes; WF-08: **focus stays on "Add an Attribute"** (F-45) | none Critical/Serious |
| Keyboard walk-through of E2E-01 (NFR-07) | Chromium, light and dark | passes | — |

### NFR

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 | `http_req_duration` p(95) 97.92 ms; checks 2,311/2,311 (`s7-nfr01-contact-list.txt`) | p(95) < 500 ms | pass |
| NFR-02 | Export 1.15 s; 10,000 data rows (`s7-nfr02-export.txt`) | < 10 s, 10,000 rows | pass |
| NFR-03 | 50 Groups, 200 Users: list p(95) 134.89 ms; Export 0.85 s, 10,000 rows (`s7-nfr03-capacity.txt`) | NFR-01, NFR-02 hold | pass |
| NFR-04 | health UP, `restart: unless-stopped`, UP again 12 s after `docker compose restart backend` (`s7-nfr04-recovery.txt`) | < 60 s | pass |
| NFR-05 | no integration test exists; on the local stack 10,003 Contacts with `deletedAt` 31 days ago, removal job at backend start: Audit entries from 07:24:49 to 07:27:37 (2 min 48 s), 0 rows left by 07:27:52 (`s7-nfr05-removal.txt`) | < 60 s, 0 rows | **fail** (F-37) |
| NFR-06 | Chromium 152/152 (one re-run); Firefox and WebKit not installed, 0/200 launched | all pass in three browsers | **no evidence** (F-39) |
| NFR-07 | axe on 19 screens × 3 widths, light and dark: no Critical/Serious; keyboard walk-through passes | none; walk-through recorded | pass |
| NFR-08 | `catalogs.test.ts` passes; screenshots of every screen in sl and en at three widths | test passes; screenshots exist | pass |
| NFR-09 | — | under 2 minutes (human, staging) | human (DoD-P02) |

### Threats and controls

| Threat | Control | How checked | Result |
|---|---|---|---|
| S: forged or replayed ID token | SR-01 Admin SDK verification | runtime: no, garbage and tampered token → 401; production profile rejects an emulator-signed token → 401; `FirebaseTokenVerifierTest`, `Us001SignInAcceptanceIT` | controlled |
| S: token of a deleted account | D-20 deleted User → 401 | `FirebaseTokenFilterTest`, US-021 acceptance tests (pass) | controlled |
| S: unverified e-mail claims an Invitation | D-16, R-004-05 | `InvitationServiceTest`, `Us004InvitationsAcceptanceIT`, `InvitationPage.test.tsx` | controlled |
| T: another Group's id or a Role | SR-02 `AccessGuard` | `AccessMatrixIT` 3/3; runtime: non-Member → 404, Viewer edit → 403 | controlled |
| T: lost update | BR-06 | runtime: stale Version → 409 `version.conflict` | controlled |
| R: Member denies an action | SR-04 Audit entries | `ArchitectureTest`, `AuditEntryRepositoryIT`; runtime: `contacts.exported`, `contact.removed` rows written | controlled |
| I: Group data to a non-Member | BR-02 404 | runtime 404 for read and list; E2E-03 passes | controlled |
| I: personal data in logs or errors | SB-07, SR-08 | `LogHygieneIT` (canary); runtime: error bodies without internals (5 cases), backend log without e-mails, names or tokens; **nginx access log carries e-mails, search text and Invitation tokens** | **weaker than required** (F-38) |
| D: request flooding | SR-03 | runtime: 101st request in a minute → 429, `Retry-After: 58` | controlled |
| D: huge payloads, big exports | nginx 2 MB, schema limits, R-016-05, streaming | runtime: 3 MB body → 413; NFR-02 | controlled |
| E: Viewer or Editor does Owner actions | BR-03, BR-04, ADR-04 | `AccessMatrixIT`; runtime Viewer → 403 | controlled |
| S: phishing on a look-alike page | Firebase-hosted flows; CSP `frame-src` auth domain only | production-style frontend: `frame-src https://<auth domain>` only | controlled (user awareness out of scope) |
| T: script injection steals the ID token | CSP, React escaping, no `dangerouslySetInnerHTML` | CSP header present (`script-src 'self' https://apis.google.com`); ESLint security 0 warnings | controlled |
| I: emulator settings reach production | unset outside test/local | production-profile backend rejects emulator tokens; production-style `config.json` has no `authEmulatorUrl` | controlled; staging check at `/release` |
| S: token for another project | `aud`/`iss` check | EX-001-02 (`Us001SignInAcceptanceIT`) | controlled |
| T/I: SQL injection | SB-05 | SpotBugs/FindSecBugs 0; EX-014-05 `LIKE` escaping test; runtime `sort=drop table` → 400 | controlled |
| I: database credentials leak | ES-02, `.env` ignored, gitleaks | `.env` leak check: 0 files, 0 commit messages; DB has no host port | controlled (gitleaks F-41 is a false positive) |
| D: removal job deletes the wrong data | BR-07, BR-08 boundaries | `RemovalJobTest`, EX-003-02, EX-012-02 | controlled |
| T: formula injection in Excel | SR-07 | runtime: `=1+1` exported as text, no `<f>`; EX-016-03 | controlled |
| I: Export outside the scope | same query as the list | EX-016-07 test; runtime `contactIds` export of one Contact | controlled |
| T: clickjacking | `frame-ancestors 'none'`, `X-Frame-Options: DENY` | runtime headers | controlled |
| I: Contact data seen by non-Members | BR-01, BR-02, SR-02 | as above | controlled |
| I: old personal data in Versions after erasure | SR-06, BR-08, D-21 | `Us013PermanentDeleteAcceptanceIT`, `PermanentDeleteServiceTest`; NFR-05 run: 0 Contact and Version rows left | controlled |
| I: User e-mail shown to other Members | R-005-01 | US-005 acceptance tests | controlled |
| I: User data kept after account deletion | D-20 | US-021 acceptance tests (AC-021-01 Versions show "Deleted user") | controlled |
| I: Invitation e-mail or token leaks | SR-05 hashed token | `InvitationTokenTest`, `InvitationRepositoryIT`; **tokens appear in the nginx access log** | **weaker than required** (F-38) |
| T/R: Audit entries changed or removed | SR-04 | `ArchitectureTest` | controlled |
| I: Audit entries reveal personal data | ids and action only | `audit_entry` columns: id, group, user, action, target, at | controlled |
| SB-04, SR-09 TLS and CORS | reverse proxy TLS; CORS | TLS: staging only (Q-39 OPEN, `/release`). CORS: no CORS configuration exists; a cross-origin preflight gets 401 without `Access-Control-Allow-Origin`, so only the same origin works | TLS not checkable here; CORS F-43 |
| SB-08 dependencies | Trivy | `fs` (CI form): 0 Critical/High; with dev dependencies 2 Critical, 3 High in `tinypool`, `minimatch` (dev tooling, D-13: Medium); images: backend 2 High (F-35), frontend 5 High (F-36), postgres `gosu` (D-11/D-12: Low) | **not met** (F-35, F-36) |
| SB-09 static analysis and secrets | SpotBugs/FindSecBugs, ESLint security, gitleaks | 0, 0; gitleaks 1 finding (F-41) | false positive, CI fails |
| SB-10 headers | nginx | CSP, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, `Permissions-Policy` present | controlled |
| SB-11 least privilege | non-root images | backend `app`, frontend `nginx` | controlled |

Scanner output: `s7-trivy-fs.txt`, `s7-trivy-fs-dev.txt`/`.json`, `s7-trivy-image-*-fixable.json` (with `--ignore-unfixed`, as D-11), `s7-trivy-image-contacts-{frontend,postgres}.json` (all), `s7-gitleaks.txt`, `s7-gitleaks-dir.txt`, `s7-gitleaks-report.json` (redacted).

### CI and deployment

- CI (`.github/workflows/ci.yml`): no run exists for this branch (not pushed) and no local runner is pinned. The jobs' commands run locally: `backend` passes (check, verify, package); `frontend` fails at `npm run check` (F-40), test and build pass; `scan`: Trivy `fs` passes, gitleaks fails (F-41). `actionlint 1.7.8` clean.
- Deployment: images `contacts-backend:local`, `contacts-frontend:local`, `contacts-postgres:16.15` build (`s7-stack-up.log`) and start; production configuration checked per image (DoD-R-16). Staging and production compose files do not exist yet (ADR-11: written at `/release` once Q-39 is answered).

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all four containers | local stack, `perf/load-seed.sh` (seed + 10,000 Contacts) | sign-in through the emulator as Owner, Editor, Viewer and a non-Member; Group list; list, search and sort in 10,000 Contacts (54 ms); add with Attribute and Tag; edit; stale Version 409; Viewer 403; non-Member 404; History; restore Version 1; Export of a checked Contact (`.xlsx`, file name R-016-06); recycle bin move, list, restore; error bodies; 413; CORS; rate limit (`s7-runtime-api.txt`) | 36/36 |
| backend | production configuration (default profile), compose database | health UP, non-root, emulator token 401, OpenAPI and actuator closed without token (`s7-prod-config.txt`) | pass |
| frontend | production-style settings | `config.json` without emulator, CSP, non-root, SPA route (`s7-prod-config-frontend.txt`) | pass |
| backend | restart (NFR-04) and removal job (NFR-05) | see NFR | NFR-04 pass, NFR-05 fail |
| browser | Chromium, light and dark | 152 browser tests incl. E2E-01 … 08, axe, languages | pass |

Logs and errors (step 8): backend log of the whole session has no e-mail, name, search text, token or stack trace; error bodies are `application/problem+json` with codes only. `.env` leak check: no tracked or untracked file and no commit message contains a `.env` value (5 values checked; output was file names only, none listed).

### Traceability

Full table in `out/logs/s7-traceability.txt` (id, test files, commits).

| R / AC / EX | Tests | Commits |
|---|---|---|
| 324 ids of US-001 … US-021 (R 112, AC 94, EX 118) | 301 named in backend `*AcceptanceIT`, frontend `acceptance/*.spec.ts` or unit tests; 23 R covered through the AC/EX that name them (e.g. R-011-03 → AC-011-04) | every id: a commit naming it (264) or its story's R/AC/EX ids; `feat` commits per story 11 … 50 |

### Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-35 | High | Trivy image `contacts-backend:local` (SB-08) | The backend image ships `com.google.guava:guava` 33.4.0-jre (CVE-2026-102554, fixed 33.7.2) inside the jar and Go stdlib 1.27.3 in the base image's `/usr/bin/pebble` (CVE-2026-97054, fixed 1.27.4). The CI `fs` scan of `pom.xml` does not see either. || fixed `d90d1ce`: guava 33.7.2-jre pinned, `/usr/bin/pebble` removed (D-92); image 0 Critical/High |
| F-36 | High | Trivy image `contacts-frontend:local` (SB-08, D-12) | The frontend image has `nginx` 1.30.6-r1 with 5 High advisories (CVE-2026-113425, -121810, -123007, -123633, -123634), all fixed in 1.30.7-r0, although D-12 requires the package upgrade at build time (the upgrade layer is cached). || fixed `47a9f9b`: `nginx:1.30.7-alpine` (D-92); image 0 Critical/High |
| F-37 | Medium | NFR-05, spec §8 | The removal job needs about 2 min 48 s for 10,000 Contacts (pass value < 60 s): one Audit entry and one Merge lookup per Contact before the bulk delete. The integration test of spec §8 (10,000 Contacts, `RemovalJob.run()` timed) does not exist. || fixed `52a32b0`: merged Contacts read once per Group; `RemovalJobIT` (NFR-05) added, job 8.2 s; stack 0.65 s |
| F-38 | Medium | runtime logs (SB-07, SR-08, SR-05) | The nginx access log writes request paths and query strings: Contact e-mails (`email-matches?email=…`), search text (names) and Invitation tokens (`/invitations/<token>`, 28 GETs of unused tokens). Logs must carry ids only; tokens are to be kept only as a hash. || fixed `183c2bb`: log format with method, redacted path, status; `scripts/check-access-log.sh` passes |
| F-39 | Medium | NFR-06, DoD-R-01 | Firefox and WebKit are not installed on the host, so 200 browser tests (the Firefox and WebKit projects) could not start; NFR-06 has no evidence. The agent does not install software: D-90. || decision: D-90 → D-91 (Firefox, WebKit installed; Firefox runs); WebKit needs `libwoff1` (sudo): D-94 pending |
| F-40 | Low | `npm run check` (DoD-R-02, DoD-R-15) | Prettier reports `frontend/README.md` (two tables not aligned, since `381c42f`), so the frontend check and the CI `frontend` job fail. || fixed `94608a5`: README formatted; `npm run check` passes |
| F-41 | Low | gitleaks (SB-09, DoD-R-15) | gitleaks flags `docs/decisions-log.md:362` (D-33 quotes the documentation example token; same value as the accepted finding, checked by hash). Its fingerprint is not in `.gitleaksignore`, so the CI `scan` job fails. || fixed `1aca83c`: fingerprint added to `.gitleaksignore` (D-33); gitleaks passes |
| F-42 | Low | Vitest (DoD-R-01; D-67, D-89) | `TagsField.test.tsx` AC-009-02 failed once in a full run while the backend suite ran in parallel; it passes alone (3/3) and in a second full run. The timeouts of D-89 do not make it reliable under load. || fixed `4b86b3d`: suggestion wait 10 s; 8/8 runs under double load pass |
| F-43 | Low | runtime (SR-09, spec §6.2) | No CORS configuration exists although spec §6.2 and `AppProperties` say CORS allows `APP_PUBLIC_URL`. Effect today: preflights get 401 without `Access-Control-Allow-Origin` for every origin, so only the same origin works. || fixed `a8210bd`: CORS for `APP_PUBLIC_URL` only; preflight own origin 200, other 403 |
| F-44 | Low | screenshots (UI-10) | On the Contact list the header item "Contacts" is not marked as the current page, while "Recycle bin" is on its page (light and dark, all widths). || fixed `69fb305`: "Contacts" links to the Group's list and is marked current |
| F-45 | Low | WF-08, keyboard (D-83 (h)) | After "Add an Attribute" the focus stays on the button; the new Key field is 24 Tab presses away (`AttributesField.tsx:85` does not move the focus). || fixed `f13e8a1`: the new Key gets the focus (0 Tab in the walk-through) |
| F-46 | Low | `git log` (DoD-R-12) | US-001, US-003, US-004, US-011, US-016, US-019, US-020 have no `feat` commit naming the `US` id (their `feat` commits name R/AC/EX/T ids). History must not be rewritten, so this needs acceptance. || accepted: D-93 (history must not be rewritten; traceability through R/AC/EX ids) |

## Run 2

Self-check in a separate session (S9 `/verify`, 2026-11-19 … 20), not an independent review. Commit under test `71dcc9c` (fixes of F-35 … F-45 since Run 1). Local stack rebuilt from `71dcc9c` with `scripts/up.sh` at `http://localhost:8090` (`APP_HOST_PORT=8090`), emulator at `9199`. Raw output in `out/logs/s9-*`. Between the runtime checks and the mutation runs the host ran out of memory (the first mutation run was stopped), then restarted and upgraded the JDK in place (21.0.13 → 21.0.13.1, unattended upgrade; a second attempt failed on the spawn helper and was repeated after the upgrade finished). The local stack was stopped for the mutation runs (human's choice).

Result: **phase 6 gate passed**: no finding open; F-35 … F-45 closed, F-46 accepted (D-93). Records still to complete (not findings): the `Human response` of D-94 and the host JDK patch change (handover notes).

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 | met | backend `./mvnw -B verify` 477 unit + 279 integration, 0 failures (`s9-backend-verify.log`); frontend Vitest 624/624 (`s9-frontend-test.log`); browser suites 456/456 in one run: Chromium 152, Firefox 152, WebKit 152 (`s9-browser-all.txt`); input (166) and acceptance (63) manifests match (`sha256sum -c`); hidden manifest: the runner reports "Manifest: ok" at `71dcc9c` (the vault is not readable by this session), and `docs/03_hidden-manifest.sha256` is unchanged since the freeze `983e512` |
| DoD-R-02 | met | backend `spotless:check checkstyle:check spotbugs:check`: 0 Checkstyle violations, 0 SpotBugs bugs (`s9-backend-check.log`); frontend `npm run check`: Prettier, ESLint 0 warnings, `tsc` 0 errors (`s9-frontend-check.log`) |
| DoD-R-03 | met | re-measured once, both components changed since phase 5. Backend JaCoCo all levels 99.53 % lines (1,908/1,917), 95.25 % branches (481/505); unit 92.12 %, non-unit 95.88 %. Frontend Vitest 99.95 % lines (4,402/4,404), 98.83 % branches. Mutation: backend PIT 1.20.3 **97.07 % raw / 97.07 % adjusted** (729 killed of 751; 20 survived, 2 no coverage; `scoreCovered` 97.33 %; equivalent 0; whole JUnit suite, 2,605 test runs, 51 min 42 s; JDK 21.0.13.1) (`s9-pit.log`, `backend/target/pit-reports/`); frontend Stryker 9.2.0 **94.87 % raw / 94.87 % adjusted** (3,045 killed + 4 timed out of 3,214; 137 survived, 28 no coverage; `scoreCovered` 95.70 %; equivalent 0; Vitest suite, 620 tests in the dry run; Node 22.23.3; 68 min) (`s9-stryker.log`, `frontend/reports/mutation/`). Thresholds: lines >= 95 %, mutation adjusted >= 90 %: both components meet them. Scope and tools as `docs/03_test-strategy.md` |
| DoD-R-04 | met | `ArchitectureTest` passes in the backend suite (AR rules, SR-04, no cycles) |
| DoD-R-05 | met | review reports have no open Critical/High; F-35 and F-36 fixed: images `contacts-backend:local` and `contacts-frontend:local` 0 fixable Critical/High (`s9-trivy-image-*-fixable.json`); Trivy `fs` 0 Critical/High; gitleaks no leaks |
| DoD-R-06 | met | runtime demonstration below: 43/43 checks (`s9-runtime-api.txt`) |
| DoD-R-07 | met | 324 R/AC/EX ids of US-001 … US-021 (R 112, AC 94, EX 118): 302 named in a test; each of the other 22 R ids is named in 1 … 3 AC/EX rows of its story, which are named in tests; every id has a commit naming it or its story (`s9-traceability.txt`) |
| DoD-R-08 | met | 19 screens × 3 widths, light (Chromium, Firefox, WebKit) and dark (Chromium): axe no Critical/Serious, no sideways scrolling, both languages (`s9-browser-all.txt`, `s9-browser-dark.txt`; screenshots `out/logs/screens/verify-run2-light/`, `verify-run2-dark/`, 105 each); F-44 and F-45 fixed (below) |
| DoD-R-09 | partly; rest at `/release` | clean clone of `71dcc9c`: backend build and check, frontend `npm ci`, build and `npm run check` all exit 0 with the README commands (`s9-clean-clone.txt`). Root `README.md` is written in phase 7 |
| DoD-R-10 | at `/release` | `docs/release-notes.md` is written in phase 7 |
| DoD-R-11 | met, one record to complete | D-01 … D-94: each has a resolution, a later record with the human answer, or is pending review, except D-94 (blocking, no `Human response`). Its option 1 has been carried out by the human: `libwoff1` is installed (`dpkg -s`) and WebKit runs 152/152. The `Human response` of D-94 is still to be recorded (see "Next"). Input and acceptance manifests match |
| DoD-R-12 | met with D-93 | the 18 commits since Run 1 follow `commits.md`: subjects 51 … 71 characters with IDs, each at most 15 files and 400 lines without logs; F-46 accepted (D-93) |
| DoD-R-13 | met | `docs/06_hidden-evaluation.md` Run 2: commit `71dcc9c`, manifest ok, 123/123 passed |
| DoD-R-14 | met (NFR-09 human) | NFR table below |
| DoD-R-15 | met (local run) | no CI run exists (branch not pushed, no local runner pinned); every command of `.github/workflows/ci.yml` passes locally: backend check, verify, package; frontend `npm ci`, check, test, build; Trivy `fs` 0 Critical/High (`s9-trivy-fs.txt`); gitleaks 476 commits, no leaks (`s9-gitleaks.txt`); `actionlint 1.7.8` clean (`s9-actionlint.txt`) |
| DoD-R-16 | partly; rest at `/release` | images build from `71dcc9c` (`s9-stack-up.log`); backend with production configuration (default profile, no emulator) healthy as user `app`, rejects the emulator-signed token that the local stack accepts (`s9-prod-config.txt`); frontend without `AUTH_EMULATOR_URL`: no emulator setting, CSP, user `nginx`, nginx 1.30.7 (`s9-prod-config-frontend.txt`). Staging/production compose files are written at `/release` (ADR-11, Q-39 OPEN). Threat table below: every threat checked except TLS (staging) |
| DoD-P01 | met | E2E-01 … E2E-08 pass in Chromium, Firefox and WebKit (`s9-browser-all.txt`) |
| DoD-P02 | human, staging | needs a staging host (Q-39 OPEN); listed for the release notes |
| DoD-P03 | partly | all 21 stories done; staging part needs a host (Q-39 OPEN) |
| DoD-P04 | partly; rest at `/release` | `docs/02_contracts/openapi.yaml`; `OpenApiContractIT` passes in the backend suite; deployment steps in the root README at `/release` |
| DoD-P05 | human, staging | restore of a backup on staging (Q-39 OPEN); listed for the release notes |

### UI and accessibility

| Screen | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|
| WF-01 … WF-05, WF-16, WF-20 | 375, 768, 1440 px; light (three browsers), dark (Chromium); sl (en where signed in) | as Run 1; no sideways scrolling | none Critical/Serious |
| WF-06 Contact list | same | "Contacts" is marked as the current page (F-44 fixed; screenshots light and dark) | none Critical/Serious |
| WF-07 … WF-11, WF-13 … WF-15, WF-17 … WF-19 | same | as wireframes; WF-08: the new Key gets the focus after "Add an Attribute" (F-45 fixed, `AttributesField.test.tsx`) | none Critical/Serious |
| Keyboard walk-through of E2E-01 (NFR-07) | Chromium, Firefox, WebKit; dark Chromium | passes | — |

### NFR

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 | `http_req_duration{scenario:contact_list}` p(95) 93.32 ms; checks 2,315/2,315 (`s9-nfr01-contact-list.txt`; the 20 failed requests of 2,454 are set-up Invitations of existing Members, as in Run 1) | p(95) < 500 ms | pass |
| NFR-02 | Export 1.13 s; 10,000 data rows (`s9-nfr02-export.txt`) | < 10 s, 10,000 rows | pass |
| NFR-03 | 50 Groups, 200 Users: list p(95) 140.62 ms; Export 1.51 s, 10,000 rows (`s9-nfr03-capacity.txt`) | NFR-01, NFR-02 hold | pass |
| NFR-04 | health UP, `restart: unless-stopped`, UP again 13 s after `docker compose restart backend` (`s9-nfr04-recovery.txt`) | < 60 s | pass |
| NFR-05 | `RemovalJobIT`: 10,000 Contacts with `deletedAt` 31 days ago, `RemovalJob.run()` 7.14 s; 0 due Contacts, Versions, Attributes, Tag links left, the one not yet due kept (`s9-backend-verify.log`) | < 60 s, 0 rows | pass (F-37 fixed) |
| NFR-06 | 456/456: Chromium 152, Firefox 152, WebKit 152 (`s9-browser-all.txt`) | all pass in three browsers | pass (F-39 closed) |
| NFR-07 | axe on 19 screens × 3 widths in three browsers and dark Chromium: no Critical/Serious; keyboard walk-through passes | none; walk-through recorded | pass |
| NFR-08 | `catalogs.test.ts` passes; screenshots of every screen in sl and en at three widths | test passes; screenshots exist | pass |
| NFR-09 | — | under 2 minutes (human, staging) | human (DoD-P02) |

### Threats and controls

Rows not listed are unchanged from Run 1 and were re-checked by the same tests (all pass in this run) and runtime checks (`s9-runtime-api.txt`): forged, tampered and garbage tokens 401; non-Member 404; Viewer 403; stale Version 409; rate limit 429 with `Retry-After`; 3 MB body 413; `=1+1` exported as text without `<f>`; error bodies `application/problem+json` without internals; headers CSP with `frame-ancestors 'none'`, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, `Permissions-Policy`.

| Threat | Control | How checked | Result |
|---|---|---|---|
| I: personal data in logs or errors | SB-07, SR-08 | `LogHygieneIT`; `scripts/check-access-log.sh` passes (`s9-access-log.txt`); whole session: frontend access log without query strings, 485 Invitation requests logged as `<token>`; backend log without e-mails, names, search text, tokens or stack traces (`s9-backend-runtime.log`, `s9-prod-backend.log`) | controlled (F-38 fixed) |
| I: Invitation e-mail or token leaks | SR-05 hashed token | `InvitationTokenTest`, `InvitationRepositoryIT`; access log as above | controlled (F-38 fixed) |
| SB-04, SR-09 TLS and CORS | reverse proxy TLS; CORS for `APP_PUBLIC_URL` | CORS: preflight from `APP_PUBLIC_URL` 200 with `Access-Control-Allow-Origin`, from another origin 403 without it (runtime); CORS tests in `ApplicationIT`. TLS: staging only (Q-39 OPEN, `/release`) | CORS controlled (F-43 fixed); TLS not checkable here |
| SB-08 dependencies | Trivy | `fs` (CI form) 0 Critical/High; images with `--ignore-unfixed`: backend 0, frontend 0; postgres 27 in `gosu` (Go stdlib; D-11/D-12: Low); emulator 38 (local and test only, never deployed, D-29) | controlled (F-35, F-36 fixed) |
| SB-09 static analysis and secrets | SpotBugs/FindSecBugs, ESLint security, gitleaks | 0, 0; gitleaks no leaks (F-41 fixed) | controlled |
| I: emulator settings reach production | unset outside test/local | production-profile backend rejects an emulator token that the local stack accepts (401 vs 200); production-style `config.json` without `authEmulatorUrl` | controlled; staging check at `/release` |
| SB-11 least privilege | non-root images | backend `app`, frontend `nginx` | controlled |

Scanner output: `s9-trivy-fs.txt`, `s9-trivy-image-{contacts-backend,contacts-frontend,contacts-postgres,contacts-firebase-emulator}-fixable.json`, `s9-gitleaks.txt`, `s9-actionlint.txt`.

### CI and deployment

- CI (`.github/workflows/ci.yml`): no run exists for this branch and no local runner is pinned. Every job command runs locally and passes: `backend` (check, verify, package), `frontend` (`npm ci`, check, test, build), `scan` (Trivy `fs`, gitleaks). `actionlint 1.7.8` clean.
- Deployment: images `contacts-backend:local`, `contacts-frontend:local`, `contacts-postgres:16.15` built from `71dcc9c` and started healthy (`s9-stack-up.log`); production configuration checked per image (DoD-R-16). Staging and production compose files: `/release` (ADR-11, Q-39).

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all four containers | local stack from `71dcc9c`, `perf/load-seed.sh` (seed + 10,000 Contacts) | SPA and headers; sign-in through the emulator as Owner, Editor, Viewer and a non-Member; no, garbage and tampered token; Group list; search and sort in 10,000 Contacts; invalid sort 400; add with Attribute and Tag; edit; stale Version 409; Viewer 403; non-Member 404 (read, list); e-mail match; History; restore Version 1; Export of a checked Contact (file name R-016-06, `=1+1` as text); recycle bin move, list, restore; Invitation create, view, accept, new Viewer reads and gets 403 on edit; 413; validation and 404 bodies; CORS own and other origin; rate limit (`s9-runtime-api.txt`) | 43/43 |
| backend | production configuration (default profile), compose database | health UP, user `app`, emulator token 401, OpenAPI, Swagger UI and actuator closed without token (`s9-prod-config.txt`) | pass |
| frontend | production-style settings | `config.json` without emulator, CSP, user `nginx`, SPA route (`s9-prod-config-frontend.txt`) | pass |
| backend | restart (NFR-04) | see NFR | pass |
| browser | Chromium, Firefox, WebKit; dark Chromium | 456 browser tests incl. E2E-01 … 08, axe, languages; 55 dark | pass |

Logs and errors (step 8): see the threat rows above. `.env` leak check: no tracked or untracked file and no commit message contains a `.env` value (5 values checked; file names only, none listed; commits `0`).

### Traceability

Full table in `out/logs/s9-traceability.txt` (id, test files, commits).

| R / AC / EX | Tests | Commits |
|---|---|---|
| 324 ids of US-001 … US-021 (R 112, AC 94, EX 118) | 302 named in backend tests, frontend unit tests or browser specs; 22 R covered through the AC/EX rows that name them | every id: a commit naming it or its story |

### Findings

No new finding in this run.

Run 1 findings: F-35 … F-38, F-40 … F-45 re-checked and closed by this run (evidence above); F-39 closed (WebKit runs, NFR-06 passes); F-46 accepted (D-93).
