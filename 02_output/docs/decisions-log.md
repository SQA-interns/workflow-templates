# Decisions log

> For: agent (writes), human (reads) · Written in: every session · Format: `general/rules/decisions.md` ("Decision record") · Agent: appends only

## D-01: Host JDK reports 21.0.13 instead of the pinned 21.0.8
- Timestamp: 2026-11-12T12:00+01:00
- Session: S1 /preflight
- Class: open
- Type: non-blocking
- Trigger: `java -version` and `javac -version` report OpenJDK 21.0.13 (Ubuntu build); `tech-stack.md` platform `java` pins Eclipse Temurin 21.0.8 `[ASSUMPTION] verify in preflight`.
- Options: 1. Build and test with the host JDK 21.0.13 (same major and minor, newer patch); the pin stays authoritative for the container image (default). 2. Install Temurin 21.0.8 on the host.
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-02: vitest and @vitest/coverage-v8 3.2.4 have a Critical advisory fixed in the same minor
- Timestamp: 2026-11-12T12:00+01:00
- Session: S1 /preflight
- Class: allow
- Type: non-blocking
- Trigger: OSV GHSA-5xrq-8626-4rwp (Critical, Vitest UI server file read and execution) affects 3.2.4, fixed in 3.2.6; latest 3.2.x is 3.2.7. Rule: `general/rules/decisions.md` (same-minor patch replacement is non-blocking).
- Options: 1. Use 3.2.7 for both packages (default). 2. Keep 3.2.4 and never start the Vitest UI.
- Human response: none
- Resolution: 1 — pending review (resolution and scan re-checked in the next `/preflight`)
- Overrides: `tech-stack.md` tooling `vitest` and `@vitest/coverage-v8` 3.2.4 → 3.2.7

## D-03: Node.js on PATH is 18.19.1; the stack pins 22.20.0
- Timestamp: 2026-11-12T12:00+01:00
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: `node --version` → v18.19.1 (`/usr/bin/node`), confirmed by `type -a node`. Vite 7 needs ^20.19 or ≥ 22.12, firebase-tools 14 and Stryker 9 need ≥ 20. nvm on the host has v22.23.3 (a different minor from the pin) and v24.x; v22.20.0 is not installed. The agent does not install software.
- Options: 1. Approve Node.js 22.23.3 (installed via nvm) as the pin and make it the default in non-interactive shells, e.g. `nvm alias default 22.23.3` (default). 2. Install Node.js 22.20.0 yourself and put it first on PATH. 3. Another version you name.
- Human response: none
- Resolution: pending
- Overrides: `tech-stack.md` platform `node` (if option 1 or 3)

## D-04: trivy, gitleaks and k6 are not installed
- Timestamp: 2026-11-12T12:00+01:00
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: `trivy`, `gitleaks`, `k6` not found (`command -v` in a login shell, `/usr/local/bin`, `~/.local/bin`, `~/go/bin`). Their pinned container images resolve: `aquasec/trivy:0.67.2`, `zricethezav/gitleaks:v8.28.0`, `grafana/k6:1.3.0`. Without Trivy the dependency scan of step 6 cannot run.
- Options: 1. Run the three tools from these pinned official container images through Docker (no host install; tooling entries change to `ecosystem: container`) (default). 2. Install the binaries yourself at the pinned versions. 3. Install or run newer versions you name.
- Human response: none
- Resolution: pending
- Overrides: `tech-stack.md` tooling `trivy`, `gitleaks`, `k6` (if option 1 or 3)

## D-05: Backend stack has Critical and High advisories; Spring Framework 6.2 has no open-source fix
- Timestamp: 2026-11-12T12:00+01:00
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: Spring Boot 3.5.7 and its managed Tomcat 10.1.48, Spring Framework 6.2.12, Spring Security 6.5.6 and pgjdbc 42.7.8 carry Critical/High advisories (`out/logs/preflight-osv-2026-11-12.txt`). Spring Boot 3.5.16 (latest 3.5.x) fixes the actuator and Security ones but still manages Tomcat 10.1.55 (Critical until 10.1.59) and Spring Framework 6.2.19, which keeps two Critical advisories fixed only in 7.0.9 (GHSA-j9f9-w8pj-32f8 SSE fragment rendering, GHSA-pc63-qcmh-9cmg XsltView). Spring Boot 4.1.2 manages Spring Framework 7.0.10, Security 7.1.2, Tomcat 11.0.27, pgjdbc 42.7.14: no OSV advisory. Q-38 asked for "Spring Boot 3.x"; a new major also changes companions (e.g. springdoc 3.x) that need pins.
- Options: 1. Move to Spring Boot 4.1.2 (Java 21 kept); the agent re-pins the companions to compatible versions, verified in the next `/preflight` (default; the 3.5 line no longer gets open-source patches, so every new CVE would block again). 2. Stay on Spring Boot 3.5.16, override Tomcat to 10.1.61 and pgjdbc to 42.7.14, and lower the two Spring Framework Criticals to Low with the evidence that the product uses no XsltView and no SSE fragment rendering (enforced by an ArchUnit rule). 3. Another version you name.
- Human response: none
- Resolution: pending
- Overrides: `tech-stack.md` backend dependencies (all `spring-boot-*` entries; plus overrides per the chosen option)

## D-06: Frontend packages need minor-version bumps to clear High advisories
- Timestamp: 2026-11-12T12:00+01:00
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: High advisories with fixes only in a later minor: react-router 7.9.4 (fixed by 7.18.2; latest 7.18.5, no advisory), zod 4.1.12 (fixed 4.6.6; latest 4.6.7), vite 7.1.12 (dev server; fixed 7.3.7; latest 7.3.8). A different minor needs approval (`general/rules/decisions.md`).
- Options: 1. react-router 7.18.5, zod 4.6.7, vite 7.3.8 (default). 2. Keep the pins and accept the advisories as Medium with evidence (SPA without React Router framework/RSC mode; Vite dev server never exposed) — needs written evidence per advisory. 3. Other versions you name.
- Human response: none
- Resolution: pending
- Overrides: `tech-stack.md` dependencies `react-router`, `zod`, tooling `vite`

## D-07: Human answers to D-03, D-04, D-05, D-06
- Timestamp: 2026-11-12T13:30+01:00
- Session: S1 /preflight (run 2)
- Class: allow
- Type: non-blocking
- Trigger: `/preflight D-03: 1, D-04: 1, D-05: 1, D-06: 1` ("all recommended").
- Options: none (records the answers).
- Human response: D-03 option 1, D-04 option 1, D-05 option 1, D-06 option 1; 2026-11-12.
- Resolution: D-03 → Node.js 22.23.3. D-04 → trivy 0.67.2, gitleaks 8.28.0 and k6 1.3.0 run from their official images (`aquasec/trivy:0.67.2`, `zricethezav/gitleaks:v8.28.0`, `grafana/k6:1.3.0`). D-05 → Spring Boot 4.1.2 (companions in D-08). D-06 → react-router 7.18.5, zod 4.6.7, vite 7.3.8.
- Overrides: `tech-stack.md` platform `node` 22.20.0 → 22.23.3; tooling `trivy`, `gitleaks`, `k6` ecosystem binary → container; backend `spring-boot-*` 3.5.7 → 4.1.2; `react-router` 7.9.4 → 7.18.5; `zod` 4.1.12 → 4.6.7; `vite` 7.1.12 → 7.3.8

