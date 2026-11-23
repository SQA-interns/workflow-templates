# Release notes

> For: agent (writes), human (reads) · Written in: phase 7 (`/release`) · Agent: writes

Release of branch `tjan/workflow/sdd/v001/run-1`, verified at `71dcc9c` (`docs/06_verification-report.md` Run 2: phase 6 gate passed, no finding open). The commits after it change only documents, deployment files and scripts.

## Delivered

All 21 stories are done and meet their DoD-S. Exceptions are listed per story in `docs/00_progress.md`: commit-size and subject-length notes under DoD-S-07, and D-93.

| Increment | Stories |
|---|---|
| INC-1 (MVP) | US-001 sign up, sign in, e-mail verification · US-020 language choice (sl, en) · US-002 create and switch Groups · US-003 Group lifecycle · US-004 Invitations · US-005 Member management · US-006 add a Contact · US-007 edit a Contact · US-008 Attributes · US-009 Tags · US-010 History · US-011 restore a Version · US-012 recycle bin with removal after 30 days · US-013 permanent delete · US-014 Contact list and search · US-015 list filters · US-016 `.xlsx` Export |
| INC-2 | US-017 possible duplicates · US-018 Merge · US-019 Undo merge · US-021 delete my account |

Evidence at `71dcc9c`:
- Backend: 477 unit and 279 integration tests. Frontend: 624 unit tests. Browser: 456 tests across Chromium, Firefox and WebKit, including E2E-01 … E2E-08.
- Held-out tests: 123/123 (`docs/06_hidden-evaluation.md`). The held-out suite is committed in `02_output/hidden-tests/` (`b3b4dc0`) and matches `docs/03_hidden-manifest.sha256`.
- Line coverage: 99.53 % backend, 99.95 % frontend. Mutation score: 97.07 % backend, 94.87 % frontend.
- NFR-01 … NFR-08 pass.
- API contract: `docs/02_contracts/openapi.yaml` (DoD-P04).

## Known limitations

| Item | Effect | Source |
|---|---|---|
| F-46 accepted | Seven stories (US-001, US-003, US-004, US-011, US-016, US-019, US-020) have no `feat` commit naming the `US` id. Traceability holds through the R/AC/EX ids | D-93 |
| No CI run | `.github/workflows/ci.yml` has never run because the branch is not pushed. Every job command passes locally | verification report, DoD-R-15 |
| No staging environment | For now the product is hosted locally on the developer's machine (D-100); a staging and production host is not decided (Q-39). DoD-P02, the staging part of DoD-P03, DoD-P05, NFR-09 and TLS on a real host are unchecked; the deployment files are host-neutral | D-97 |
| TLS certificate renewal | The proxy uses certificate files. Renewal (e.g. certbot) and restarting the proxy are the operator's job. Caddy (automatic HTTPS) was rejected: its latest image has a fixable High | D-97, `out/logs/s10-trivy-caddy.txt` |
| Retention is partly assumed | The Invitation and Audit retention, and the controller/processor roles, are assumptions until the customer confirms them (Q-32, Q-33 OPEN) | `project/02_design/security-requirements.md` |
| Audit entries not in the UI | Only the Operator sees them, in the database | security-requirements.md (assumption) |
| Postgres image | 27 findings in `gosu` (Go stdlib), rated Low because `gosu` runs only at container start | D-11, D-12 |
| Host toolchain | Builds and tests used host JDK 21.0.13 and 21.0.13.1; the image and CI pin 21.0.13 | D-01, D-96 |

## Must be tested manually by a human

