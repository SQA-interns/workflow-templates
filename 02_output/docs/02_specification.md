# Specification

> For: agent (writes), human (reads) · Written in: phase 2 (`/plan`) · Source: `project/01_requirements/*`, `project/02_design/*`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `general/security/*`, `general/design/ui-baseline.md`, `docs/02_adr/` · Agent: writes

Decisions applied throughout: D-15 … D-27 (requirements), D-28 … D-33 (plan). Where a decision overrides an input entry, the decision wins (`AGENTS.md`).

## 1 Components and modules

| Component | Module | Responsibility | ADR |
|---|---|---|---|
| backend | `web` | One controller per resource of `docs/02_contracts/openapi.yaml`; request/response records; `ProblemHandler` maps exceptions to §5 | ADR-01, ADR-06 |
| backend | `auth` | `FirebaseTokenFilter` (verify token, provision User, refuse deleted User, e-mail-verified gate), `CurrentUser`, `RateLimitFilter`, `FirebaseAccountDeleter` (US-021) | ADR-03 |
| backend | `service` | `AccessGuard` (§6.1), `UserService`, `GroupService`, `MembershipService`, `InvitationService`, `ContactService` (create, update incl. Attributes and Tags), `ContactValidator`, `ContactQueryService` (list, search, filters, Export scope), `TagService`, `VersionWriter`, `HistoryService`, `RecycleBinService`, `PermanentDeleteService`, `RemovalJob`, `ExportService`, `DuplicateService`, `MergeService`, `AuditService`, `Normaliser` (Normalised text, phone, full name) | ADR-02, ADR-04, ADR-07, ADR-09, ADR-10, ADR-12 |
| backend | `repository` | Spring Data repositories, Group-owned ones extend `Repository` and take `groupId` (AR-04) | ADR-02 |
| backend | `domain` | Entities of §2 | ADR-01 |
| backend | `config` | `AppProperties`, `SecurityConfig`, `ApplicationConfig` (clock), CORS for `APP_PUBLIC_URL` | ADR-05 |
| frontend | `app` | router (§9.2), `AppLayout` (header, Group switcher, user menu), error boundary | ADR-08 |
| frontend | `api` | `apiClient` (Bearer token, problem parsing → catalog key), TanStack Query hooks per resource | ADR-06, ADR-08 |
| frontend | `auth` | Firebase init from `/config.json`, sign-in/up/out, e-mail verification screen, route guard | ADR-03, ADR-05 |
| frontend | `features/*` | screens of §9.1 per glossary concept | ADR-08 |
| frontend | `i18n` | `sl.json`, `en.json`, formatters for dates (`dd.mm.yyyy`, `HH:mm`, Europe/Ljubljana) and numbers per language | UI-18, UI-30 |
| frontend | `styles`, `components/ui` | tokens (wireframe/final), shadcn/ui primitives | UI-01, UI-03 |

Commands (ES-05) are in each component README. Local stack, images, seed: `02_output/docker-compose.yml`, `scripts/up.sh`, `scripts/seed-local.sh` (ADR-11, D-32).

## 2 Domain model (glossary → code)

| Term | Module | Type | Table | API name |
|---|---|---|---|---|
| User | domain, auth | `User` | `app_user` (`user` is an SQL keyword) | `Me`, `/me` |
| Group | domain, service | `Group` | `contact_group` (`group` is an SQL keyword) | `Group`, `/groups` |
| Member | domain | `Membership` | `membership` | `Member`, `/members` |
| Role | domain | enum `Role` (`OWNER`, `EDITOR`, `VIEWER`) | `membership.role` | `Role` |
| Owner / Editor / Viewer | domain | `Role` values | — | `OWNER` / `EDITOR` / `VIEWER` |
| Invitation | domain, service | `Invitation` | `invitation` | `Invitation`, `/invitations` |
| Contact | domain, service | `Contact` | `contact` | `Contact`, `/contacts` |
| Field | domain | `ContactFields` (record of the 11 Fields, D-23) | `contact` columns | `fields` |
| Attribute | domain | `Attribute` (`key`, `value`) | `attribute` | `attributes` |
| Tag | domain | `Tag`, link `ContactTag` | `tag`, `contact_tag` | `Tag`, `tags` |
| Version | domain, service | `ContactVersion` | `contact_version` | `Version`, `/versions` |
| History | service | `HistoryService.history(…)` | `contact_version` | `/versions` (list) |
| Restore (a Version) | service | `HistoryService.restoreVersion` | — | `/versions/{n}/restore` |
| Recycle bin | service | `RecycleBinService` | `state = 'inRecycleBin'` | `/recycle-bin`, `/deleted-groups` |
| Move to recycle bin | service | `moveToRecycleBin` | — | `/move-to-recycle-bin` |
| Permanent delete | service | `PermanentDeleteService.permanentlyDelete` | — | `DELETE /recycle-bin/{id}` |
| Possible duplicate | service | `DuplicateCandidate` | — | `DuplicateCandidate`, `/duplicates` |
| Merge | domain, service | `Merge`, `MergeService.merge` | `contact_merge` (`merge` is an SQL keyword) | `Merge`, `/merges` |
| Primary contact | domain | `Merge.primary` | `contact_merge.primary_id` | `primaryId` |
| Undo merge | service | `MergeService.undoMerge` | `contact_merge.undone_at` | `/merges/{id}/undo` |
| Contact list | service, frontend | `ContactQueryService`, `ContactList` | — | `ContactPage` |
| Search | service | `ContactQuery.search` | `contact.search_text` | `q` |
| Filter | service | `ContactQuery.filters` | — | `hasTagIds`, `withoutTagIds`, `attributeKey`, `attributeValue`, `company`, `country` |
| Checked contacts | frontend | `selection` | — | `ExportRequest.contactIds` |
| Export | service | `ExportService.export` | — | `/exports` |
| Audit entry | domain, service | `AuditEntry` | `audit_entry` | none (no API, D-19) |

