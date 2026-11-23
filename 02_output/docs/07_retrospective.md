# Retrospective

> For: agent (writes), human (reads) · Written in: phase 7 (`/release`) · Source: `docs/decisions-log.md`, `docs/04_review-*.md`, `docs/06_verification-report.md`, `docs/00_progress.md` · Agent: writes

## What went well

- **The judges agreed.** Held-out tests passed 123/123 at the first hidden evaluation of the final code (`docs/06_hidden-evaluation.md` Run 2). The frozen acceptance and input manifests stayed unchanged from the freeze to the release (`sha256sum -c`, DoD-R-01).
- **Strong test suite.** Line coverage is 99.53 % (backend) and 99.95 % (frontend); mutation scores are 97.07 % and 94.87 %, against a 90 % threshold. 302 of the 324 R/AC/EX ids are named in a test, and the other 22 are covered through their AC/EX rows (`out/logs/s9-traceability.txt`).
- **NFRs with room to spare.** List p(95) is 93 ms (limit 500 ms), Export of 10,000 rows takes 1.13 s (limit 10 s), and the backend recovers in 13 s (limit 60 s). The one capacity failure, NFR-03 at 3.71 s with 50 Groups, was found by the planned load run and fixed before review (D-82).
- **One fix loop was enough.** Verification Run 1 found 12 findings (2 High, 3 Medium, 7 Low). One `/fix` session closed 11 and accepted F-46 (D-93); Run 2 found nothing new. Neither review had a Critical or High finding (INC-1: 14 Medium, 20 Low; INC-2: 6 Medium, 22 Low).
- **Decisions were written down, not guessed.** 98 records; every blocking one got a human answer in a later record (D-07, D-12, D-27, D-35, D-43, D-65, D-91, D-95, D-97). Personal-data topics (D-19, D-20, D-21) waited for the human.

## What held the run back

- **The host was not ready for what the gates measure.** Node on `PATH` was 18 instead of 22 (D-03, D-07). Trivy, gitleaks and k6 were missing (D-04). Firefox and WebKit were missing at verification (D-90), then WebKit's system library (D-94); together that cost an extra human round and a second verification run. The host ran out of memory during the mutation runs and the JDK upgraded itself mid-run (Run 2 notes, D-96). Docker stopped during `/implement US-018` (D-73).
- **The supply chain moved under the pins.** Critical and High advisories at preflight (D-02, D-05, D-06, D-10, D-11) needed four preflight rounds. Two High findings in the images (F-35, F-36) appeared only at verification, because the CI scan reads `pom.xml` and `package-lock.json` but not the built images. Caddy was dropped at release for the same reason (D-97).
- **Frozen tests depended on other stories.** Acceptance tests of one story set up data or opened screens of stories not built yet (D-39, D-44, D-46, D-49, D-68, D-71). Some used locators or page stubs that broke once the real screens existed (D-40, D-51, D-64, D-77). Each case needed a decision, and three needed a human approval (D-43, D-65, D-69). DoD-S-01 of 15 stories could only be met after US-004, US-018 or D-65 (`docs/00_progress.md`).
- **The inputs were silent on details.** About 25 records exist only because a story left its texts, limits or edge behaviour open: D-37, D-42, D-45, D-47, D-50, D-52 … D-62, D-66, D-70, D-72, D-75, D-76, D-81, D-83. Most of the 62 review findings are of the same kind: UI states, copy and limits.
- **Commit discipline slipped under pressure.** DoD-S-07 has exceptions in 14 stories: subjects over 72 characters, commits over 400 lines, commits that failed a check. Seven stories have no `feat` commit naming the `US` id (F-46, D-93). 375 of the 484 commits landed on 2026-11-16 and 17.
- **Open customer questions reached the release.** Q-39 (host, operator) is still open, so DoD-P02, DoD-P05, the staging part of DoD-P03 and TLS remain manual tests. Q-32 and Q-33 (retention) are still assumptions (`docs/release-notes.md`).
- **No runner logs.** The sessions were started by hand, so `system/run.mjs --summary` reports 0 sessions and no cost (Numbers below).

## Improvements

| # | Improvement | For (project input or template rule / skill) | Evidence |
|---|---|---|---|
| 1 | `/preflight` checks every tool the later gates run on this host: all three Playwright browsers with their system libraries, the scanner images, memory headroom for mutation testing. It also asks the human to hold host package upgrades for the length of the run | template (`/preflight` skill) | D-03, D-04, D-90, D-94, D-96; out-of-memory stop in Run 2 |
| 2 | CI and the per-story DoD scan the built container images (Trivy `image`), not only the manifests | template (ES-14, `general/security`) | F-35, F-36 found only at `/verify`; D-97 |
| 3 | `/acceptance-tests` writes each story's frozen tests so they set up their data through the API and use only screens the story itself builds. Otherwise `/plan` orders the stories by test dependency | template (`/acceptance-tests`, `/plan`) | D-39, D-44, D-46, D-49, D-68, D-71, D-40, D-51, D-64, D-77 |
| 4 | Answer the hosting and retention questions (Q-39, Q-32, Q-33) before `/plan`, so staging exists for DoD-P02, DoD-P03 and DoD-P05 and the deployment files have a target | project (`01_project/00_intake`, `environments.md`) | D-97; verification report DoD-P02, DoD-P03, DoD-P05; release notes M-03 … M-06 |
| 5 | `/grill-me` and `/to-spec` ask per story for UI copy in both languages, input limits and empty/error states | template (`/grill-me`, `/to-spec`) | about 25 "details not stated" records (D-42 … D-83); 62 review findings |

## Numbers

Written by `system/run.mjs --summary`.

| Measure | Value |
|---|---|
| Sessions started | 0 |
| Sessions failed (exit code) | 0 |
| Sessions per skill | none |
| Cost reported by the agents (USD) | not reported |
| Decisions | 98 |
| Decisions by class (allow / open / block) | 16 / 59 / 23 |
| Approvals | 9 |
| Verification runs | 2 |
| Commits | 484 |
