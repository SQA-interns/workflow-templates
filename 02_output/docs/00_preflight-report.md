# Preflight report

> For: agent (writes), human (reads) · Written in: phase 0 (`/preflight`) · Source: `project/00_setup/*` · Procedure: `skills/preflight` · Agent: writes

Run 1: 2026-11-12, session S1. **Gate not passed.** Decisions D-01 … D-06 in `docs/decisions-log.md`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass for this session's scope: "Pre-approved findings" is empty (none apply). |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | **Fail.** Java runs as 21.0.13 (D-01, non-blocking). Node.js on PATH is 18.19.1 (pin 22.20.0; D-03). `trivy`, `gitleaks`, `k6` not installed (D-04). Maven: no host install needed; the wrapper's Maven 3.9.11 resolves on Maven Central. npm-based tools (vite, eslint, vitest, stryker, playwright, firebase-tools, …) are project-local and resolve; they cannot run until D-03 is decided. |
| Browser test runner starts and saves a screenshot | `tech-stack.md` (`@playwright/test`) | **Not run:** needs Node.js ≥ 18 with the stack's engines (vite needs ≥ 20.19); waits on D-03. |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Partial. Docker daemon reachable (server 29.3.1, Compose v5.1.1); the `postgres:16.10` image resolves. Firebase Auth emulator (`firebase-tools`, needs Node ≥ 20) **not run**, waits on D-03. The local stack itself does not exist yet (built from `/implement` on). |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | **Fail.** `.env` does not exist, so `DB_PASSWORD` (marked "provided") is missing. `FIREBASE_SERVICE_ACCOUNT_FILE` is marked "not needed until increment 2" and was not checked. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. All 25 Maven coordinates, 38 npm packages and 3 container images resolve at the pinned version. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Fail.** The pinned dependency-scan tool (Trivy) is not installed (D-04). OSV database lookup of the direct dependencies and the main Spring-managed artifacts: Critical/High in Spring Boot 3.5.7 (actuator, managed Tomcat 10.1.48, Spring Framework 6.2.12, Spring Security 6.5.6, pgjdbc 42.7.8), react-router 7.9.4, zod 4.1.12, vite 7.1.12, vitest 3.2.4 (D-02, D-05, D-06). Transitive npm packages are not checked yet (no lock file). Full list: `out/logs/preflight-osv-2026-11-12.txt`. |
| Clean working tree on the starting commit | repository | Pass: `git status --short` empty at `d95d8fb`; `.env` absent. |
| Input manifest written | `docs/00_input-manifest.sha256` | Not written: the gate did not pass. |

Methods (no values shown): versions with each tool's version command in a non-login `bash -c`; second method for every failure: `type -a`, `command -v` in a login shell, nvm's version list and common install folders. Resolution: HTTP HEAD on the Maven Central `.pom`, `npm view <pkg>@<version> version` (npm 10.9.9 from Node 22.23.3 via nvm), `docker manifest inspect`. Vulnerabilities: OSV `querybatch` API, severity from each advisory's `database_specific.severity`, mapped as `general/quality/severity-scale.md` (MODERATE = Medium). `.env`: existence test only.

## Dependency results

Only entries with an advisory or a decision are listed separately; every other entry resolves and has no OSV advisory at its pinned version.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-* (8 entries) | 3.5.7 | yes | High (actuator auth bypass GHSA-8hfc-fq58-r658, GHSA-mgvc-8q2h-5pgc; fixed 3.5.12) |
| ↳ managed org.apache.tomcat.embed:tomcat-embed-core | 10.1.48 | yes | Critical (8 advisories; the last fixed in 10.1.59; 10.1.61 has none) |
| ↳ managed org.springframework:spring-webmvc | 6.2.12 | yes | Critical (GHSA-j9f9-w8pj-32f8, GHSA-pc63-qcmh-9cmg: fixed only in 7.0.9; still present in 6.2.19, the last 6.2 release) |
| ↳ managed org.springframework.security:spring-security-web | 6.5.6 | yes | Critical (GHSA-mf92-479x-3373; fixed 6.5.9) |
| ↳ managed org.postgresql:postgresql | 42.7.8 | yes | High (fixed 42.7.12) |
| ↳ managed org.apache.commons:commons-lang3 | 3.17.0 | yes | Medium |
| com.google.firebase:firebase-admin | 9.5.0 | yes | none found |
| org.apache.poi:poi-ooxml | 5.4.1 | yes | none found |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 2.8.13 | yes | none found |
| react-router | 7.9.4 | yes | High (8 advisories; fixed by 7.18.2; 7.18.5 latest 7.x) |
| zod | 4.1.12 | yes | High (GHSA-mrhx-c7xc-m3w5; fixed 4.6.6) |
| vite | 7.1.12 | yes | High (dev server; fixed 7.3.7; 7.3.8 latest 7.3.x) |
| vitest, @vitest/coverage-v8 | 3.2.4 | yes | Critical (GHSA-5xrq-8626-4rwp, UI server; fixed 3.2.6) |
| postgres (container) | 16.10 | yes | not scanned (needs Trivy, D-04) |
| eclipse-temurin (container) | 21.0.8_9-jre | yes | not scanned (needs Trivy, D-04) |
| nginx (container) | 1.29.2-alpine | yes | not scanned (needs Trivy, D-04) |
| apache-maven (wrapper) | 3.9.11 | yes | none found |
| all other Maven and npm entries | as listed | yes | none found |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-11-12 | Node.js 22 on PATH (D-03) | | Node version, Playwright screenshot, Firebase emulator, npm tool versions |
| 2026-11-12 | Trivy, gitleaks, k6 (D-04) | | Tool versions, dependency scan |
| 2026-11-12 | `.env` with `DB_PASSWORD` | | Secret presence |
| 2026-11-12 | Backend stack line (D-05), frontend bumps (D-06) | | Resolution and scan of the approved versions |