## 3 Data model and validation

Schema: `backend/src/main/resources/db/migration/V1__schema.sql` (persistence contract). Additions over the Data model: `app_user.deleted_at` (D-20), `contact.search_text`, `contact.sort_name` (ADR-02), `contact_version.restored_from` (R-011-01), `group_id` on every Group-owned table (AR-04).

Validation, server side (SB-01), after trimming every text (R-006-07); the frontend repeats it for inline errors (UI-13):

| Item | Rule | Error code (§5) | Source |
|---|---|---|---|
| Group name | 1–100 characters | `group.nameLength` | R-002-02, R-003-01 |
| Contact name parts | at least one of firstName, lastName, company non-blank | `contact.nameRequired` | R-006-02 |
| Field length | firstName 100, lastName 100, email 254, phone 30, company 200, jobTitle 100, street 200, postalCode 20, city 100, note 2000 | `field.tooLong` {field, max} | R-006-03 |
| email | blank or valid syntax (one `@`, non-empty local part, domain with a dot, no spaces) | `field.emailInvalid` | R-006-04, EX-004-06 |
| phone | blank or only digits, spaces, `+ - ( ) /` | `field.phoneInvalid` | D-23 |
| country | blank or an ISO 3166-1 alpha-2 code from `java.util.Locale.getISOCountries()` | 400 `field.countryInvalid` | R-006-05 |
| Attribute key | 1–50 characters; unique per Contact case-insensitively | `attribute.keyLength`, `attribute.keyTwice` {key} | R-008-02, R-008-04 |
| Attribute value | 0–500 characters | `attribute.valueTooLong` | R-008-03 |
| Attributes per Contact | at most 50 | `attribute.tooMany` | R-008-05 |
| Tag name | 1–50 characters, no comma; one Tag at most once per Contact (duplicates collapse) | `tag.invalid` | R-009-05, R-009-06 |
| Invitation | e-mail valid; Role `EDITOR` or `VIEWER` (else 400) | `field.emailInvalid`, `invitation.roleInvalid` | R-004-01, EX-004-07 |
| Language | `sl` or `en` | 400 `me.languageInvalid` | EX-020-01 |

Derived values: Normalised text = lower case, `č→c š→s ž→z ć→c đ→d` (US-014); `search_text` over firstName, lastName, email, phone, company, city; `sort_name` = lastName, else firstName, else company (EX-014-04); Normalised phone and Normalised full name per US-017; Name similarity per D-25.

## 4 Behaviour per story

Rules not repeated; each row says where they are implemented. "Guard" = `AccessGuard` permission (§6.1). Every change of a Contact runs in one transaction: lock the Contact (`FOR UPDATE`), compare `versionNumber` (BR-06 → 409 `version.conflict` {changedBy}), apply, `VersionWriter.write(kind)` (BR-05, AR-08).

