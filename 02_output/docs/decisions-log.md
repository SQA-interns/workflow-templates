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
