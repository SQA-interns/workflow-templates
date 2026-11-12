# Review checklist

> For: agent (reads), human (reviews) · Owner: Developer · Agent: writes in intake only

Everything the developer must confirm or answer before the run. Edit the project file named in each row, then mark the row done.

| Done | File | ID | Kind (assumption, open, developer answer) | Question | Proposed answer |
|---|---|---|---|---|---|
| | `01_requirements/intent.md` | OQ-P-01 (Q-01) | open — needs customer | Who suffers today, how do they cope, what does it cost? | Ask the customer; not blocking for the build. |
| | `intent.md`; `business-rules.md` BR-03, BR-04; US-002 OQ-002-01, US-005 OQ-005-01, US-011 OQ-011-01, US-018 OQ-018-01 | OQ-P-02 (Q-07, Q-17, Q-18) | open — needs customer (Block: rights) | Roles Owner / Editor / Viewer; several Groups per User; no administrator who sees Contacts; Owner-only Group and Member management; Owner and Editor merge and restore? | As written. Must be confirmed before `/clarify`, or the stories stay not ready (DoR-S-07). |
| | `intent.md`; BR-08; US-003 OQ-003-01, US-012 OQ-012-01, US-013 OQ-013-01; project-wide facts "recycle bin period" | OQ-P-03 (Q-22, Q-33) | open — needs customer (Block: deletion) | Recycle bin for 30 days for Contacts and Groups; Owner-only permanent delete; removal after 30 days? | As written. |
| | `intent.md`; `02_design/security-requirements.md` (Personal data, SR-06); US-021 OQ-021-01 | OQ-P-04 (Q-32, Q-33) | open — needs customer (Block: personal data) | Group is controller, we are processor; permanent delete erases all Versions; retention while the Group exists; account deletion with "Deleted user" in old Versions? | As written. |
| | `intent.md`; BR-10; SR-03, SR-05; US-001 OQ-001-01, OQ-001-02; US-004 OQ-004-01 | OQ-P-05 (Q-34, Q-35) | open — needs customer (Block: security) | Sign-in with e-mail/password and Google; e-mail verification required; 100 requests/min per User; audited actions; Invitation links Owner-only, 7 days, single use, bound to the invitee's e-mail, copied by the Owner? | As written. |
| | `intent.md`; `00_setup/environments.md` (staging, production) | OQ-P-06 (Q-39) | open — needs customer | Where do staging and production run, and who operates them? | Customer to name a host before `/release`; staging is needed for DoD-P02, P03, P05. |
| | `00_setup/environments.md` (External services) | Q-36 | open — developer | Create or provide a Firebase project (staging) with e-mail/password and Google enabled. | Needed before `/release`; local and test use the emulator, so `/preflight` can run without it. |
| | US-005 | OQ-005-02 | open | Must an Owner be able to hand over ownership? | No, not in this release. |
| | US-009 | OQ-009-01 | open | Tag several Checked contacts at once? | No, not in this release. |
| | US-015 | OQ-015-01 | open | "has Tags" with several Tags: all of them or any? | All. |
| | US-017 | OQ-017-01 | open | Dismiss a Possible duplicate pair as "not a duplicate"? | Not in this release. |
| | US-017 | OQ-017-02 | open | Match "041 123 456" with "+386 41 123 456"? | No. |
| | `business-rules.md` project-wide facts | Date and time format | assumption | Time `HH:mm` 24-hour and the same `dd.mm.yyyy` format in English? | Yes. |
| | `business-rules.md` project-wide facts | Number format | assumption | Slovenian `10.000`, English `10,000`? | Yes. |
| | `business-rules.md` project-wide facts | Languages | assumption | Is the language choice kept per User? | Yes. |
| | `business-rules.md` project-wide facts | Working hours | assumption | Working hours for the 99 % availability: Mon–Fri 08:00–16:00 Europe/Ljubljana? | Yes. |
| | `business-rules.md` | BR-02 | assumption | Non-Members get 404 (not 403) so a Group's existence is hidden? | Yes. |
| | `business-rules.md` | BR-09 | assumption | Contacts in the recycle bin or merged are excluded from list, search, filters, Export and duplicates? | Yes. |
| | `business-rules.md` Data model | Field lengths | assumption | Group name 1–100; first/last name 100; e-mail 254; phone 30 (digits, spaces, `+ - ( ) /`); company 200; job title 100; street 200; postal code 20; city 100; note 2000? | Yes. |
| | `business-rules.md` Data model | Group name, Owner, Invitation role, displayName, AuditEntry | assumption | Group names not unique; exactly one Owner per Group; Invitations never grant Owner; display name falls back to the e-mail's local part; Audit entries are never changed? | Yes. |
| | `intent.md` MVP | US-021 | assumption | Account deletion as a `should` story in increment 2? | Yes. |
| | US-001 | R-001-05 | assumption | E-mail must be verified before use? | Yes (also in OQ-P-05). |
| | US-002 | R-002-02, R-002-03, R-002-05 | assumption | Group name max 100; Groups sorted A–Z; open the Group used last? | Yes. |
| | US-004 | R-004-03, R-004-05 | assumption | A new Invitation revokes an open one for the same e-mail; Invitations bound to the invitee's e-mail? | Yes. |
| | US-005 | R-005-01, R-005-05 | assumption | Only the Owner sees the Member list; the Owner cannot remove or demote themselves? | Yes. |
| | US-006 | R-006-07 | assumption | Trim spaces of every text Field? | Yes. |
| | US-007 | R-007-03, EX-007-03 | assumption | Save without changes creates no Version; editing a Contact in the recycle bin is refused? | Yes. |
| | US-008 | R-008-03, R-008-06 | assumption | Empty Attribute value allowed; Attributes shown sorted by key? | Yes. |
| | US-009 | R-009-05 | assumption | No comma in a Tag name (Export joins Tags with commas)? | Yes. |
| | US-011 | R-011-04 | assumption | Restoring a Version re-creates a Tag that no longer exists? | Yes. |
| | US-012 | R-012-02, R-012-05 | assumption | Recycle bin visible to Owner and Editor only; removal job at least hourly? | Yes. |
| | US-013 | R-013-03, R-013-04 | assumption | Permanent delete only from the recycle bin; refused while another Contact is merged into it? | Yes. |
| | US-014 | Page, R-014-01, R-014-02, R-014-04 | assumption | 50 per Page; columns name, company, e-mail, phone, Tags; default sort last name A–Z (Slovenian collation); search in first/last name, e-mail, phone, company, city, ignoring case and diacritics? | Yes. |
| | US-015 | R-015-01 … R-015-04 | assumption | "has Tags" = all; "without Tags" filter; Attribute filter exact, case-insensitive; company and country filters? | Yes. |
| | US-016 | R-016-03 … R-016-06 | assumption | Attribute columns and Tags sorted A–Z; headers in the User's language; country as ISO code; more than 10,000 → refused, not cut; file name `kontakti-<Group>-<date>.xlsx`? | Yes. |
| | US-017 | R-017-03, R-017-04 | assumption | Phone match needs at least 6 digits; name similarity threshold 0.8 (trigram)? | Yes; tune after a trial with real data. |
| | US-018 | R-018-01, R-018-03, R-018-05 | assumption | Primary contact's non-blank values preselected; the User must choose between different Attribute values; "Merged from" link on the Primary contact? | Yes. |
| | US-019 | R-019-02 | assumption | Undo merge refused once the Primary contact changed after the Merge? | Yes. |
| | US-021 | priority, R-021-02, R-021-03 | assumption | Owner must delete their Groups before deleting the account; "Deleted user" shown in Versions? | Yes. |
| | `e2e-scenarios.md` | E2E-06, E2E-07, E2E-08 | assumption | Three scenarios added so every MVP story is covered (Members, permanent delete, Group lifecycle). | Keep. |
| | `02_design/architecture.md` | backend bootstrap, frontend deploys_as, local port | assumption | Root package `app.contacts`; frontend served by nginx proxying `/api`; local URL `http://localhost:8080`? | Yes. |
| | `02_design/security-requirements.md` | Verification standard; Personal data | assumption | ASVS Level 2; Invitation e-mails and Audit entries kept until the Group is erased; no Audit log screen in this release? | Yes. |
| | `02_design/quality-requirements.md` | NFR-02, NFR-05 | assumption | Export test data shape (5 Attributes, 3 Tags); removal of 10,000 Contacts under 1 minute? | Yes. |
| | `02_design/quality-requirements.md` | NFR-06, NFR-07 | developer answer | Q-37 named desktop and tablet and WCAG 2.1 AA; the baseline (mobile from 320 px, WCAG 2.2 AA) is stricter and is kept. | Keep the baseline. |
| | `02_design/ui-design.md` | UI-05 override | developer answer | Light mode only, no dark mode (Q-40)? | Yes; or drop the override to get dark mode at no extra design cost. |
| | `00_setup/environments.md` | Backups | assumption | `pg_dump` daily at 02:00 Europe/Ljubljana, stored off the database host? | Yes. |
| | `00_setup/tech-stack.md` | all versions | assumption | Every version is `[ASSUMPTION] verify in preflight` (Java 21, Spring Boot 3.5.7, React 18.3.1, PostgreSQL 16.10, …). | Let `/preflight` verify. |
| | `00_setup/tech-stack.md` | k6 | assumption | k6 is AGPL-3.0; used only as a load-test tool, never distributed. Acceptable? | Yes. |
| | `00_setup/run-config.md` | Allowed commands | developer answer | Allow `./mvnw *`, `java -version`, `trivy *`, `gitleaks *`, `k6 *` (plus the runner's base set)? | Yes. |
| | `00_setup/secrets.env.example` | DB_PASSWORD, FIREBASE_SERVICE_ACCOUNT_FILE | developer answer | Fill `DB_PASSWORD` in `.env`; the service account is needed only for US-021 on staging/production. | Yes. |
| | `intent.md`, all stories | Q-02, Q-05, Q-06, Q-10, Q-14 … Q-16, Q-19 … Q-21, Q-23 … Q-31, Q-37, Q-38, Q-40 … Q-42 | developer answer | These came from the developer, not the customer: success criteria, build from scratch, terms, priorities, Field list, rules for versions, locking, duplicates, Merge, Tags, Attributes, Export, facts, scope, principles, quality, stack, design, journeys, DoD-P. Confirm with the customer where it matters. | Show the customer `intent.md` and the story list. |

## Stories

| Story | Title | Priority | MVP | Estimate | Depends on |
|---|---|---|---|---|---|
| US-001 | Sign up and sign in | must | true | 3 | — |
| US-002 | Create a Group and switch between Groups | must | true | 3 | US-001 |
| US-003 | Rename, delete and restore a Group | must | true | 5 | US-002 |
| US-004 | Invite a Member | must | true | 5 | US-002 |
| US-005 | Change a Member's Role or remove a Member | must | true | 3 | US-004 |
| US-006 | Add a Contact | must | true | 3 | US-002 |
| US-007 | Edit a Contact | must | true | 3 | US-006 |
| US-008 | Keep Attributes on a Contact | must | true | 3 | US-007 |
| US-009 | Tag Contacts | must | true | 3 | US-007 |
| US-010 | View a Contact's History | must | true | 3 | US-007 |
| US-011 | Restore an earlier Version | must | true | 3 | US-010 |
| US-012 | Move a Contact to the recycle bin and restore it | must | true | 5 | US-007 |
| US-013 | Permanently delete a Contact | must | true | 2 | US-012 |
| US-014 | Browse and search the Contact list | must | true | 5 | US-006 |
| US-015 | Filter the Contact list | must | true | 5 | US-014, US-008, US-009 |
| US-016 | Export Contacts to Excel | must | true | 5 | US-015 |
| US-017 | Find Possible duplicates | should | false | 5 | US-014 |
| US-018 | Merge two Contacts | should | false | 8 | US-017, US-010 |
| US-019 | Undo a Merge | should | false | 3 | US-018 |
| US-020 | Choose the language | must | true | 1 | US-001 |
| US-021 | Delete my account | should | false | 3 | US-005 |