| Story | Backend | Frontend screen (§9.1) | Notes and decisions |
|---|---|---|---|
| US-001 | `FirebaseTokenFilter`: R-001-02/03 → 401; first call creates the User with token e-mail, name (or e-mail local part) and `sl` (R-001-04); deleted User → 401, never re-created (D-20); unverified password account → only `GET /me`, else 403 `auth.emailNotVerified` (R-001-05, D-16). `GET /me`. | WF-01 sign-in, WF-02 sign-up, WF-03 verify e-mail, WF-05 no Groups | Sign-out discards the token (R-001-06). Wrong password / existing e-mail messages come from Firebase error codes (EX-001-01, EX-001-05). |
| US-002 | `GroupService.create` (Owner Membership, R-002-01), `listGroups` sorted by name in `sl-x-icu` (R-002-03), `getGroup`. | WF-04 create Group, header Group switcher | Last selected Group in `localStorage` per User id (R-002-05, D-22); unavailable → first Group or WF-05. |
| US-003 | `GroupService.rename` (MANAGE_GROUP), `moveToRecycleBin`, `listDeletedGroups`, `restore` (Owner of a Group in recycle bin; at or after `deletedAt + 30 days` → 409 `group.noLongerRestorable`, R-003-06); Group in recycle bin → 404 for all other requests (R-003-03); Audit entries (BR-10). `RemovalJob` erases Groups (BR-08, D-19). | WF-14 Group settings, WF-15 Deleted Groups | Removal date shown in Europe/Ljubljana (AC-003-03: DST). Invitation of a Group in the recycle bin → `invitation.noLongerValid` (EX-003-05). |
| US-004 | `InvitationService.create` (MANAGE_GROUP; 128-bit token from `SecureRandom`, SHA-256 hash stored, link returned once, expires +7 days, earlier open Invitation for the same e-mail revoked + Audit, R-004-03, D-17; Member e-mail → 409 `invitation.alreadyMember`), `listOpen`, `revoke` (+ Audit), `view` and `accept` (e-mail match case-insensitive, R-004-05; not open or expired at `expiresAt` → 409 `invitation.noLongerValid`; acceptance + Audit, D-17). | WF-13 Members and Invitations (Owner), WF-16 Invitation | Not signed in → sign-in, then back to the Invitation (R-004-07). |
| US-005 | `MembershipService.list` (MANAGE_GROUP), `changeRole` (EDITOR↔VIEWER; own Role → 409 `member.ownerRequired`; OWNER → 400), `remove` (own → 409 `member.ownerRequired`); Audit entries. | WF-13 | Versions keep the User id, so removed Members still show their name (R-005-04). |
| US-006 | `ContactService.create` (CHANGE_CONTACT, `ContactValidator`, Version 1 `created`); `GET /contacts/email-matches` for R-006-06. | WF-08 Contact form | Warning, not error: saving stays possible (AC-006-05). |
| US-007 | `ContactService.update` (Fields; no changed item → 409 `contact.noChanges`, R-007-03; `inRecycleBin` → 409 `contact.inRecycleBin`; `merged` → 409 `contact.merged`, D-23). | WF-08 | One Version lists every changed item (EX-007-01). |
| US-008 | Part of `ContactService.update`/`create`: Attribute changes as `attribute:<key>` items in the same Version (R-008-01); sorted by key A–Z, spelling kept (R-008-06). | WF-08 (Attributes section), WF-07 | — |
| US-009 | Part of `ContactService.update`/`create` via `TagService.resolve` (existing Tag case-insensitively, else created, R-009-03/04); `GET /tags?q=` suggestions only for Tags on an active Contact (R-009-02). | WF-08 (Tags input) | No bulk tagging (OQ-009-01, D-22). |
| US-010 | `HistoryService.history` (READ), `version(n)`; no update/delete mapping → 405 (EX-010-03). | WF-09 History, WF-10 Version | Display name of the User, "Deleted user" after D-20 (`user.deleted`). |
| US-011 | `HistoryService.restoreVersion` (CHANGE_CONTACT; same content → 409 `version.sameAsCurrent`; re-creates missing Tags, R-011-04; kind `restored`, `restored_from` = n). | WF-10 (Restore) | Confirmation dialog (UI-15). |
| US-012 | `RecycleBinService.move` (Version `movedToRecycleBin`), `list` (VIEW_RECYCLE_BIN, newest first), `restore` (before removal date, Version `restoredFromRecycleBin`; else 409 `contact.noLongerRestorable`); `RemovalJob` every 15 min (R-012-05, D-18). Already in recycle bin → 409 `contact.inRecycleBin` (EX-012-06). | WF-11 Recycle bin, Contact details action | BR-09: queries filter `state = 'active'`. |
| US-013 | `PermanentDeleteService` (PERMANENTLY_DELETE; active → 409 `contact.notInRecycleBin`; deletes Contact, Versions, Attributes, Tag assignments and every Contact merged into it, D-21; Audit entry per Contact without personal data, R-013-02). | WF-11 (Delete permanently) | The confirmation names merged Contacts (D-21). R-013-04 dropped (D-27). |
| US-014 | `ContactQueryService.page` (READ; 50 per Page, total, sort R-014-02/03 with D-24 (a), search R-014-04 with `%`/`_` escaped). | WF-06 Contact list, WF-07 Contact details | Empty states WF-06 (R-014-05, EX-014-02). |
| US-015 | Same query: has-all Tags, without Tags, Attribute key [= value] case-insensitive, company equals case-insensitive, country; AND with search (R-015-01 … 05); Tag ids of another Group → 404 (EX-015-04). | WF-06 filter panel and chips | Filters live in the URL query so the list, the count and the Export use the same query. |
| US-016 | `ExportService` (EXPORT; scope from the same query, `contactIds` within it, > 10,000 → 409 `export.tooMany`, empty → 409 `export.empty`; layout `export-xlsx.schema.json`; SR-07; Audit). | WF-06 (Export, Export checked) | Checked contacts kept across Pages, cleared when search or filters change (D-24 (b)). |
| US-017 | `DuplicateService` (READ; ADR-12, D-25 (a)). | WF-17 Possible duplicates | — |
| US-018 | `MergeService.merge` (CHANGE_CONTACT; both active, same Group, current Versions; Attribute conflicts need a choice → 400 `merge.choiceRequired` {key}; > 50 Attributes → 409 `merge.tooManyAttributes`; Versions `merged` on both, `merged_into`, Audit). | WF-18 Merge screen, WF-07 ("Merged from") | First Contact = first in R-014-02 order (D-25 (b)). |
| US-019 | `MergeService.undoMerge` (checks in the order of D-25 (c); Versions `mergeUndone` on both; Audit). | WF-07 (Undo merge) | — |
| US-020 | `UserService.changeLanguage` (`PATCH /me`). | user menu, WF-19 Account | Catalog switch without reload (R-020-02); formatters (R-020-03). |
| US-021 | `UserService.deleteAccount` (typed e-mail must match; Owner of any Group → 409 `account.ownsGroups` {names}; delete Memberships; anonymise User; delete Firebase account through `FirebaseAccountDeleter`). | WF-19 Account | D-20. Needs `FIREBASE_SERVICE_ACCOUNT_FILE` on staging/production. |

