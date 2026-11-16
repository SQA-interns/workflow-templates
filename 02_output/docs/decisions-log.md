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
