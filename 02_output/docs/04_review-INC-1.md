# Review of INC-1

> For: agent (writes), human (reads) · Written in: phase 4 (`/review INC-1`) · Source: stories US-001 … US-016, US-020, `docs/02_specification.md`, `docs/wireframes/`, `git diff 1c757a1..f80e83b` · Procedure: `skills/review` · Agent: writes

- Session: R /review INC-1, 2026-11-17
- Range: `1c757a1..f80e83b` (first story commit `f4a078b`, last `f80e83b`); 298 files, +25,426 / −54 lines under `backend/` and `frontend/`.
- Paths: `B/` = `02_output/backend/src/main/java/app/contacts/`, `BT/` = `02_output/backend/src/test/java/app/contacts/`, `F/` = `02_output/frontend/src/`, screens = `out/logs/screens/review-inc1/`.
- Method: the commit and test-first checks were run directly. The backend security review, the rule-by-rule review per story (three groups), and the runtime and wireframe check were done by separate read-only reviewers. Each finding below was checked against the code, a live request to the local stack on port 8090, or a screenshot. The live checks left data in the local stack (see "Notes").

## Result

- Critical: 0 · High: 0 · Medium: 14 · Low: 20.
- **DoD-I is not met yet, but only because of DoD-I-01; no review finding blocks it.** See "Definition of Done (increment)".

## Definition of Done (increment)

| ID | Result | Evidence |
|---|---|---|
| DoD-I-01 | not met | US-012, US-013 and US-020 are still "in progress" (DoD-S-01 open; `docs/00_progress.md`). US-012 AC-012-01 and US-020 EX-020-02 wait for the approved `stubGet` change (D-51, D-64, D-65), which only `/acceptance-tests` may apply. US-013 EX-013-03 cannot pass before US-018 (Merge, INC-2), so DoD-I-01 cannot be met within INC-1 as planned (see F-14). The other 14 stories meet DoD-S, apart from the unrecorded DoD-S-07 slips in F-34. |
| DoD-I-02 | met | No Critical or High finding. |
| DoD-I-03 | met | Every flow passed on the running stack (table below); no console error other than the expected 400 and 404. |
| DoD-I-04 | met with findings | 119 screenshots at 375, 768 and 1440 px (UI-22). The screens match their wireframes in structure and wording, except for the header navigation (F-07, F-08) and the Low items F-27 … F-33. WF-19 Account is not built (D-44: US-021, INC-2); the language choice is in the user menu. |

### Flows exercised at runtime (DoD-I-03)

All passed on `http://127.0.0.1:8090`, signed in as the seeded Owner, plus one new verified User for the Invitation:

- Sign-in, wrong password, sign-up to WF-03, existing e-mail, WF-05 for a new User.
- Contact: add with an Attribute and Tags, duplicate e-mail warning, Tag suggestion, field errors, edit.
- History and Versions: History, Version 1, restore with confirmation (Version 3 "restored · from Version 1").
- Recycle bin: move to recycle bin, restore, Delete permanently.
- Contact list: search, no-match state, sort, Filters panel, chips, check rows, Export and Export checked (the `.xlsx` opens with the expected columns).
- Groups: create a Group (with the empty-name error), switch Group, rename, move the Group to the recycle bin, then restore it from Deleted Groups.
- Members and Invitations: create an Invitation; the second User accepts it on WF-16; change the Role; Remove from Group.
- Language: Slovenščina ↔ English, kept after reload, no untranslated key found.
- WF-20 for an unknown URL and an unknown Contact; the mobile menu; the sticky "Add a Contact" at 375 px.

No horizontal overflow at any width.

### Commit checks

- All 135 `feat` commits since `1c757a1` carry `Red:` lines (DoD-S-02).
- Subjects over 72 characters, and commits over 15 files or 400 lines (logs, lock files and images not counted), that the story records do not mention: F-34. The records already note 116483b, 577e900, bdb4dec, 5168f66, daf3e85, bacf2c9, 24e39f9, 2ae5475, b7b36b7 and c1b76ff.