## 5 Error model

RFC 9457 problem (`openapi.yaml#/components/schemas/Problem`, ADR-06). The UI shows the catalog text for `code`; the English source texts below are the story wording (catalog `en.json`); Slovenian texts are written with the screen.

| Code | Status | English text (catalog) | Source |
|---|---|---|---|
| `auth.unauthenticated` | 401 | (sign-in screen) | R-001-03, SR-01 |
| `auth.emailNotVerified` | 403 | Confirm your e-mail address: we sent you a link. | R-001-05 |
| `auth.forbidden` | 403 | You are not allowed to do this. | BR-03, BR-04 |
| `notFound` | 404 | This page cannot be found. | BR-02 |
| `rateLimited` | 429 | Too many requests. Try again in a minute. | SR-03 |
| `validation` (+ `errors[]`) | 400 | per field code of §3 | SB-01 |
| `group.nameLength` | 400 | Enter a name of 1 to 100 characters. | R-002-02 |
| `group.noLongerRestorable` | 409 | This Group can no longer be restored. | R-003-06 |
| `invitation.alreadyMember` | 409 | This person is already a Member. | R-004-02 |
| `invitation.otherEmail` | 403 | This Invitation is for another e-mail address. | R-004-05 |
| `invitation.noLongerValid` | 409 | This Invitation is no longer valid. | R-004-06 |
| `member.ownerRequired` | 409 | A Group always has one Owner. | R-005-05 |
| `contact.nameRequired` | 400 | Enter a first name, last name or company. | R-006-02 |
| `field.tooLong` | 400 | {field} may have at most {max} characters. | R-006-03 |
| `field.emailInvalid` | 400 | Enter a valid e-mail address. | R-006-04 |
| `field.phoneInvalid` | 400 | Enter a valid phone number. | D-23 |
| `field.countryInvalid` | 400 | (API only) | R-006-05 |
| `contact.emailExists` (warning, 200) | — | A Contact with this e-mail already exists: {name}. | R-006-06 |
| `contact.noChanges` | 409 | No changes to save. | R-007-03 |
| `version.conflict` | 409 | Contact was changed by {changedBy}, reload | BR-06 |
| `contact.inRecycleBin` | 409 | This Contact is in the recycle bin. | EX-007-03, EX-011-04 |
| `contact.merged` | 409 | This Contact was merged into another Contact. | D-23 |
| `attribute.keyLength` / `attribute.valueTooLong` / `attribute.keyTwice` / `attribute.tooMany` | 400 | A key must have 1 to 50 characters. / A value may have at most 500 characters. / The key {key} is used twice. / A Contact may have at most 50 Attributes. | R-008-02 … 05 |
| `tag.invalid` | 400 | A Tag must have 1 to 50 characters and no comma. | R-009-05 |
| `version.sameAsCurrent` | 409 | This Version is the same as the current one. | R-011-03 |
| `contact.noLongerRestorable` | 409 | This Contact can no longer be restored. | R-012-04 |
| `contact.notInRecycleBin` | 409 | Move the Contact to the recycle bin first. | R-013-03 |
| `export.tooMany` | 409 | Narrow the list to at most 10,000 Contacts to export. | R-016-05 |
| `export.empty` | 409 | Nothing to export. | R-016-07 |
| `merge.choiceRequired` | 400 | Choose a value for {key}. | R-018-03 |
| `merge.tooManyAttributes` | 409 | Together these Contacts have more than 50 Attributes; remove some first. | R-018-06 |
| `merge.notPossible` | 409 / 400 (same Contact) | These Contacts cannot be merged. | R-018-07, EX-018-05 |
| `merge.alreadyUndone` | 409 | This Merge was already undone. | EX-019-01, D-25 |
| `merge.changedAfter` | 409 | The Contact changed after the Merge. Restore the Versions you need instead. | R-019-02 |
| `merge.restoreFirst` | 409 | Restore the Contact from the recycle bin first. | R-019-03 |
| `account.ownsGroups` | 409 | Delete your Groups first: {names}. | R-021-02 |
| `me.languageInvalid` | 400 | (API only) | EX-020-01 |
| `user.deleted` | — | Deleted user | R-021-03 |