## D-08: Companion pins for Spring Boot 4.1.2
- Timestamp: 2026-11-12T13:30+01:00
- Session: S1 /preflight (run 2)
- Class: allow
- Type: non-blocking
- Trigger: D-05 option 1 asks the agent to re-pin the companions. Spring Boot 4 renames and splits modules. All coordinates resolve on Maven Central (HTTP 200); Trivy scan of the resulting tree: 0 Critical/High/Medium (`out/logs/preflight-trivy-backend.txt`).
- Options: 1. The pins below (default).
- Human response: none
- Resolution: 1 — pending review. `spring-boot-starter-web` → `spring-boot-starter-webmvc` 4.1.2; add `spring-boot-starter-flyway` 4.1.2 (Flyway autoconfiguration is its own module in Boot 4; `flyway-database-postgresql` stays, managed 12.4.0); test slices `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, `spring-boot-starter-data-jpa-test` 4.1.2 (test scope); `org.testcontainers:postgresql` → `org.testcontainers:testcontainers-postgresql` (managed 2.0.5); `springdoc-openapi-starter-webmvc-ui` 2.8.13 → 3.2.0 (built against Boot 4.1.2). Managed by Boot 4.1.2: Spring Framework 7.0.10, Spring Security 7.1.2, Tomcat 11.0.27, Hibernate 7.4.13.Final, Jackson 3.1.7, JUnit Jupiter 6.0.3, pgjdbc 42.7.14. Not yet proven: `pitest-junit5-plugin` 1.2.3 with JUnit 6 (checked when mutation testing first runs).
- Overrides: `tech-stack.md` backend dependencies as listed

## D-09: Container images replaced by their latest patch of the same line
- Timestamp: 2026-11-12T13:30+01:00
- Session: S1 /preflight (run 2)
- Class: allow
- Type: non-blocking
- Trigger: Trivy: `eclipse-temurin:21.0.8_9-jre` scan timed out; `21.0.13_9-jre` (same Java 21.0 line) is clean. `postgres:16.10`: 17 Critical / 109 High (OS) plus Go binary findings; `16.15` (PostgreSQL patch release of the same major 16) leaves 2 High (OS) plus the `gosu` findings (D-11).
- Options: 1. `eclipse-temurin:21.0.13_9-jre` and `postgres:16.15` (default).
- Human response: none
- Resolution: 1 — pending review
- Overrides: `tech-stack.md` containers `eclipse-temurin` 21.0.8_9-jre → 21.0.13_9-jre, `postgres` 16.10 → 16.15

## D-10: @grpc/grpc-js 1.9.16 (High) pulled in by the firebase web SDK, no upstream fix
- Timestamp: 2026-11-12T13:30+01:00
- Session: S1 /preflight (run 2)
- Class: block
- Type: blocking
- Trigger: Trivy on the frontend lock file: CVE-2026-101916 (High, authentication bypass via improper peer certificate validation) in `@grpc/grpc-js` 1.9.16, fixed in 1.13.6 / 1.14.5. It is required by `@firebase/firestore` (`~1.9.0`) inside `firebase` 12.4.0; the latest `firebase` 12.19.0 and 13.3.0 still require `~1.9.0`. The product uses only Firebase Authentication in the browser; grpc-js is used only by Firestore under Node.js and is not in a browser bundle.
- Options: 1. Force `@grpc/grpc-js` 1.14.7 with an npm `overrides` entry in the frontend `package.json` (default; nothing we use depends on it). 2. Keep 1.9.16 and lower the finding to Low with the evidence above (no Firestore, browser-only bundle; checked by a test that the production bundle contains no grpc-js). 3. Replace the `firebase` umbrella package with `@firebase/app` + `@firebase/auth` only (different dependencies; needs their pins).
- Human response: none
- Resolution: pending
- Overrides: `tech-stack.md` frontend dependencies (option 1 or 3)

## D-11: Remaining Critical/High findings in the postgres and nginx images
- Timestamp: 2026-11-12T13:30+01:00
- Session: S1 /preflight (run 2)
- Class: block
- Type: blocking
- Trigger: Trivy (`--ignore-unfixed`): `postgres:16.15` — 2 High in Debian `zlib1g` (fixed in `+deb13u1`) and, in `/usr/local/bin/gosu` (Go 1.24.6 stdlib), 1 Critical (CVE-2025-68121 crypto/tls) and 25 High; `postgres:16.15-alpine` has 19 High plus the same `gosu`. `nginx:1.29.8-alpine` (latest 1.29) — 1 Critical, 57 High; `nginx:1.30.6-alpine` (stable line, different minor) — 3 High, all in `libxslt` (fixed 1.1.43-r4), a library only the optional XSLT module uses, which we do not load. `gosu` runs once at container start to drop from root to the `postgres` user and makes no network or TLS connection. The database is never exposed outside the Compose network.
- Options: 1. Use `postgres:16.15` and `nginx:1.30.6-alpine`, each through a small project Dockerfile that runs the distribution's package upgrade at build time (fixes zlib and libxslt), and lower the `gosu` Go-stdlib findings to Low with the evidence above (default). 2. Same images without the package upgrade; lower all remaining image findings to Low with evidence. 3. Other images you name.
- Human response: none
- Resolution: pending
- Overrides: `tech-stack.md` container `nginx` 1.29.2-alpine → 1.30.6-alpine; severity of the listed `gosu` findings (option 1 or 2)

## D-12: Human answers to D-10, D-11
- Timestamp: 2026-11-16T09:40+01:00
- Session: S1 /preflight (run 4)
- Class: allow
- Type: non-blocking
- Trigger: `/preflight` with "i agree with all the defaults for d 10 and d11"; human also created `.env` and put Node.js 22.23.3 first on PATH.
- Options: none (records the answers).
- Human response: D-10 option 1, D-11 option 1; 2026-11-16.
- Resolution: D-10 → npm `overrides` `@grpc/grpc-js` 1.14.7 in the frontend `package.json`. D-11 → `postgres:16.15` and `nginx:1.30.6-alpine` built through project Dockerfiles that run the distribution package upgrade at build time; the `gosu` Go-stdlib findings in the postgres image are lowered to Low with the D-11 evidence (runs once at start to drop privileges, no network/TLS, database not exposed outside the Compose network). Verified by the Trivy re-scan of this run.
- Overrides: `tech-stack.md` frontend dependencies (add override `@grpc/grpc-js` 1.14.7); severity of the `gosu` findings in `postgres:16.15`

## D-13: Critical/High findings in frontend dev tooling only
- Timestamp: 2026-11-16T10:05+01:00
- Session: S1 /preflight (run 4)
- Class: block
- Type: approval
- Trigger: Trivy 0.67.2 `fs` on the frontend lock: product (runtime) dependencies 0 findings (`out/logs/preflight-trivy-frontend-run4.txt`, Trivy default suppresses dev dependencies, as in run 2). With `--include-dev-deps`: 3 Critical, 12 High, all in transitive packages of dev tooling (`out/logs/preflight-trivy-frontend-run4-dev-critical-high.txt`): `tar` 6.2.1 (via firebase-tools, node-gyp; CVE-2026-59873 Critical + 8 High, extraction DoS / file overwrite), `tinypool` 1.1.1 (via vitest 3.2.7; CVE-2026-104848, CVE-2026-104849 Critical), `minimatch` 10.0.3 (via eslint, Stryker, firebase-tools, test-exclude; 3 High ReDoS), `basic-ftp` 5.3.1 (via get-uri; 1 High DoS). None is in the browser bundle or any container image; they run only on the developer machine and in CI on project-controlled input. Fixes need new majors for `tar`, `tinypool`, `basic-ftp`.
- Options: 1. Lower to Medium with this evidence (not shipped, no attacker-controlled input); the phase 0 gate counts product dependencies; re-scan in phase 6 (default). 2. npm overrides to the fixed versions (three new majors; may break firebase-tools or vitest). 3. Block and propose newer tooling pins.
- Human response: option 1; 2026-11-16 (asked with the four packages; the full CVE list above covers the same packages).
- Resolution: 1 — the 15 findings are Medium for this run; phase 6 re-scans with `--include-dev-deps` and re-evaluates if a finding reaches a shipped artifact.
- Overrides: severity of the listed dev-tooling findings

## D-14: `.env` was not git-ignored
- Timestamp: 2026-11-16T10:05+01:00
- Session: S1 /preflight (run 4)
- Class: block
- Type: blocking
- Trigger: the repository has no `.gitignore`; `git check-ignore .env` fails, so `.env` (holds `DB_PASSWORD`) could be committed by `git add -A` (`general/rules/secrets.md`). `.gitignore` is not in the preflight "Writes" list.
- Options: 1. Agent adds `.gitignore` with `.env` and commits it with the preflight report (default). 2. Human adds it. 3. Leave it.
- Human response: option 1; 2026-11-16.
- Resolution: 1 — `.gitignore` created containing `.env`; `git check-ignore .env` succeeds.
- Overrides: preflight "Writes" list, for `.gitignore` only, this once

## D-15: Roles and rights (Owner, Editor, Viewer)
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: rights and access still `OPEN — needs customer`: OQ-P-02, OQ-002-01, OQ-005-01, OQ-005-02, OQ-011-01, OQ-018-01 (Q-07, Q-17, Q-18); BR-03, BR-04 open; `[ASSUMPTION]`s BR-02 (404 rather than 403 for non-Members), exactly one Owner per Group, an Invitation never grants Owner (Data model), R-005-01 (only the Owner sees the Member list), R-005-05 (no handover of ownership), R-012-02 (Viewers do not see the recycle bin). R-016-01 lets every Member, Viewers included, export Contacts' personal data.
- Options: 1. Confirm the model as written: several Groups per User; creator is the only Owner; Owner manages Group, Members, Invitations and permanent delete; Owner and Editor add, change, tag, restore Versions, use the recycle bin, merge and undo; Viewer reads, searches, filters, sees History and exports; only the Owner sees the Member list; no handover in this release; non-Members get 404 (default). 2. As 1, but Viewers may not export. 3. As 1, but Editors and Viewers see the Member list (display names only, no e-mails). 4. Other (state it).
- Human response: none
- Resolution: pending
- Overrides: none

## D-16: Sign-in, e-mail verification, rate limit, verification level
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: OQ-P-05, OQ-001-01, OQ-001-02 `OPEN — needs customer` (Q-34); R-001-05 `[ASSUMPTION]` (unverified e-mail blocks use) and ambiguous: "reject every other request" names no allowed request, so the UI could not even load the User (`GET /api/v1/me`); SR-03 (100 requests per minute) open; ASVS Level 2 `[ASSUMPTION]` (security-requirements).
- Options: 1. E-mail/password and Google; e-mail verification required, and while unverified the backend answers only `GET /api/v1/me` (everything else HTTP 403); 100 requests per minute per User; ASVS 5.0 Level 2 (default). 2. As 1 without e-mail verification (R-001-05 dropped; Invitations then rely on Firebase e-mail ownership only). 3. Other (state it).
- Human response: none
- Resolution: pending
- Overrides: R-001-05 wording (option 1 settles "every other request")

## D-17: Invitation links
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: OQ-004-01 `OPEN — needs customer` (Q-35); `[ASSUMPTION]` R-004-03 (a new Invitation revokes the open one for the same e-mail) and R-004-05 (link bound to the invitee's e-mail); BR-10 does not say whether the automatic revocation of R-004-03, or an accepted Invitation (a new Member), writes an Audit entry.
- Options: 1. Owner only, single use, 7 days, copied by the Owner, bound to the invitee's e-mail (case-insensitive), a new Invitation revokes the open one; Audit entries for creation, every revocation (also automatic) and acceptance (default). 2. As 1, Audit entries only for creation and manual revocation (BR-10 as written). 3. Other (state it).
- Human response: none
- Resolution: pending
- Overrides: BR-10 (option 1 adds automatic revocation and acceptance to the audited actions)

## D-18: Recycle bin and permanent delete
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: OQ-P-03, OQ-003-01, OQ-012-01, OQ-013-01 `OPEN — needs customer` (Q-22, Q-33); project-wide fact "recycle bin period" 30 days OPEN; BR-08 open; `[ASSUMPTION]` R-012-05 (removal at least hourly, so data can live up to 1 hour past the removal date), R-013-03 (permanent delete only from the recycle bin). Gap: while a Group is in the recycle bin, its own Contacts in the recycle bin reach their removal date; R-003-05 promises "all its Contacts as they were", BR-08 deletes them.
- Options: 1. 30 days for Contacts and Groups; permanent delete Owner-only and only from the recycle bin; removal job at least hourly; a Contact's own removal date keeps running while its Group is in the recycle bin (it is erased on time; restoring the Group does not bring it back) (default). 2. As 1, but a Contact's removal clock pauses while its Group is in the recycle bin. 3. Other period or rights (state them).
- Human response: none
- Resolution: pending
- Overrides: none

## D-19: Personal data roles, erasure, retention and audit
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: OQ-P-04 `OPEN — needs customer` (Q-32, Q-33): each Group controller, we processor; permanent delete erases all Versions (SR-06); retention table rows `[ASSUMPTION]` (Invitation e-mails and Audit entries kept until the Group is erased; Audit entries shown to nobody in the UI). BR-10 audited actions open (Q-34, OQ-P-05). Gap: BR-08 lists what a Group's erasure removes but not its Audit entries and Merge records, while the Data model keeps Audit entries "while the Group exists".
- Options: 1. Confirm as written; erasing a Group (BR-08) also removes its Audit entries and Merge records; Audit entries have no UI in this release (default). 2. As 1, but Audit entries outlive the Group for a fixed period (state it). 3. Other (state it).
- Human response: none
- Resolution: pending
- Overrides: none

## D-20: Deleting a User account (US-021, increment 2)
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: OQ-021-01 `OPEN — needs customer` (Q-33); `[ASSUMPTION]` priority `should`, increment 2, R-021-02, R-021-03 ("Deleted user"). Gaps: (a) R-021-01 "delete the User" conflicts with Versions, Audit entries, Groups and Invitations that reference the User id; (b) a Firebase ID token stays valid for up to 1 hour after the Firebase account is deleted, so under R-001-02 and R-001-04 such a token would be accepted and the User created again, contrary to EX-021-03.
- Options: 1. Keep US-021 as written; the User record is anonymised instead of removed (e-mail and display name erased, id and Firebase uid kept, marked deleted); any token of a deleted User gets HTTP 401 and never re-creates the User (default). 2. Drop US-021 from this release. 3. Other (state it).
- Human response: none
- Resolution: pending
- Overrides: R-021-01 ("delete the User" read as anonymise), R-001-04 (no re-creation of a deleted User)

## D-21: Erasing Contacts involved in a Merge (increment 2)
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: gap between US-012, US-013, US-018 and BR-08: a `merged` Contact can never be moved to the recycle bin (R-012-01 needs `active`), so its personal data can be erased only through Undo merge (R-019-02 blocks that after any later change of the Primary contact). A Primary contact with Contacts merged into it can be moved to the recycle bin; R-013-04 then blocks a manual permanent delete, but BR-08 would erase it after 30 days and leave the merged Contacts pointing at nothing.
- Options: 1. Permanently deleting a Primary contact (by the Owner from the recycle bin, or by BR-08) also permanently deletes every Contact merged into it, each with its own Audit entry; R-013-04 is dropped; the confirmation names the merged Contacts (default). 2. Keep R-013-04; BR-08 skips a Primary contact with merged Contacts until the Merge is undone (the data outlives 30 days). 3. Other (state it).
- Human response: none
- Resolution: pending
- Overrides: R-013-04 (option 1)

## D-22: Non-blocking `[ASSUMPTION]`s and story open questions accepted as written
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: DoR-S-07, DoR-S-09: every `[ASSUMPTION]` and `OPEN` item not on a `Block` topic needs a recorded default.
- Options: 1. Use each as written (default): project-wide facts time format, number format, language kept per User, working hours; BR-09; Data model displayName fallback, Group name 1–100 and not unique, Contact Field lengths, e-mail syntax check only, phone characters; R-002-02, R-002-03, R-002-05, R-006-07, R-007-03, EX-007-03, R-008-03, R-008-06, R-009-05, R-011-04, US-014 Page size, columns, default sort and searched Fields, R-015-01 … R-015-04, R-016-03 … R-016-07, R-017-03, R-018-01, R-018-03, R-018-05, R-019-02; NFR-02 data shape, NFR-05; E2E-06 … E2E-08; US-021 in increment 2. Story questions take their proposals: OQ-009-01 no bulk tagging; OQ-015-01 "has Tags" = all; OQ-017-01 no dismissing; OQ-017-02 no local/international phone matching. 2. Name the items to change.
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-23: Contact Fields and saves that the rules leave open
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: (a) "Field" = "fixed data items of a Contact (Data model)", and the Contact form shows "all Fields" (US-006), but the Data model Contact also holds id, groupId, state, deletedAt, mergedInto, versionNumber, created/updated: form, Export columns (R-016-03) and Merge choices (R-018-01) differ with the reading. (b) The Data model restricts phone characters, but no rule or edge case rejects others (US-006, DoR-S-05). (c) Editing, tagging, restoring a Version of, or moving to the recycle bin a `merged` Contact is not covered (US-007 … US-012 cover `inRecycleBin` only).
- Options: 1. (a) Fields = firstName, lastName, email, phone, company, jobTitle, street, postalCode, city, country, note, in that order; (b) a phone with another character is rejected with "Enter a valid phone number." and nothing is stored; (c) every change to a `merged` Contact is rejected with HTTP 409 and nothing changes (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-24: Contact list and Export details
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: (a) R-014-03 "sort by name" with the name column showing "first last": by first or last name? (b) Checked contacts (R-016-02): kept across Pages, search and filter changes? (c) R-016-06 "characters not allowed in file names" names no set. (d) R-016-04 (country as ISO code) and R-016-06 (date `yyyy-mm-dd` in the file name, AC-016-01) differ from the project-wide facts (country shown as a name; dates `dd.mm.yyyy`), which no story may override.
- Options: 1. (a) "name" sorts like R-014-02 (last name, first name, company); (b) Checked contacts are kept across Pages and cleared when search or filters change; (c) `\ / : * ? " < > |` and control characters become `-`; (d) the Export file is a machine-readable exception: keep R-016-04, R-016-06 and AC-016-01 as written (default). 2. As 1, but (d) follows the project-wide facts (country name in the User's language, `dd.mm.yyyy` in the file name), which changes AC-016-01. 3. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: project-wide facts "Date and time format" and "country codes", for the Export file only (option 1 (d))