## Findings

Severity per `general/quality/severity-scale.md`. No fixes are given here; the next session decides them.

### Medium

| F | Story | Evidence | What is wrong |
|---|---|---|---|
| F-01 | US-009 (also US-006, US-007, US-011) | `B/service/ContactValidator.java:101-116`; contract `ContactWrite.tags maxItems: 1000` (`docs/02_contracts/openapi.yaml:652`); live: POST with 3,000 distinct Tags → 201 after 7.1 s | The number of Tags per Contact is not limited on the server (SB-01; the contract says at most 1,000, and Attributes are capped at 50). One Editor request up to the 2 MB nginx limit (about 200,000 Tags) holds a transaction for minutes. It also creates Tag rows that stay in the Group's suggestions for good, and writes a huge Version snapshot. The rate limit does not help, because one request is enough. |
| F-02 | US-009 | `B/service/TagService.java:83` (`findInGroup(…).orElseGet(() -> tags.add(…))`); unique index `tag_name_uq` (`V1__schema.sql:106`); live: 12 parallel saves with one new Tag → four 500s; backend log WARN `Detail: Key (group_id, lower(name::text))=(…, racetagx) already exists` | Two saves that create the same new Tag at once lose one save with 500 `internal` (R-009-03/04 expect the existing Tag to be reused). Hibernate's WARN also writes the constraint `Detail`, which holds the data values, to the log (ES-07, SR-08). The same applies to every unique violation, e.g. the e-mail in F-04. D-55 (e) left the race to this review. |
| F-03 | US-001 (`ProblemHandler`), US-006, US-014 | `B/web/ProblemHandler.java:125` (catch-all → 500, logged at ERROR); `B/service/ContactQueryService.java:93` (`(page - 1) * PAGE_SIZE` in `int`); live 500s: `GET …/contacts/email-matches` without `email`, `POST /groups` without a JSON content type, `GET …/contacts?page=50000000` | Malformed requests are answered 500 instead of 400 `validation` (spec §5, SB-01). A large `page` overflows to a negative OFFSET instead of an empty Page with the correct total (EX-014-06; no test uses a large page). Any Member can fill the ERROR log with stack traces. |
| F-04 | US-001 | `B/service/UserService.java:73-79`; `app_user_email_uq` on `lower(email)` (`V1__schema.sql:20`) | A Firebase account deleted outside the app (e.g. by the Operator in the console) and created again with the same e-mail gets a new uid. Provisioning hits the e-mail index, the fallback looks up the new uid, finds nothing and rethrows. Every request of that person then fails with 500, and the e-mail is logged as in F-02. No test covers it (R-001-04). Also, the stored e-mail is never refreshed from the token, so Invitation matching (`InvitationService.java:187`) uses the e-mail of the first sign-in. |
| F-05 | US-007 | `F/main.tsx:12-14` (React Query defaults: refetch on window focus); `F/features/contacts/EditContactPage.tsx:256` (`key={read.versionNumber}`) | If another Member saves while Ana's Contact form is open, the re-read on focus brings a new Version number and the form is mounted again. Her typed edits are lost without a message, and she never sees "Contact was changed by {changedBy}, reload" (AC-007-02, BR-06). |
| F-06 | US-006 (also US-004 Invitations, US-007) | `B/service/ContactValidator.java:55`; `F/features/contacts/contactFields.ts:49` | The e-mail syntax accepts `janez@.`, `janez@example.` and `janez@.si`. Spec §3 asks for "a domain with a dot", so an empty domain label must not pass (R-006-04, EX-004-06). The tests (`BT/service/ContactValidatorTest.java:150-171`, `contactFields.test.ts:64`) do not cover these cases. |
| F-07 | US-002 (header, Group switcher) | screens `WF-06_contact-list_tablet.png`, `WF-07_contact-details_tablet.png`, `WF-11_recycle-bin_tablet.png` vs `docs/wireframes/WF-06_contact-list_tablet.png` | At 768 px the wireframe puts the navigation on a second row. The build puts logo, Group switcher, navigation and user menu in one row: the Group name is cut ("Konferenca 20"), "Recycle bin" wraps onto two lines, and the user menu shows only "A…" (UI-22, DoD-I-04). |
| F-08 | US-002, US-003 | `WF-15_deleted-groups_desktop.png`, `WF-04_create-group-error_desktop.png`, `WF-20_not-found_desktop.png` | On routes without a Group id (`/deleted-groups`, `/groups/new`, unknown URLs), the header shows only "Contacts" while the switcher still shows the current Group. Recycle bin, Members and Settings are missing, although the wireframes show the full navigation there (§9.2). |
| F-09 | US-005 | `BT/service/MembershipServiceTest`, `HistoryServiceTest`, `ContactVersionRepositoryIT` | R-005-04 (Versions of a removed Member keep their name) has no unit or integration test; only the frozen AC-005-03 checks it (DoD-S-02). |
| F-10 | US-003 | `BT/repository/GroupRepositoryIT.java:104-121` | The BR-08 / AC-003-05 removal test asserts only that the Group and its Memberships are gone. It does not check Contacts, Versions, Attributes, Tags, Invitations or Audit entries, so a dropped `ON DELETE CASCADE` would pass every test outside the acceptance suite (SR-06). |
| F-11 | US-011 | `BT/service/ContactServiceTest`, `HistoryServiceTest` | R-011-02 (earlier Versions are kept after a restore) has no own unit or integration test; only the frozen AC-011-01 checks it. |
| F-12 | US-016 | `F/i18n/sl.json:224-225, 257-259` ("Označi …", "Izvozi označene", "Označenih: …") | The Slovenian UI uses "označeni", which the glossary lists as an avoided synonym of Checked contacts ("ambiguous", `business-rules.md`). This contradicts ES-13; D-61 (d), which chose these texts, is still pending review. |
| F-13 | US-009 | D-67; `F/features/contacts/TagsField.test.tsx`; `out/logs/implement-us005-frontend-test.txt` | 1 to 4 tests of the file hit Vitest's 5 s timeout in 2 of 4 full runs, because each renders the whole form and waits the real 300 ms suggestion delay. A test that fails at random breaks "no previously passing test fails" (DoD-S-01) and the CI gate (ES-14). |
| F-14 | US-013 | `docs/02_tasks.md` (INC-1 contains US-013; T-47 erases merged Contacts in INC-2); `docs/00_progress.md` | The frozen EX-013-03 needs Merge (US-018, INC-2), so US-013 cannot meet DoD-S-01, and INC-1 cannot meet DoD-I-01, inside INC-1. The plan does not say how this gate is passed. A decision is needed: for example, accept US-013's DoD-S-01 at the end of INC-2. |