Errors carry no stack trace, SQL, token or Contact data (SB-07, SR-08). Server errors → 500 `internal` with a generic text; the UI shows the error state with retry (UI-14).

## 6 Security controls

### 6.1 Rights

`AccessGuard` permission table: ADR-04 (D-15). Every endpoint of `openapi.yaml` is tested as a non-Member (404) and, where a Role lacks the permission, as that Role (403) (SR-02).

### 6.2 Controls

| Control | Implementation | Checked by |
|---|---|---|
| SB-01 | `ContactValidator` and request validation in `service` (§3) | unit tests per rule; API tests |
| SB-02, SR-01 | `FirebaseTokenFilter` + `AccessGuard`; public: `/actuator/health` only | `ApplicationIT.sr01…`; tests with no, expired, foreign-project and tampered tokens |
| SB-03 | Passwords only in Firebase; secrets only from env (`DB_PASSWORD`, service account file) | gitleaks in CI; config review |
| SB-04, SR-09 | TLS at the staging/production reverse proxy; CORS allows only `APP_PUBLIC_URL` | runtime demonstration on staging (D-26) |
| SB-05 | JPA parameters only; `LIKE` escaping (EX-014-05); React escaping; no `dangerouslySetInnerHTML` (lint) | SpotBugs/FindSecBugs, ESLint security, tests |
| SB-06, SR-03 | `RateLimitFilter` (Bucket4j, 100/min/User, 429 + `Retry-After`); Firebase limits its own sign-in endpoints | 101st-request test |
| SB-07, SR-08 | `ProblemHandler` without internals; logs carry User and Contact ids only | log-capture test during acceptance tests |
| SB-08 | Trivy in CI and phase 6 (D-13 for dev tooling) | CI `scan` job |
| SB-09 | SpotBugs + FindSecBugs, ESLint security, gitleaks (D-30, D-33) | `check` commands, CI |
| SB-10 | nginx headers: CSP, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, `Permissions-Policy` | `logs/plan-stack-checks.txt`; header test in phase 6 |
| SB-11 | Non-root users in every image | `docker compose exec … id -un` (plan-stack-checks) |
| SB-12, SB-13 | Data items and retention: security-requirements "Personal data" with D-19, D-20 | review; SR-06 test |
| SB-14 | No consent processing (the Group is controller, D-19) | — |
| SR-04 | `audit_entry` has no update/delete path; no API | architecture test: no repository method updates or deletes `AuditEntry` except Group erasure |
| SR-05 | 128-bit `SecureRandom` token, base64url; SHA-256 hash stored; single use; 7 days | unit tests (length, hash only stored), AC-004-04, EX-004-02 |
| SR-06 | `PermanentDeleteService`, `RemovalJob` delete all rows of the Contact; Audit entry without personal data | database check after delete (D-26) |
| SR-07 | `ExportService` quote-prefixes values starting with `=`, `+`, `-`, `@`, tab, CR | EX-016-03 test + tab/CR cases |