| # | Test | Why no automated evidence | Record |
|---|---|---|---|
| M-01 | Sign in with a real Google account on staging | The emulator fakes Google sign-in | date, result |
| M-02 | Sign up with e-mail and password on staging, receive the real verification e-mail, verify, sign in | The emulator does not send e-mails | date, result |
| M-03 | DoD-P02 / NFR-09: a new User adds a first Contact in under 2 minutes without help | Timed task with a person | User, time |
| M-04 | DoD-P03: customer acceptance of the 21 stories on staging | Needs the staging host (Q-39) | date, who accepted |
| M-05 | DoD-P05: restore one backup on staging with `scripts/restore.sh` (rehearsed on the build host: `out/logs/s10-backup-restore.txt`) | Needs the staging host and its off-host backup storage | date, result (write it in this file) |
| M-06 | TLS on staging: valid certificate, HTTP → HTTPS, HSTS (rehearsed with a self-signed certificate: `out/logs/s10-deploy-smoke.txt`) | Needs the real domain and certificate | date, result |
| M-07 | US-021 on staging: deleting an account removes the user from the real Firebase project (service account key) | The emulator stands in for Firebase in every test | date, result |
| M-08 | Invitation links on staging start with `https://<APP_DOMAIN>` and open for the invitee | `APP_PUBLIC_URL` is local in all tests | date, result |
| M-09 | Browsers: the product works in Edge and in Safari on a real device (NFR-06 automated: Chromium, Firefox, WebKit engines) | Branded browsers are not in the test run | browser, result |
| M-10 | The daily backup cron job runs at 02:00 Europe/Ljubljana and deletes dumps older than 30 days | Needs the host's cron | date of first dump |

## Decisions pending review

Every blocking decision has a human answer in a later record: D-07, D-12, D-27, D-35, D-43, D-65, D-91, D-95, D-97. The decisions below were resolved by the agent (`Resolution: … pending review`). **The human reviewed and approved all of them on 2026-11-23 (D-99).**

| Area | Decisions |
|---|---|
| Toolchain and versions | D-01, D-02, D-08, D-09, D-92, D-96 |
| Dev-only tools and images | D-28, D-29, D-31, D-32 |
| Requirements details left open by the inputs | D-22, D-23, D-24, D-25, D-26, D-37, D-42, D-45, D-47, D-50, D-66 |
| Story details found while implementing | D-52 … D-62, D-79, D-81, D-83 |
| Tests and test environment | D-36, D-38, D-39, D-44, D-46, D-49, D-67, D-89 |
| UI behaviour | D-48 |
| Performance | D-82 |
| Process | D-93 (F-46), D-98 (hidden suite, closed by `b3b4dc0`) |
| Approval asked earlier and still open in the log | D-40, D-41 (answered in D-43), D-51, D-64 (answered in D-65); the records keep `pending` as written |

## Data retention duties

From `project/02_design/security-requirements.md` (Q-32, Q-33 OPEN: confirm with the customer). The Group is the controller and the operator of the product is its processor.

| Data | Retention | Who or what does it |
|---|---|---|
| Contact Fields, Attributes, Tag assignments | While the Contact is active. 30 days in the recycle bin, then erased with all its Versions | The removal job inside the backend (ADR-10, NFR-05), or a permanent delete by an Owner (US-013) |
| Versions | As long as the Contact exists | Erased with the Contact (SR-06) |
| User e-mail and display name | Until the User deletes the account (US-021); the display name is replaced by "Deleted user" | The product. The Firebase account is deleted through the service account (M-07) |
| Invitation e-mail | Until the Group is erased (assumption) | The product |
| Audit entries (no personal data beyond the User id) | Until the Group is erased (assumption) | The product. They cannot be changed through the product (SR-04) |
| Database backups | 30 days, stored off the database host | The operator: cron with `scripts/backup.sh` and the storage behind `BACKUP_DIR`. Erased data stays in backups until they expire; tell data subjects this when answering an erasure request |
| Logs | No personal data or tokens (SB-07, F-38); keep them as the host's log rotation does | The operator |

The Operator manages accounts in the Firebase console and does not see Contacts through the product.

## How to deploy

Full steps: `02_output/README.md`, "Deploy". In short:
1. Create a Firebase project per environment and store its service account key on the host.
2. Put the TLS certificate and key on the host.
3. Build the images with `scripts/build-images.sh`.
4. Copy `deploy/env.example` to `deploy/<environment>.env` and fill it.
5. Run `docker compose --env-file <environment>.env up -d --wait` in `deploy/`.
6. Add the backup cron entry.
7. Restore one backup (M-05).
