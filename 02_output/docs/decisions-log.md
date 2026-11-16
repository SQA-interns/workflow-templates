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