## 7 Threat model

STRIDE per interface (architecture.md "Interfaces") and per personal-data item (security-requirements).

| Threat (STRIDE) | Interface or data | Control (SB, SR or new) |
|---|---|---|
| S: forged or replayed ID token | Contacts API | SR-01 (signature, project, expiry via Admin SDK); TLS SB-04 |
| S: token of a deleted account (≤ 1 h valid) | Contacts API | D-20: deleted User → 401 (ADR-03) |
| S: unverified e-mail claims an Invitation | Contacts API | D-16 e-mail-verified gate; R-004-05 e-mail binding |
| T: client sends another Group's id or a Role | Contacts API | AR-04, AR-05, SR-02 (`AccessGuard`) |
| T: lost update by concurrent editors | Contacts API | BR-06 optimistic check with row lock |
| R: Member denies an Export, delete or Role change | Contacts API | SR-04 Audit entries (BR-10, D-17) |
| I: Group data to a non-Member, incl. existence | Contacts API | BR-02 404, SR-02 cross-Group tests, E2E-03 |
| I: personal data in logs or errors | Contacts API | SB-07, SR-08 |
| D: request flooding by a signed-in User | Contacts API | SR-03 rate limit |
| D: huge payloads, 10,000-row exports | Contacts API, Export | request size 2 MB (nginx), schema `maxLength`/`maxItems`, export limit R-016-05, streaming (ADR-09) |
| E: Viewer or Editor performs Owner actions | Contacts API | BR-03, BR-04, ADR-04 |
| S: phishing of credentials on a look-alike page | Sign-in | Firebase-hosted flows; CSP `frame-src` only the auth domain [ASSUMPTION] user awareness is out of scope |
| T: script injection steals ID token from the browser | Sign-in, UI | CSP (SB-10), React escaping (SB-05), no third-party scripts except Google sign-in |
| I: emulator settings reach production | Sign-in, Token verification | `FIREBASE_AUTH_EMULATOR_HOST`/`authEmulatorUrl` unset outside test/local (environments.md); staging check in phase 6 |
| S: backend trusts a token for another project | Token verification | `aud`/`iss` = `FIREBASE_PROJECT_ID` (EX-001-02) |
| T/I: SQL injection | Persistence | SB-05 parameterised queries, `LIKE` escaping |
| I: database credentials leak | Persistence | ES-02, `.env` ignored, gitleaks; DB not exposed outside the Compose network |
| D: removal job deletes the wrong data | Persistence | BR-07, BR-08 boundaries tested with a fixed `Clock` (EX-003-02, EX-012-02) |
| T: formula injection in Excel | Export | SR-07 |
| I: Export of Contacts outside the scope or Group | Export | same query as the list (BR-01, BR-09), `contactIds` filtered within it (EX-016-07) |
| T: clickjacking | UI | `frame-ancestors 'none'`, `X-Frame-Options: DENY` |
| I: Contact Fields, Attribute values seen by non-Members | Contact Fields, Attribute values | BR-01, BR-02, SR-02 |
| I: old personal data kept in Versions after erasure | Versions | SR-06, BR-08, D-21 (merged Contacts erased with the Primary contact) |
| I: User e-mail shown to other Members | User e-mail, display name | e-mail only to the Owner on the Members screen (R-005-01); display name only to Members of shared Groups |
| I: User data kept after account deletion | User e-mail, display name | D-20 anonymisation; "Deleted user" (R-021-03) |
| I: Invitation e-mail or token leaks | Invitation e-mail | token shown once, stored hashed (SR-05); e-mail only to the Owner |
| T/R: Audit entries changed or removed | Audit entries | SR-04; removed only with the Group (D-19) |
| I: Audit entries reveal personal data | Audit entries | ids and action only (R-013-02, SR-06) |

## 8 NFR checks