### Low

| F | Story | Evidence | What is wrong |
|---|---|---|---|
| F-15 | US-002, US-003, US-006 … US-009 | client `.trim()` (`F/features/contacts/contactFields.ts:97`, `attributes.ts:163`, `tags.ts:190`, `F/features/groups/CreateGroupPage.tsx:22`, `GroupSettingsPage.tsx:49`); server `.strip()` (`B/service/ContactValidator.java:207, 215`, `B/service/GroupService.java:187`) | The browser and the server trim different characters: JS `trim()` removes U+00A0 and U+202F, Java `strip()` does not. Through the API, a name of only non-breaking spaces passes `contact.nameRequired` / `group.nameLength`, and trailing non-breaking spaces are stored (R-006-07). |
| F-16 | US-008, US-009 | `B/service/ContactValidator.java:152, 197` (`toLowerCase(Locale.ROOT)`); indexes on `lower()` (`V1__schema.sql:98, 106`) | Duplicate Attribute keys and Tag names are found with Java lower case, but the unique indexes use PostgreSQL `lower()`, which depends on the database locale. For letters such as İ or ß they can disagree, and the expected 400 becomes a 500 (BR-11). |
| F-17 | US-006 | `B/service/ContactService.java:131-141`; contract `email-matches` (`openapi.yaml:290`) | `email-matches` has no length check (contract `maxLength: 254`; 5,000 characters answer 200), and the contract parameter `excludeContactId` is not implemented. |
| F-18 | US-007, US-010 | `F/api/contacts.ts:65-68` vs `F/api/history.ts:65` | Saving a Contact does not mark the cached History as stale (restoring a Version does). An open or cached History shows the old Versions until it is read again. |
| F-19 | US-011 | `B/service/ContactService.java:250-253` | The restored Version's snapshot records the old snapshot's Tag spelling, not the spelling of the Tags actually put on. They differ only if a Tag of another spelling exists; this cannot happen while Tags are never renamed. |
| F-20 | US-012, US-013 | `B/service/PermanentDeleteService.java:69-78` | The removal job writes `contact.removed` for each due Contact, then runs a separate bulk DELETE. A Contact restored between the two keeps a wrong "removed" Audit entry (SR-04). |
| F-21 | US-001 | `B/auth/RateLimitFilter.java:116` | The per-User bucket map is never emptied, so memory grows with every User ever seen (SR-03). |
| F-22 | US-009, US-015 | `B/repository/ContactQueryRepository.java:71-73` (`WITHOUT_TAGS`), `B/repository/TagRepository.java:37-38` (`suggest`) | The two subqueries do not filter `contact_tag` by `group_id`, unlike `HAS_TAGS` and the Attribute filters. Today this is safe (Tag ids are checked against the Group, and the outer query is filtered), but it departs from AR-04. |
| F-23 | US-006, US-009, US-016 | `F/features/contacts/ContactForm.tsx:28-31` vs `contactFields.ts:35-46`; `B/service/TagService.java:25` vs `ContactValidator.MAX_TAG_LENGTH`; `F/api/tags.ts:6` vs `F/features/contacts/tags.ts:186`; `F/api/exports.ts:218` and `ExportButtons.tsx:177` vs `ExportService.MAX_CONTACTS` (D-61 (c)) | The same limit is written in two places (ES-11). |
| F-24 | US-015, US-016 | `B/web/ExportController.java:366-373` vs `B/web/ContactController.java:73-75`; `B/repository/ContactQueryRepository.java:150-152` | The mapping from request to `ContactQuery.Filters` is copied, so a new filter added in only one place makes the Export scope drift from the list (R-016-01). The list count calls `countExport(…, null)` (ES-11 names). |
| F-25 | US-003 … US-005 (frontend) | `F/features/groups/GroupSettingsPage.tsx:24`, `F/features/members/MembersPage.tsx:19` vs `useIsOwner` (`F/api/groups.ts:28-30`); problem → catalog text in `MemberList.tsx:49`, `InvitationsSection.tsx:105`, `InvitationPage.tsx:101-105`, `groupName.ts:17-21`, `DeletedGroupsPage.tsx:44`, `MoveGroupToRecycleBin.tsx:25` | The Owner check is copied instead of reused, and the problem → catalog text mapping is written six times, some copies with `error.params` and some without (ES-11). |
| F-26 | US-004, US-005, US-006 … US-011, US-014 | `B/service/InvitationService.java:182-194` (Group read twice, `activeGroup()` does not check the state, `view` takes a `PESSIMISTIC_WRITE` lock); `B/service/MembershipService.java:79-82` (reloads all Members to return one); `B/service/ContactService.java` (406 lines, seven jobs); `F/features/contacts/ContactForm.tsx:60-226` (one 165-line function); `B/service/Normaliser.java:30-31` (Javadoc says "one per line", code joins with spaces as D-52 (c) chose) | Simplicity and naming (ES-11): needless reads, a misleading name, long units and a Javadoc that contradicts the code. |
| F-27 | US-020 | `02_output/frontend/index.html:6`; nothing sets `document.title` | The browser tab title stays "Kontakti" after choosing English (R-020-02 "every text"). The visible header title does change. |
| F-28 | US-001 | `WF-01_sign-in-error_*.png`, `WF-02_sign-up_*.png` | The e-mail and password labels have no required marker (wireframe "E-mail *", UI-13), and the Google button has no icon placeholder. |
| F-29 | US-002 | screens at 768 and 1440 px, e.g. `WF-14_group-settings_desktop.png` | The current page is not marked in the navigation (the wireframes outline the active item). "Contacts" links to `/`, so it never gets `aria-current`. |
| F-30 | US-002 | `WF-06_contact-list_mobile.png` | At 375 px the menu button is an icon with a hidden label, while the wireframe shows the text "Menu". The user menu is on a second header row, not behind the menu button (UI-23). |
| F-31 | US-001 | `WF-05_no-groups_*.png` | WF-05 has no illustration placeholder and no Group switcher; a lone "Kontakti" link is shown instead. |
| F-32 | US-003 | `WF-14_group-settings_desktop.png`, `WF-20_not-found_*.png` | WF-14 is a narrow centred column of about 580 px, where the wireframe has full-width, left-aligned cards. WF-20 is plain left-aligned text, where the wireframe has a centred card. |
| F-33 | US-001, US-003, US-004 | `WF-03_confirm-email_*.png`, `WF-16_invitation_*.png`, `WF-15_deleted-groups_*.png` | Slovenian wording (D-37): the WF-03 button "E-poštni naslov je potrjen" is a statement, not an action; "Povabilo v Konferenca 2025" puts the Group name in the wrong case; "Obnovite jo" uses a singular pronoun after a plural noun. |
| F-34 | US-001, US-002, US-006, US-014 | `ffa2f0f` (16 files, 591 lines), `c5c558f` (subject 74 characters), `ec5a95d` (283-character subject: the body was committed as the subject), `596b51c` (418 lines), `055943e` (subject 75 characters) | DoD-S-07 slips that the stories' records and `docs/00_progress.md` do not mention. History is not to be rewritten; this only records them. |

