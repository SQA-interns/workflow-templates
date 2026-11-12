# Preflight report

> For: agent (writes), human (reads) · Written in: phase 0 (`/preflight`) · Source: `project/00_setup/*` · Procedure: `skills/preflight` · Agent: writes

Run 2: 2026-11-12, session S1. **Gate not passed.** Run 1 decisions D-01 … D-06 were answered (D-07); run 2 added D-08 … D-11 (`docs/decisions-log.md`). Versions below are the pins after those decisions.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass for this session's scope: "Pre-approved findings" is empty. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | **Fail (Node on PATH only).** Java 21.0.13 (D-01). Node.js 22.23.3 (D-07) runs from `~/.nvm/versions/node/v22.23.3/bin`, but `node` on the PATH of non-interactive bash, login bash and zsh is still 18.19.1; nvm is not loaded there, so `nvm alias default` alone does not help. Trivy 0.67.2, gitleaks 8.28.0, k6 1.3.0 run from their images (D-07). npm tools resolve and run with Node 22.23.3 (firebase-tools 14.20.0, Playwright 1.56.1 checked). Maven 3.9.11 (wrapper) resolves. |
| Browser test runner starts and saves a screenshot | `tech-stack.md` (`@playwright/test`) | Pass. Playwright 1.56.1 with Chromium 141.0.7390.37 saved `out/logs/preflight-screenshot.png` (1440 × 900); second method `playwright screenshot` CLI also succeeded. |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker daemon reachable; Firebase Auth emulator (firebase-tools 14.20.0, project `demo-contacts`) started, answered `GET /` and `accounts:signUp` with 200, and was stopped. Note: host port 9099 is held by an unrelated container `contacts-auth-1`; the emulator was checked on 9199. The Compose stack must not publish 9099 on the host while that container runs. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | **Fail.** `.env` does not exist (checked twice with `test -f`), so `DB_PASSWORD` is missing. `FIREBASE_SERVICE_ACCOUNT_FILE` is "not needed until increment 2". |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. Spring Boot 4.1.2 starters and companions (D-08), all npm pins (incl. D-02, D-07), and images `postgres:16.15`, `eclipse-temurin:21.0.13_9-jre`, `nginx:1.30.6-alpine` resolve. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Fail.** Trivy 0.67.2 (`fs` on throw-away manifests with the full resolved tree; `image` with `--ignore-unfixed`): backend 0 findings; frontend 1 High (`@grpc/grpc-js` via `firebase`, D-10); `eclipse-temurin:21.0.13_9-jre` 0; `postgres:16.15` 2 High + `gosu` 1 Critical / 25 High; `nginx:1.30.6-alpine` 3 High (D-11). Logs: `out/logs/preflight-trivy-*.txt`. |
| Clean working tree on the starting commit | repository | Pass: `git status --short` empty at `32873a0`. |
| Input manifest written | `docs/00_input-manifest.sha256` | Not written: the gate did not pass. |

Methods (no values shown): version commands in `bash -c`, `bash -lc` and `zsh -c`; tools in containers with `docker run --rm <image> --version`; Playwright via a script and via its CLI; emulator via HTTP probes. Resolution: HTTP HEAD on Maven Central `.pom`, `npm view`, `npm install --package-lock-only --ignore-scripts` in the scratchpad, `docker manifest inspect`, Docker Hub tag listing. Vulnerabilities: Trivy (primary), OSV API (run 1, second source). `.env`: existence test only; no value read.

## Dependency results

Entries after D-07 … D-09. Every entry not listed resolves with no Critical or High finding.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot starters (webmvc, validation, data-jpa, security, actuator, flyway, test slices, testcontainers) | 4.1.2 | yes | none (Trivy on the full tree) |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 3.2.0 | yes | none |
| com.google.firebase:firebase-admin, poi-ooxml, bucket4j, archunit | as listed | yes | none |
| firebase (→ @firebase/firestore → @grpc/grpc-js 1.9.16) | 12.4.0 | yes | High (CVE-2026-101916; D-10) |
| react-router / zod / vite / vitest | 7.18.5 / 4.6.7 / 7.3.8 / 3.2.7 | yes | none |
| firebase-tools (dev; its own @grpc/grpc-js 1.14.7) | 14.20.0 | yes | none |
| eclipse-temurin (container) | 21.0.13_9-jre | yes | none |
| postgres (container) | 16.15 | yes | Critical (`gosu`, Go stdlib CVE-2025-68121); High (`zlib1g`, `gosu`) — D-11 |
| nginx (container) | 1.30.6-alpine | yes | High (`libxslt`, 3) — D-11 |
| trivy / gitleaks / k6 (tool images) | 0.67.2 / v8.28.0 / 1.3.0 | yes | not part of the product |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-11-12 run 1 | Node.js 22 on PATH (D-03) | D-03: 1 (Node 22.23.3) | Run 2: approved version runs; still not first on PATH |
| 2026-11-12 run 1 | Trivy, gitleaks, k6 (D-04) | D-04: 1 (images) | Run 2: pass |
| 2026-11-12 run 1 | `.env` with `DB_PASSWORD` | not yet | Run 2: still missing |
| 2026-11-12 run 1 | Backend line (D-05), frontend bumps (D-06) | D-05: 1, D-06: 1 | Run 2: backend clean; frontend D-10 |
| 2026-11-12 run 2 | Node 22.23.3 first on PATH in non-interactive shells | | Node version in `bash -c` and `zsh -c` |
| 2026-11-12 run 2 | `.env` with `DB_PASSWORD` | | Secret presence |
| 2026-11-12 run 2 | D-10 (grpc-js), D-11 (images) | | Trivy re-scan of the chosen option |