| NFR | How it is checked | Pass value |
|---|---|---|
| NFR-01 | k6 (`grafana/k6:1.3.0`) script `02_output/perf/contact-list.js` against the local stack seeded by `perf/seed-10k.sql` (10,000 Contacts in one Group, 5 Attributes and 3 Tags each); 20 virtual Users for 2 minutes mixing list, search and filter requests | `http_req_duration` p(95) < 500 ms |
| NFR-02 | Timed Export of the 10,000 seeded Contacts (k6 single iteration, `perf/export.js`) | duration < 10 s, file has 10,000 rows |
| NFR-03 | NFR-01 run with `perf/seed-capacity.sql`: 50 Groups, 200 Users, one Group with 10,000 Contacts | NFR-01 and NFR-02 pass values hold |
| NFR-04 | Runtime demonstration: `/actuator/health` UP, `restart: unless-stopped`, `docker compose restart backend` → health UP again | recovery < 60 s |
| NFR-05 | Integration test: 10,000 Contacts with `deletedAt` 31 days ago, `RemovalJob.run()` timed | < 60 s and 0 rows left |
| NFR-06 | Playwright projects chromium, firefox, webkit run the e2e suite | all pass in the three browsers |
| NFR-07 | `@axe-core/playwright` on every screen at the three widths; keyboard walk-through of E2E-01 | no Critical or Serious violation; walk-through recorded |
| NFR-08 | `src/i18n/catalogs.test.ts` (key parity, no empty text); screenshots of each screen in both languages | test passes; screenshots exist |
| NFR-09 | As DoD-P02 (human, staging) | under 2 minutes |

## 9 User interface

### 9.1 Screens

Wireframe ids for `/wireframes`. Every screen has the four states (UI-14) and is drawn at the three widths (UI-22).

| Id | Screen | Route | Stories, AC | Owner-only actions |
|---|---|---|---|---|
| WF-01 | Sign-in | `/sign-in` | US-001 AC-001-01, AC-001-02, EX-001-01 | — |
| WF-02 | Create an account | `/sign-up` | US-001 EX-001-05 | — |
| WF-03 | Confirm your e-mail | `/verify-email` | US-001 AC-001-05 | — |
| WF-04 | Create a Group | `/groups/new` | US-002 AC-002-01, AC-002-04 | — |
| WF-05 | No Groups yet | `/` (no Group) | US-001 R-001-07, EX-002-06 | — |
| WF-06 | Contact list (search, sort, filters, chips, checked rows, Export) | `/groups/:groupId/contacts` | US-014, US-015, US-016 | — |
| WF-07 | Contact details (Fields, Attributes, Tags, History link, Merged from, Undo merge, Move to recycle bin) | `/groups/:groupId/contacts/:contactId` | US-014 R-014-06, US-012, US-018 R-018-05, US-019 | — |
| WF-08 | Contact form (add/edit, Attributes, Tags with suggestions, duplicate e-mail warning) | `/groups/:groupId/contacts/new`, `…/:contactId/edit` | US-006, US-007, US-008, US-009 | — |
| WF-09 | History | `…/:contactId/history` | US-010 AC-010-01 | — |
| WF-10 | Version (read-only, Restore) | `…/:contactId/history/:number` | US-010 AC-010-02, US-011 | — |
| WF-11 | Recycle bin (restore, Delete permanently) | `/groups/:groupId/recycle-bin` | US-012, US-013 | Delete permanently |
| WF-13 | Members and Invitations | `/groups/:groupId/members` | US-004, US-005 | whole screen |
| WF-14 | Group settings (rename, Move Group to recycle bin) | `/groups/:groupId/settings` | US-003 | whole screen |
| WF-15 | Deleted Groups | `/deleted-groups` | US-003 AC-003-03, AC-003-04 | whole screen |
| WF-16 | Invitation (accept) | `/invitations/:token` | US-004 AC-004-02 … 04 | — |
| WF-17 | Possible duplicates | `/groups/:groupId/duplicates` | US-017 | — |
| WF-18 | Merge | `/groups/:groupId/merge?first=&second=` | US-018 | — |
| WF-19 | Account (language, Delete my account) | `/account` | US-020, US-021 | — |
| WF-20 | Not found | `*` | BR-02, E2E-03 | — |

(WF-12 is not used.)

### 9.2 Navigation

Header (UI-10): text logo "Kontakti"/"Contacts", Group switcher (US-002), navigation Contacts · Recycle bin (Owner, Editor) · Possible duplicates (increment 2) · Members and Settings (Owner), user menu (language, Account, Deleted Groups for Owners, Sign out). Mobile: navigation behind the menu button (UI-23); Contact list rows become cards below 768 px (UI-16); primary action "Add a Contact" sticky at the bottom on mobile.

### 9.3 Tokens and components