## Decisions pending review

| Decision | Review result |
|---|---|
| D-55 (e) Tag race left to review | Raised as F-02. |
| D-61 (c) client copy of the Export limit | Accepted as a pre-check; listed in F-23. |
| D-61 (d) Slovenian "označeni" | Contradicts ES-13: F-12. |
| D-63 (j), D-66 (i) DoD-S-07 slips | Noted; the remaining unrecorded ones are F-34. |
| D-66 (a) … (h) Member management details | No objection; (e) `endMembership` instead of `remove` is accepted (SpotBugs reason recorded). |
| D-67 TagsField timeouts | Raised as F-13; option 1 fits. |

## Notes

- The live checks left data in the local stack on port 8090:
  - emulator Users `secrev1@example.test` and `review1794935127354@example.si` ("Rok Review");
  - Groups "secrev" (about 9 Contacts, one with 3,000 Tags) and "Review Beta";
  - Contacts Janez Novak and Maja Zupan in "Konferenca 2025".
- `scripts/seed-local.sh` fails with `duplicate key … app_user_firebase_uid_key` once the stack has Users created outside the seed. Reset the stack's volumes before the next run that needs the seed state.
- Once, while navigating fast in a script, the UI reviewer got HTTP 429 (rate limit) on `GET …/contacts/{id}`. It did not check whether the screen then showed its error state (UI-14); `/verify` could check this.
- Screens: 119 screenshots in `out/logs/screens/review-inc1/`.