## D-25: Duplicates, Merge and Undo merge details (increment 2)
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: (a) "Name similarity" (US-017) does not define the trigram measure; with PostgreSQL `pg_trgm` similarity, AC-017-03 ("Janez Novak" / "Janez Novack") scores 0.67 < 0.8 and fails; Sørensen–Dice over the character trigrams of the whole Normalised full name scores 0.84 (computed in this session). (b) R-018-01 "the first Contact" of a pair is undefined. (c) EX-019-01 ("This Merge was already undone.", HTTP 409) has no rule and collides with R-019-02; Undo merge is not covered by BR-06 (stale Version) nor by a Primary contact that was itself merged later. (d) US-018 (8 points) is more than a third of increment 2 (19 points) (DoR-S-02).
- Options: 1. (a) Sørensen–Dice of the character-trigram sets of the two Normalised full names, threshold 0.8; (b) the first Contact is the one first in the R-014-02 order; (c) checks in order: already undone → HTTP 409 "This Merge was already undone."; Primary or Other not `active` → rejected as R-019-03 (recycle bin) or HTTP 409 (merged); stale Version → BR-06; then R-019-02; (d) keep US-018 as one story; `/plan` splits it into tasks (Field choice and Merge; Tags and Attributes) (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-26: How Audit entries and security requirements are verified
- Timestamp: 2026-11-16T10:30+01:00
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: (a) AC-003-03, AC-003-04, AC-004-01, AC-004-06, AC-005-02, AC-005-03, AC-013-01, AC-016-01, AC-018-01, AC-019-01 expect an Audit entry, but Audit entries have no UI or API (security-requirements, Personal data), so they are not observable through a public interface (requirements-standard, Acceptance criteria). (b) DoR-R-05: SR-01 and SR-03 … SR-09 do not say how they are checked.
- Options: 1. (a) Audit entries are checked in backend integration tests through the database, the Operator's interface; (b) SR-01 API tests with no, expired, foreign and tampered tokens; SR-03 the 101st request in a minute gets 429 with `Retry-After`; SR-04 no endpoint changes or deletes an Audit entry (API test, architecture test); SR-05 test of token length, hashed storage, single use, expiry; SR-06 database check after permanent delete and after BR-08; SR-07 Export test (EX-016-03, also tab and carriage return); SR-08 log capture during the acceptance tests contains no Contact data, e-mail or token; SR-09 runtime demonstration on staging (HTTPS only, CORS rejects another origin) (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-27: Human answers to D-15 … D-21
- Timestamp: 2026-11-16T11:00+01:00
- Session: S2 /clarify (run 2)
- Class: allow
- Type: non-blocking
- Trigger: `/clarify D-15: 1, D-16: 1, D-17: 1, D-18: 1, D-19: 1, D-20: 1, D-21: 1.`
- Options: none (records the answers).
- Human response: option 1 for each of D-15, D-16, D-17, D-18, D-19, D-20, D-21; 2026-11-16.
- Resolution: D-15 → roles and rights as written (several Groups per User; creator sole Owner; Owner manages Group, Members, Invitations, permanent delete; Owner and Editor change, tag, restore, use the recycle bin, merge, undo; Viewer reads, sees History, exports; only the Owner sees the Member list; no ownership handover; non-Members get HTTP 404). D-16 → e-mail/password and Google; e-mail verification required, unverified Users may call only `GET /api/v1/me` (else HTTP 403); 100 requests per minute per User; ASVS 5.0 Level 2. D-17 → Invitations Owner-only, single use, 7 days, bound to the invitee's e-mail (case-insensitive), a new one revokes the open one; Audit entries for creation, every revocation (also automatic) and acceptance. D-18 → recycle bin 30 days for Contacts and Groups; permanent delete Owner-only and only from the recycle bin; removal at least hourly; a Contact's removal date keeps running while its Group is in the recycle bin. D-19 → Group is controller, we are processor; permanent delete erases all Versions; Group erasure (BR-08) also removes its Audit entries and Merge records; no Audit UI in this release. D-20 → US-021 kept (increment 2); a deleted User is anonymised (e-mail and display name erased, id and Firebase uid kept, marked deleted); any token of a deleted User gets HTTP 401 and never re-creates the User. D-21 → permanently deleting a Primary contact (Owner or BR-08) also permanently deletes every Contact merged into it, each with an Audit entry; the confirmation names them; R-013-04 dropped.
- Overrides: R-001-05 ("every other request" = all except `GET /api/v1/me`); BR-10 (adds automatic Invitation revocation and Invitation acceptance); BR-08 (adds Audit entries and Merge records of an erased Group; adds Contacts merged into an erased Primary contact); R-021-01 (anonymise instead of delete the User); R-001-04 (no re-creation of a deleted User); R-013-04 (dropped); project-wide fact "recycle bin period" confirmed 30 days; every `OPEN — needs customer` item listed in D-15 … D-21 confirmed

## D-28: Dev-only tools for contract validation, CI syntax and the Maven wrapper
- Timestamp: 2026-11-16T11:30+01:00
- Session: S3 /plan
- Class: allow
- Type: non-blocking
- Trigger: phase 2 needs a parser for the OpenAPI contract, a syntax check for the CI workflow and the Maven wrapper files; `tech-stack.md` lists none of these. Rule: dev-only tooling may be added with an exact version and a non-blocking record (`tech-stack.md` Rules).
- Options: 1. `@redocly/cli` 2.76.0 via `npx` (OpenAPI lint), Python `jsonschema` already on the host (JSON Schema meta-validation), `rhysd/actionlint:1.7.8` image (workflow syntax), `maven:3.9.11-eclipse-temurin-21` image run once to generate the Maven wrapper for Maven 3.9.11 (default). 2. Validate by hand.
- Human response: none
- Resolution: 1 — pending review. None of these ships with the product; versions older than one week chosen on purpose.
- Overrides: none

## D-29: Firebase Auth emulator image for the local stack and tests
- Timestamp: 2026-11-16T11:30+01:00
- Session: S3 /plan
- Class: allow
- Type: non-blocking
- Trigger: environments.md runs the Auth emulator (`firebase-tools` 14.20.0) in Docker Compose; the emulator needs Node.js and Java, and no pinned image provides both. Host port 9099 is held by another container (preflight).
- Options: 1. Project image `02_output/docker/firebase-emulator/Dockerfile`: `eclipse-temurin:21.0.13_9-jre` plus the Node.js 22.23.3 binary copied from `node:22.23.3-bookworm-slim` plus `firebase-tools@14.20.0`; exposed to the browser on host port 9199, to the backend on `firebase-emulator:9099` (default). 2. Run the emulator on the host.
- Human response: none
- Resolution: 1 — pending review. Local and test only, never deployed; scanned with the other images in phase 6.
- Overrides: `tech-stack.md` adds container `node` 22.23.3-bookworm-slim (build stage of the emulator image only)

## D-30: SpotBugs SPRING_CSRF_PROTECTION_DISABLED (High) on the stateless API
- Timestamp: 2026-11-16T12:30+01:00
- Session: S3 /plan
- Class: block
- Type: approval
- Trigger: `./mvnw spotbugs:check` (FindSecBugs 1.14.0): High `SPRING_CSRF_PROTECTION_DISABLED` at `app.contacts.config.SecurityConfig`. The API is stateless (`SessionCreationPolicy.STATELESS`), authenticates only with a Firebase Bearer token in the `Authorization` header (ADR-03) and sets no cookies, so a cross-site request carries no credential. Lowering a High needs approval (`general/quality/severity-scale.md`).
- Options: 1. Lower to Low (false positive) with this evidence; exclude only this pattern on `SecurityConfig` in `spotbugs-exclude.xml`; a skeleton test checks that API responses set no cookie and create no session (default). 2. Enable CSRF tokens for the API.
- Human response: option 1; 2026-11-16 (asked in session).
- Resolution: 1 — excluded in `02_output/backend/spotbugs-exclude.xml` citing D-30; `ApplicationIT.d30ApiSetsNoCookie` keeps the evidence checked. Also excluded (Low): `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` on `SecurityConfig.securityFilterChain`, whose signature follows `HttpSecurity.build() throws Exception`.
- Overrides: severity of SPRING_CSRF_PROTECTION_DISABLED at SecurityConfig (High → Low)

## D-31: Frontend dev packages not listed in tech-stack.md
- Timestamp: 2026-11-16T12:45+01:00
- Session: S3 /plan
- Class: allow
- Type: non-blocking
- Trigger: the frontend build needs type definitions and ESLint flat-config helpers that `tech-stack.md` does not list; `shadcn` 3.5.0 components need `tw-animate-css`. Rule: a dependency not listed may be added with an exact version and a non-blocking record (`tech-stack.md` Rules). `firebase-tools` is installed only in the emulator image (D-29), not in the frontend package. `npm install` notes that `eslint` 9.38.0 is out of upstream support (a pin; replacing it needs a decision).
- Options: 1. Add, dev scope only, versions older than two weeks: `@types/react` 18.3.31, `@types/react-dom` 18.3.7, `@types/node` 22.20.5, `@eslint/js` 9.38.0 (matches `eslint`), `globals` 16.5.0, `tw-animate-css` 1.4.0 (MIT each) (default). 2. Do without them (no type checking of React, no shadcn animations).
- Human response: none
- Resolution: 1 — pending review; scanned in phase 6 with `--include-dev-deps`.
- Overrides: none

## D-32: Local stack name and host ports; emulator image without Java
- Timestamp: 2026-11-16T13:15+01:00
- Session: S3 /plan
- Class: open
- Type: non-blocking
- Trigger: (a) an unrelated Docker Compose project named `contacts` (working directory `…/agentic_lab/matt_workflow`) runs on this host and holds host ports 8080 and 9099; environments.md puts the product at `http://localhost:8080` `[ASSUMPTION port]`. (b) Follow-up to D-29: the Firebase Auth emulator runs on Node.js alone; only the Firestore and Database emulators need Java.
- Options: 1. (a) Compose project `kontakti`; host ports `APP_HOST_PORT` (default 8080) and `EMULATOR_HOST_PORT` (default 9199); while the other stack runs, sessions on this host use `APP_HOST_PORT=8180` and `E2E_BASE_URL=http://localhost:8180`; the other project is never touched. (b) Emulator image = `node:22.23.3-bookworm-slim` + `firebase-tools@14.20.0`, no Temurin layer (default). 2. Stop the other stack (it is not ours).
- Human response: none
- Resolution: 1 — pending review
- Overrides: D-29 (image base), environments.md local port (configurable, default unchanged)

## D-33: gitleaks finding in a frozen input skill file
- Timestamp: 2026-11-16T13:40+01:00
- Session: S3 /plan
- Class: block
- Type: approval
- Trigger: the CI secret scan (gitleaks 8.28.0, rule `generic-api-key`) flags `.claude/skills/firebase-ai-logic-basics/SKILL.md` line 232 (commit `d95d8fb`, before preflight): the documentation example `AppCheck debug token: "123a4567-b89c-12d3-e456-789012345678"`, a placeholder in a Firebase tutorial, not a credential. The file is a frozen input (`AGENTS.md`, Never).
- Options: 1. False positive (Low); allowlist its exact fingerprint in a root `.gitleaksignore`, so any other finding still fails (default). 2. Exclude `.claude/skills/` from the scan.
- Human response: option 1; 2026-11-16 (asked in session).
- Resolution: 1 — `.gitleaksignore` holds the one fingerprint; the scan passes (`out/logs/plan-ci-gitleaks.txt`).
- Overrides: severity of this gitleaks finding (High → Low)

## D-34: Approve wireframes
- Timestamp: 2026-11-16T15:00+01:00
- Session: W /wireframes
- Class: block
- Type: approval
- Trigger: `docs/wireframes/` holds 19 screens (WF-01 … WF-20, no WF-12; index in `docs/wireframes/README.md`) at mobile 375, tablet 768 and desktop 1440 px. Image files: `WF-01_sign-in_desktop.png`, `WF-01_sign-in_mobile.png`, `WF-01_sign-in_tablet.png`, `WF-02_sign-up_desktop.png`, `WF-02_sign-up_mobile.png`, `WF-02_sign-up_tablet.png`, `WF-03_confirm-email_desktop.png`, `WF-03_confirm-email_mobile.png`, `WF-03_confirm-email_tablet.png`, `WF-04_create-group_desktop.png`, `WF-04_create-group_mobile.png`, `WF-04_create-group_tablet.png`, `WF-05_no-groups_desktop.png`, `WF-05_no-groups_mobile.png`, `WF-05_no-groups_tablet.png`, `WF-06_contact-list_desktop.png`, `WF-06_contact-list_mobile.png`, `WF-06_contact-list_tablet.png`, `WF-07_contact-details_desktop.png`, `WF-07_contact-details_mobile.png`, `WF-07_contact-details_tablet.png`, `WF-08_contact-form_desktop.png`, `WF-08_contact-form_mobile.png`, `WF-08_contact-form_tablet.png`, `WF-09_history_desktop.png`, `WF-09_history_mobile.png`, `WF-09_history_tablet.png`, `WF-10_version_desktop.png`, `WF-10_version_mobile.png`, `WF-10_version_tablet.png`, `WF-11_recycle-bin_desktop.png`, `WF-11_recycle-bin_mobile.png`, `WF-11_recycle-bin_tablet.png`, `WF-13_members_desktop.png`, `WF-13_members_mobile.png`, `WF-13_members_tablet.png`, `WF-14_group-settings_desktop.png`, `WF-14_group-settings_mobile.png`, `WF-14_group-settings_tablet.png`, `WF-15_deleted-groups_desktop.png`, `WF-15_deleted-groups_mobile.png`, `WF-15_deleted-groups_tablet.png`, `WF-16_invitation_desktop.png`, `WF-16_invitation_mobile.png`, `WF-16_invitation_tablet.png`, `WF-17_possible-duplicates_desktop.png`, `WF-17_possible-duplicates_mobile.png`, `WF-17_possible-duplicates_tablet.png`, `WF-18_merge_desktop.png`, `WF-18_merge_mobile.png`, `WF-18_merge_tablet.png`, `WF-19_account_desktop.png`, `WF-19_account_mobile.png`, `WF-19_account_tablet.png`, `WF-20_not-found_desktop.png`, `WF-20_not-found_mobile.png`, `WF-20_not-found_tablet.png`.
- Options: 1. Approved with the customer (default). 2. Approved by the developer only. 3. Change: what, per `WF-nn`.
- Human response: none
- Resolution: pending
- Overrides: none

## D-35: Human answer to D-34 (wireframe approval)
- Timestamp: 2026-11-16T15:20+01:00
- Session: W /wireframes (run 2)
- Class: allow
- Type: non-blocking
- Trigger: `/wireframes D-34: 2`.
- Options: none (records the answer).
- Human response: D-34 option 2 (approved by the developer only); 2026-11-16.
- Resolution: wireframes WF-01 … WF-20 (commit `3694d5b`) approved by the developer; not yet confirmed by the customer. They bind `/acceptance-tests`, `/implement` and `/design`; a later change to structure is a decision. Customer confirmation is still open; it should happen at the latest on staging (DoD-P02, DoD-P03).
- Overrides: none

## D-36: Token signature cannot be checked against the Auth emulator
- Timestamp: 2026-11-16T16:00+01:00
- Session: S4 /acceptance-tests
- Class: open
- Type: non-blocking
- Trigger: EX-001-04 ("token with a wrong signature, one character changed → HTTP 401"). The test environment uses the Firebase Auth emulator (environments.md); the emulator issues unsigned tokens and Firebase Admin SDK 9.5.0 skips the signature check whenever `FIREBASE_AUTH_EMULATOR_HOST` is set (`FirebaseTokenVerifierImpl.verifyToken` calls `checkSignature` only outside emulator mode). A correct implementation (ADR-03) would therefore accept a token whose signature part alone was changed. Real Google signing keys need the live service (writing-tests.md forbids it).
- Options: 1. EX-001-04 is tested with an emulator token whose payload has one character changed so it is no longer a valid token (HTTP 401); EX-001-02 with a token whose `aud`/`iss` name another project; AC-001-04 with a token whose `exp` lies in the past; the real signature check (a token signed by another key) is a manual check on staging, listed in the release notes for `/release` (default). 2. Run a second backend without the emulator in the tests and fetch Google's public keys (live service; rejected).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-37: Slovenian wording of the screens is not in the inputs
- Timestamp: 2026-11-16T16:00+01:00
- Session: S4 /acceptance-tests
- Class: open
- Type: non-blocking
- Trigger: UI tests must find elements by the role, label or text the wireframe shows (writing-tests.md); the wireframes show the English text only (`docs/wireframes/README.md`: "the Slovenian catalog uses the same structure"). Before sign-in, and while the e-mail is unverified (D-16: only `GET /api/v1/me`), the UI is Slovenian (project-wide fact "Languages": Slovenian default; R-020-01), so WF-01, WF-02 and WF-03 have no known text. E2E-01 checks "UI is Slovenian" first and switches to English only at its end, so its middle steps would need Slovenian labels.
- Options: 1. UI tests switch each User to English through `PATCH /api/v1/me` before signing in through the UI (R-020-02); on WF-01/WF-02/WF-03 they find the e-mail textbox, the password field and the Google button (by its brand name) without language, submit with Enter, and check messages through their role (`alert`) and outcome, plus the equality of the two messages of EX-001-01; E2E-01 checks the Slovenian UI by the header text "Kontakti" (specification §9.2), switches to English on the Account screen at once ("Slovenščina · English" are language names), checks "Contacts", then runs its other steps in English: the step moves, every observation stays (default). 2. Add a language choice to WF-01/WF-02 (changes approved wireframes; needs approval).
- Human response: none
- Resolution: 1 — pending review
- Overrides: E2E-01 step order (the language switch moves from the last step to right after the Slovenian check)

## D-38: Settings of the test environment
- Timestamp: 2026-11-16T16:00+01:00
- Session: S4 /acceptance-tests
- Class: open
- Type: non-blocking
- Trigger: (a) acceptance tests create up to 120 Contacts per User through the API (AC-014-05) while SR-03 allows 100 requests per minute; (b) AC-016-03 and EX-016-01 need 10,001 and 10,000 Contacts; (c) BR-08 runs in the removal job every 15 minutes (ADR-10), tests cannot wait for it; (d) only Chromium is installed for Playwright on this host and installing software on the host is not allowed; (e) every UI and end-to-end test needs known Users with the e-mails of the scenarios.
- Options: 1. (a) Backend acceptance and hidden tests set `RATE_LIMIT_PER_MINUTE=100000` (a setting of environments.md; the local stack keeps 100); (b) the 10,000/10,001 Contacts are inserted through the persistence contract (`V1__schema.sql`) the way `seed/seed.sql` does; (c) tests set the time through a `java.time.Clock` bean (ADR-10) and then invoke the backend's `@Scheduled` methods, as the scheduler would; (d) browser tests run on Chromium here; Firefox and WebKit run where they are installed (projects stay in `playwright.config.ts`); (e) before each UI and end-to-end test the local stack is emptied (all tables truncated, emulator accounts deleted) and the test creates its own data (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-39: Frozen tests of US-001 set up data through US-002 and US-020
- Timestamp: 2026-11-16T12:40+01:00
- Session: S5 /implement US-001
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01 needs every frozen test of US-001 to pass. Seven of the nine API tests (`Us001SignInAcceptanceIT` AC-001-01 … 04, 06, EX-001-02, EX-001-04) create a Group (`POST /groups`) or list Groups (`GET /groups`, 200) as setup or as part of the assertion; both endpoints are task T-07 of US-002. Four of the six UI tests (AC-001-01, AC-001-06, EX-001-01, EX-001-05) switch the User to English with `PATCH /me` (D-37), which is task T-06 of US-020; AC-001-06 also creates a Group. `/implement` must not build another story's code (skill "Must not"). Run of this session: these tests fail only at those calls (`POST /groups` 404 `notFound`, `PATCH /me` not 200); logs `out/logs/implement-us001-backend-full.txt`, `out/logs/implement-us001-ui-final.txt`.
- Options: 1. Build US-001 to its tasks T-01 … T-05 only; the dependent frozen tests turn green with `/implement US-020` (next) and `/implement US-002`, which re-check them; US-001 keeps status "in progress" until they pass (default). 2. Build the needed parts of T-06 and T-07 now (crosses story boundaries; rejected).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-40: Frozen UI tests find the e-mail field as the only textbox, but Playwright counts the password field as a textbox
- Timestamp: 2026-11-16T12:40+01:00
- Session: S5 /implement US-001
- Class: open
- Type: approval
- Trigger: The frozen helper `frontend/acceptance/support/stack.ts:16` (`signIn`, used by about 28 UI and e2e test files), `acceptance/us001-sign-in.spec.ts:25, 111, 119` and `e2e/e2e-01-sign-up-to-excel.spec.ts:20` fill the e-mail with `page.getByRole("textbox")` (D-37: without language). Playwright 1.56.1 gives `<input type="password">` the implicit role `textbox` (`playwright-core` role table: every `input` type not listed is `textbox`), so on WF-01 and WF-02, which show e-mail and password as the approved wireframes require, the locator resolves to two elements and fails in strict mode ("strict mode violation: getByRole('textbox') resolved to 2 elements"). No accessible form can satisfy it: the only ways are a wrong ARIA role on the password field or hiding the password field until an e-mail is typed (both against UI-19 … 21 and the wireframes). The behaviour itself works: a script that finds the e-mail field by `input[type="email"]` passed AC-001-01 (up to the US-020 step), AC-001-05, AC-001-06, EX-001-01 and EX-001-05 on the local stack (this session).
- Options: 1. Change the five frozen locators to `page.locator('input[type="email"]')` (language-free like the password locator next to them), in one `test:` commit with a new `docs/03_acceptance-manifest.sha256`, made by the session the human names (default). 2. Keep the tests and give the password field an explicit non-textbox role (breaks accessibility; rejected). 3. Other (state it).
- Human response: none
- Resolution: pending (approval needed)
- Overrides: none

## D-41: SpotBugs/FindSecBugs findings that remain in the US-001 code
- Timestamp: 2026-11-16T12:40+01:00
- Session: S5 /implement US-001
- Class: open
- Type: approval
- Trigger: `./mvnw spotbugs:check` (threshold Low, D-30) reports three Low findings after three others were fixed in code (commit `3ee361e`): `SERVLET_HEADER` in `auth/FirebaseTokenFilter` (the ID token must be read from the `Authorization` header; it is verified for signature, project and expiry before use, ADR-03, SR-01); `SE_TRANSIENT_FIELD_NOT_RESTORED` in `service/ValidationException` (service exceptions are never serialised; their data goes only into the HTTP problem); `SPRING_ENDPOINT` in `web/MeController` (informational marker that every controller will get; access is enforced by `SecurityConfig` and `AccessGuard`). Adding the exclusions to `backend/spotbugs-exclude.xml` was refused by the tool's permission check in this session, so the change waits for the human; until then `spotbugs:check` fails (DoD-S-03 open). Log `out/logs/implement-us001-backend-check.txt`.
- Options: 1. Exclude exactly these three patterns (`SERVLET_HEADER` for `FirebaseTokenFilter`, `SE_TRANSIENT_FIELD_NOT_RESTORED` for package `app.contacts.service`, `SPRING_ENDPOINT` for package `app.contacts.web`), each with its reason in `spotbugs-exclude.xml` (default). 2. Exclude only `SERVLET_HEADER` and `SE_TRANSIENT_FIELD_NOT_RESTORED`, and accept a `SPRING_ENDPOINT` exclusion per controller as each story adds one. 3. Other (state it).
- Human response: none
- Resolution: pending (approval needed)
- Overrides: none

## D-42: Texts and limits of US-001 not stated in the inputs
- Timestamp: 2026-11-16T12:40+01:00
- Session: S5 /implement US-001
- Class: open
- Type: non-blocking
- Trigger: (a) Firebase answers `auth/weak-password` for a password below its default rule (US-001 "Out of scope": Firebase defaults); the story has no message for it. (b) A token without a name gives the display name "e-mail local part" (specification §4); a name longer than 100 characters does not fit `app_user.display_name` (`V1__schema.sql`, `Me.displayName` maxLength 100). (c) The Slovenian texts of WF-01, WF-02, WF-03 and WF-05 are not in the inputs (D-37; specification §5: "Slovenian texts are written with the screen").
- Options: 1. (a) [ASSUMPTION] "The password must have at least 6 characters." / "Geslo mora imeti vsaj 6 znakov." (Firebase's default minimum); (b) cut the display name to its first 100 characters; (c) Slovenian texts as in `frontend/src/i18n/sl.json`, the Google button keeps the brand name "Google" (D-37) (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-43: Answers to D-40 and D-41
- Timestamp: 2026-11-16T13:10+01:00
- Session: S5 /implement US-001 (resumed)
- Class: allow
- Type: non-blocking
- Trigger: developer answer "d-40 a and d-41 a" (a = the first, default option).
- Options: none (records the answer).
- Human response: D-40 option 1, D-41 option 1; 2026-11-16.
- Resolution: D-40 → the five frozen e-mail locators (`acceptance/support/stack.ts:16`, `acceptance/us001-sign-in.spec.ts:25, 111, 119`, `e2e/e2e-01-sign-up-to-excel.spec.ts:20`) become `page.locator('input[type="email"]')`, nothing else in the tests changes, and `docs/03_acceptance-manifest.sha256` is regenerated, in one `test:` commit of this session. D-41 → `backend/spotbugs-exclude.xml` excludes exactly `SERVLET_HEADER` (`auth.FirebaseTokenFilter`), `SE_TRANSIENT_FIELD_NOT_RESTORED` (package `app.contacts.service`) and `SPRING_ENDPOINT` (package `app.contacts.web`), each with its reason.
- Overrides: frozen test files listed above (approved change, D-40)

## D-44: Frozen UI tests of US-020 set up data through US-002 and need screens of US-014 and US-010
- Timestamp: 2026-11-16T14:25+01:00
- Session: S5 /implement US-020
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01 needs every frozen test of US-020 to pass. The three API tests pass (`Us020LanguageAcceptanceIT`, log `out/logs/implement-us020-backend-full.txt`). The four UI tests (`acceptance/us020-language.spec.ts`) create a Group (`POST /groups`, task T-07 of US-002) and then read the Contact list heading and count (WF-06, US-014: AC-020-01, AC-020-02, EX-020-02) or the History (WF-09, US-010: AC-020-03). This session they fail only at `POST /groups` (404 `notFound`; log `out/logs/implement-us020-ui-acceptance.txt`). The US-020 behaviour they reach afterwards was checked on the local stack with a throwaway script (switch without reload, `<html lang>`, kept after signing out and in, axe clean in both languages; screens `out/logs/screens/WF-05_no-groups/language-menu/`), and the formatters by unit tests (`src/i18n/format.test.ts`).
- Options: 1. Build US-020 to task T-06 only; US-020 stays "in progress" until `/implement US-002`, `/implement US-014` and `/implement US-010` re-run `us020-language.spec.ts` and set it to done when it passes (as D-39) (default). 2. Build the list and History screens now (crosses story boundaries; rejected).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-45: Details of the language choice not stated in the inputs
- Timestamp: 2026-11-16T14:25+01:00
- Session: S5 /implement US-020
- Class: open
- Type: non-blocking
- Trigger: R-020-02 names the user menu; specification §4 also lists WF-19 Account (US-020, US-021), but task T-06 lists only `F/app/UserMenu.tsx`. The inputs give no wording for the language group, no feedback for a failed save, no English number locale and no language for the screens after signing out.
- Options: 1. [ASSUMPTION] (a) the language is chosen in the user menu only; the language section of WF-19 Account is built with the Account screen in US-021; (b) the items are named in their own language, "Slovenščina" and "English", in both catalogs, grouped as "Jezik" / "Language", the chosen one marked (`aria-pressed`, check icon); (c) when `PATCH /me` fails the language stays and the menu shows "Nekaj je šlo narobe." / "Something went wrong." (`state.error`); (d) English numbers use `en-GB` (`10,000`, as the project-wide facts); dates use `dd.mm.yyyy HH:mm` in Europe/Ljubljana in both languages; (e) after signing out, the public screens keep the last language of the tab (they are language-free in the tests, D-37) (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-46: Frozen tests of US-002 need Contacts, the Contact list and the Group recycle bin
- Timestamp: 2026-11-16T15:25+01:00
- Session: S5 /implement US-002
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01 needs every frozen test of US-002 to pass. API `Us002GroupsAcceptanceIT` passes 7/10: AC-002-01 reads the Contact list (`GET /groups/{id}/contacts`, T-11 of US-014), AC-002-02 adds Contacts (`POST /groups/{id}/contacts`, T-09 of US-006), EX-002-06 moves the Group to the recycle bin (`POST /groups/{id}/move-to-recycle-bin`, US-003); each fails only at that call (404 `notFound`). UI `us002-groups.spec.ts` passes 2/5 (AC-002-04, AC-002-05): AC-002-01 fails only at "No Contacts yet." (WF-06 empty state, T-12 of US-014), AC-002-02 at `POST …/contacts`, EX-002-06 at `move-to-recycle-bin`. Logs `out/logs/implement-us002-backend-full.txt`, `out/logs/implement-us002-ui-acceptance.txt`. R-002-04 needs a route for the Contact list of a Group, but WF-06 is task T-12 of US-014.
- Options: 1. Build US-002 to T-07 and T-08 only; `/groups/:groupId/contacts` shows the WF-06 heading ("Kontakti" / "Contacts", `ContactListPlaceholder` in `F/app/pages.tsx`) until US-014 replaces it with `ContactListPage`; US-002 stays "in progress" until `/implement US-006`, `/implement US-014` and `/implement US-003` re-run both US-002 suites and set it to done when they pass (as D-39, D-44) (default). 2. Build the Contact endpoints, WF-06 and the Group recycle bin now (crosses story boundaries; rejected).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-47: Details of creating and switching Groups not stated in the inputs
- Timestamp: 2026-11-16T15:25+01:00
- Session: S5 /implement US-002
- Class: open
- Type: non-blocking
- Trigger: WF-04, the Group switcher of WF-06 and R-002-02 … R-002-05 leave open: how the length of a name is counted, the storage key of the Group selected last, the switcher's accessible name, its contents outside a Group and without Groups, the answer to a malformed Group id, and the Slovenian wording.
- Options: 1. [ASSUMPTION] (a) the name length counts characters (Unicode code points, as PostgreSQL `varchar(100)`), checked in the browser before sending and on the server; (b) the Group selected last is kept in `localStorage` under `contacts.lastGroup.<User id>` (specification §4 US-002, D-22); blocked storage only forgets it; (c) the switcher button is named "Group switcher: <name>" / "Izbira skupine: <name>" and shows the name, its list holds links to the Groups in the order of `GET /groups` (current one marked with `aria-current` and a check), then "Create a Group"; outside a Group it shows the Group selected last (else the first); without Groups it is hidden, as the WF-05 header draws it; (d) an id in the path that is not a UUID is answered 404 `notFound` like any unknown item (BR-02), not 500; (e) Slovenian texts: "Ustvari skupino", "Ime skupine", "1 do 100 znakov, npr. projekt ali podjetje.", "Prekliči", "Vnesite ime z 1 do 100 znaki."; (f) on mobile the header wraps as WF-04 mobile: logo and switcher, then menu button and user menu (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-48: The user menu stays open after choosing a language (US-020)
- Timestamp: 2026-11-16T15:25+01:00
- Session: S5 /implement US-002
- Class: open
- Type: non-blocking
- Trigger: With Groups available, the frozen UI test AC-020-02 (`us020-language.spec.ts`) now gets past `POST /groups` and switches to English, but then `signOut` (`acceptance/support/stack.ts`) clicks the "Ana Kovač" button to open the user menu, which is still open after the language choice, so the click closes it and "Sign out" is not found (timeout; page snapshot in the run of `out/logs/implement-us002-ui-acceptance.txt`). The defect is in `F/app/UserMenu.tsx` (task T-06 of US-020): choosing a language does not close the menu. `/implement US-002` must not change another story's code.
- Options: 1. Fix it in US-020's scope: choosing a language closes the user menu (like Sign out), as its own `fix:` commit with a unit test in `UserMenu.test.tsx`, done by the next session that works on US-020 (`/implement US-014` or `/implement US-010`, which re-run `us020-language.spec.ts`, D-44) or by `/fix` after `/review INC-1` (default). 2. Fix it now in US-002 (crosses story boundaries; rejected).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-49: Frozen tests of US-006 set up Roles through Invitations and read the Contact list, details and History
- Timestamp: 2026-11-16T16:18+01:00
- Session: S5 /implement US-006
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01 needs every frozen test of US-006 to pass. Both suites build their team with `team()` (`AcceptanceTest.team`, `acceptance/support/api.ts`), which makes Marko Editor and Petra Viewer through `POST /groups/{id}/invitations` and `POST /invitations/{token}/accept` (US-004); this session all 15 API tests (`Us006AddContactAcceptanceIT`) and all 5 UI tests (`us006-add-contact.spec.ts`) fail there (404 `notFound`), before any US-006 call (logs `out/logs/implement-us006-backend-full.txt`, `out/logs/implement-us006-ui-acceptance.txt`). After the setup they also read the Contact list (`GET …/contacts`, WF-06: US-014), one Contact (`GET …/contacts/{id}`: US-014 R-014-06) and the History (`GET …/versions`, WF-09: US-010). The US-006 behaviour itself was checked on the local stack with throwaway scripts that set the Roles by SQL: every AC and EX of the API (`out/logs/implement-us006-api-check.txt`) and the UI steps of AC-006-01, -03, -04, -05 (`out/logs/implement-us006-ui-check.txt`); unit and integration tests cover each rule.
- Options: 1. Build US-006 to T-09 and T-10 only; US-006 stays "in progress" until `/implement US-014`, `/implement US-010` and `/implement US-004` re-run both US-006 suites and set it to done when they pass (as D-39, D-44, D-46) (default). 2. Build Invitations, the Contact list and History now (crosses story boundaries; rejected).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-50: Details of adding a Contact not stated in the inputs
- Timestamp: 2026-11-16T16:18+01:00
- Session: S5 /implement US-006
- Class: open
- Type: non-blocking
- Trigger: R-006-01 … R-006-07, WF-08, specification §3 to §5 and the contract leave open: the error field of R-006-02, an unknown Field name in the body, how `search_text` joins its Fields, the name of a ContactRef, when and how the e-mail warning is shown, where a saved Contact leads, how the country is chosen and named, how lengths are counted, and the Slovenian wording.
- Options: 1. [ASSUMPTION] (a) the problem of R-006-02 has `field` = `name` (the Name parts as a whole); every invalid Field gets its own error at once, a Field longer than its maximum only `field.tooLong`; (b) an unknown Field name or a body without `fields` → 400 `validation` without errors (contract `additionalProperties: false`); `attributes` and `tags` are read from US-008/US-009 on; (c) `search_text` joins the Normalised Fields with spaces, as `seed/seed.sql` does; lengths count characters (code points, as PostgreSQL `varchar(n)`), trimming uses Unicode white space; (d) ContactRef `name` = first and last name joined by a space, else company; several matches of R-006-06 are named in one warning, oldest first, joined by ", "; (e) the e-mail is looked up when the User leaves its box, only when it is a valid address; `excludeContactId` is built with the edit form (US-007); (f) after saving, WF-08 opens the Group's Contact list (the new Contact is listed there; WF-07 details come with US-014); (g) the country is a native select of the ISO 3166-1 alpha-2 codes of `java.util.Locale.getISOCountries()` (the list the API checks), named by `Intl.DisplayNames` in the User's language and sorted by its collation, empty by default; (h) numbers in hints and errors use the formatter of R-020-03 (Slovenian CLDR leaves four digits ungrouped: "2000", English "2,000"); (i) Slovenian texts: "Dodaj kontakt", "Podatki", "Ime", "Priimek", "E-pošta", "Telefon", "Podjetje", "Delovno mesto", "Ulica", "Poštna številka", "Kraj", "Država", "Opomba", "Največ {{max}} znakov", "Števke, presledki in + - ( ) /", "Vnesite vsaj ime, priimek ali podjetje.", "Izberite …", "Shrani", "Prekliči", "Vnesite ime, priimek ali podjetje.", "{{field}} ima lahko največ {{max}} znakov.", "Vnesite veljavno telefonsko številko.", "Kontakt s tem e-poštnim naslovom že obstaja: {{name}}."; (j) the method the specification calls `VersionWriter.write` is named `record`, because SpotBugs takes a `write…` method as a sign of a mutable class (EI_EXPOSE_REP2 on every service holding it); (k) WF-08 for adding loads no data, so its loading and empty states (UI-14) come with the edit form (US-007); a failed save is its error state (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none

## D-51: The frozen UI test EX-020-02 stubs the page itself, not only the API
- Timestamp: 2026-11-16T17:53+01:00
- Session: S5 /implement US-014
- Class: open
- Type: approval
- Trigger: `acceptance/us020-language.spec.ts` EX-020-02 calls `stubGet(page, `/groups/${groupId}/contacts`, …)` (`acceptance/support/stack.ts`), which fulfils every GET whose path ends with that suffix, and then `page.goto(listPath(groupId))`, whose path is the same `/groups/{id}/contacts`. The browser's navigation to the Contact list is therefore answered with the stubbed JSON instead of the app (screenshot `test-results/…EX-020-02…/test-failed-1.png` in the run of `out/logs/implement-us014-ui-acceptance.txt`), so "10.000" can never appear, whatever the app does. The behaviour itself is covered: `ContactListPage.test` "UI-18 the count is a number of the language, with its plural" shows "10.000 kontaktov" and "1 Contact". A frozen test may only change with the human's approval (`general/rules/decisions.md`).
- Options: 1. Approve a change of the frozen helper `stubGet` to fulfil only API calls (`route.request().resourceType()` is `fetch` or `xhr`), re-hashing `docs/03_acceptance-manifest.sha256` in the same commit (default). 2. Approve a change of EX-020-02 only: stub `/api/v1/groups/{id}/contacts`. 3. Keep the test; EX-020-02 stays failing and US-020 cannot reach DoD-S-01.
- Human response: none
- Resolution: pending review
- Overrides: none

## D-52: Frozen tests and details of the Contact list (US-014) not stated in the inputs
- Timestamp: 2026-11-16T17:53+01:00
- Session: S5 /implement US-014
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01: all 12 API tests (`Us014ContactListAcceptanceIT`) and 5 UI tests (`us014-contact-list.spec.ts`) build their team with Invitations (US-004) and fail there (logs `out/logs/implement-us014-backend-full.txt`, `implement-us014-ui-acceptance.txt`); after that they move a Contact to the recycle bin (US-012) and save Tags and Attributes with the Contact (US-008, US-009). The steps were run on the stack with that data set up by SQL: every API case and the UI steps of AC-014-01, -02, -04, EX-014-02 and R-014-06 pass (throwaway; results in `out/logs/implement-us014-api-check.txt`, `implement-us014-ui-check.txt`). The rules leave open further details listed in option 1.
- Options: 1. [ASSUMPTION] (a) US-014 stays "in progress" until `/implement US-004`, `US-008`, `US-009` and `US-012` re-run both US-014 suites (as D-39, D-46, D-49); (b) sorting by company, e-mail or last change orders ties by name; "desc" reverses only the chosen key (for name all three name keys); empty company or e-mail sort last in both orders; e-mails compare in lower case; (c) the search runs over the stored search text, so a search may span two neighbouring Fields (seed.sql's form, D-50 (c)); (d) an unknown `sort` or `dir`, a `page` below 1 or not a number → 400 `validation` without errors; `q` over 200 characters → `field.tooLong` on `q`; a mistyped id in the path stays 404 (D-47 (d)); (e) `GET /contacts/{contactId}` answers a Contact in any state, with its Attributes by key A–Z (sl-x-icu) and Tags by name; the list shows each row's Tags by name; (f) the search box asks 300 ms after the last key; search, sort and order start again at Page 1; the query lives in the URL; Viewers get no "Add a Contact", which on mobile is a bar at the bottom; (g) details show "—" for an empty Field, the country by name, "Version n · dd.mm.yyyy HH:mm" and "No Attributes." / "No Tags."; History links to WF-09 (`…/history`, US-010; not found until then); (h) a 4xx answer is final for every read of the app (`retryUnlessClientError` in `main.tsx`), so "not found" shows at once; (i) the list endpoint answers the service views, which have the contract's shape; (j) catalogs.test compares messages with plural suffixes folded and requires each language's plural forms (Intl.PluralRules); (k) Slovenian texts: "Ime" (Name column), "Podjetje", "E-pošta", "Telefon", "Značke", "Iskanje", "Ime, e-pošta, telefon, podjetje ali kraj", "Razvrsti po", "Zadnja sprememba", "Vrstni red", "Naraščajoče", "Padajoče", "Dodaj kontakt", "{{formatted}} kontakt/kontakta/kontakti/kontaktov", "Ni še kontaktov.", "Noben kontakt se ne ujema.", "Počisti iskanje", "Strani", "Prejšnja", "Naslednja", "Stran {{page}} od {{pages}}", "Zgodovina", "Atributi", "Ni atributov.", "Ni značk.", "Različica {{number}} · {{at}}" (default). 2. Other (state it).
- Human response: none
- Resolution: 1 — pending review
- Overrides: none