Tokens `src/styles/theme-wireframe.css` (active) and `theme-final.css` (UI-03, UI-04, UI-06, UI-07); light mode only (ui-design.md overrides UI-05). Components: shadcn/ui Button, Card, Input, Label (in the skeleton) plus Dialog (confirmations, UI-15), Select, Checkbox, Badge (Tags, chips), Table, Toast, Skeleton, DropdownMenu (user menu, Group switcher), Popover/Command (Tag suggestions), added by the stories that need them; Lucide icons with labels (UI-17). Copy tone: calm, plain, short (ui-design.md).

## 10 Traceability

Every ID of the stories maps to the story's row in §4 plus the sections below.

| R / AC / EX / BR / SR / SB / NFR | Section |
|---|---|
| R-001-01 … 07, AC-001-01 … 06, EX-001-01 … 05 | §4 US-001, §5, §6.2 (SR-01), §9.1 WF-01 … 03, 05 |
| R-002-01 … 05, AC-002-01 … 05, EX-002-01 … 06 | §4 US-002, §3, §9.1 WF-04, WF-05 |
| R-003-01 … 06, AC-003-01 … 05, EX-003-01 … 06 | §4 US-003, §5, §9.1 WF-14, WF-15, ADR-10 |
| R-004-01 … 09, AC-004-01 … 06, EX-004-01 … 08 | §4 US-004, §5, §6.2 (SR-05), §9.1 WF-13, WF-16 |
| R-005-01 … 05, AC-005-01 … 05, EX-005-01 … 05 | §4 US-005, §5, §9.1 WF-13 |
| R-006-01 … 07, AC-006-01 … 06, EX-006-01 … 09 | §4 US-006, §3, §5, §9.1 WF-08 |
| R-007-01 … 04, AC-007-01 … 05, EX-007-01 … 05 | §4 US-007, §5, §9.1 WF-08 |
| R-008-01 … 06, AC-008-01 … 04, EX-008-01 … 07 | §4 US-008, §3, §5 |
| R-009-01 … 06, AC-009-01 … 05, EX-009-01 … 07 | §4 US-009, §3, §5 |
| R-010-01 … 03, AC-010-01 … 03, EX-010-01 … 05 | §4 US-010, ADR-07, §9.1 WF-09, WF-10 |
| R-011-01 … 04, AC-011-01 … 04, EX-011-01 … 04 | §4 US-011, ADR-07, §5 |
| R-012-01 … 05, AC-012-01 … 05, EX-012-01 … 06 | §4 US-012, ADR-10, §9.1 WF-11 |
| R-013-01 … 03 (R-013-04 dropped, D-27), AC-013-01 … 03, EX-013-01 … 04 | §4 US-013, §6.2 (SR-06), ADR-10 |
| R-014-01 … 06, AC-014-01 … 05, EX-014-01 … 07 | §4 US-014, §3 (derived values), ADR-02, §9.1 WF-06, WF-07 |
| R-015-01 … 07, AC-015-01 … 05, EX-015-01 … 05 | §4 US-015, ADR-02 |
| R-016-01 … 07, AC-016-01 … 04, EX-016-01 … 07 | §4 US-016, ADR-09, `export-xlsx.schema.json` |
| R-017-01 … 05, AC-017-01 … 05, EX-017-01 … 07 | §4 US-017, ADR-12 |
| R-018-01 … 07, AC-018-01 … 05, EX-018-01 … 07 | §4 US-018, §5, §9.1 WF-18 |
| R-019-01 … 03, AC-019-01 … 03, EX-019-01 … 03 | §4 US-019, §5 |
| R-020-01 … 03, AC-020-01 … 03, EX-020-01 … 02 | §4 US-020, §1 (i18n), §9.1 WF-19 |
| R-021-01 … 03, AC-021-01 … 02, EX-021-01 … 03 | §4 US-021, §7 |
| BR-01, BR-02 | §6.1, ADR-02 (AR-04), ADR-04, §7 |
| BR-03, BR-04 | §6.1, ADR-04 |
| BR-05, BR-06 | §4 (transaction rule), ADR-02, ADR-07 |
| BR-07, BR-08 | ADR-10, §4 US-003, US-012, US-013 |
| BR-09 | §4 US-012, US-014 … US-017 |
| BR-10 | §1 `AuditService`, §6.2 (SR-04), D-17 |
| BR-11 | §3 (case-insensitive keys, Tags, e-mails), schema unique indexes |
| SR-01 … SR-09 | §6.2 |
| SB-01 … SB-14 | §6.2 |
| NFR-01 … NFR-09 | §8 |
