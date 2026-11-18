# Review of INC-2

> For: agent (writes), human (reads) · Written in: phase 4 (`/review INC-2`) · Source: stories US-017, US-018, US-019, US-021, `docs/02_specification.md`, `docs/wireframes/`, `git diff 38683e5..d24f1df` · Procedure: `skills/review` · Agent: writes

- Session: R /review INC-2, 2026-11-18
- Range: `38683e5..d24f1df` (first story commit `12e656f`, last `d24f1df`); 92 files, +4,609 / −87 lines without logs and images.
- Paths: `B/` = `02_output/backend/src/main/java/app/contacts/`, `BT/` = `02_output/backend/src/test/java/app/contacts/`, `F/` = `02_output/frontend/src/`, screens = `out/logs/screens/review-inc2/`.
- Method: the commit and test-first checks were run directly. Separate read-only reviewers did four jobs: the security review, the rule-by-rule review of US-017 and US-018, the rule-by-rule review of US-019 and US-021, and the runtime and wireframe check. Each finding below was checked against the code, a live request to the local stack on port 8090, or a screenshot. The live checks left data in the local stack (see "Notes").

## Result

- Critical: 0 · High: 0 · Medium: 6 · Low: 22.
- **DoD-I is met.** Six Medium findings remain open; none blocks the increment.

## Definition of Done (increment)

| ID | Result | Evidence |
|---|---|---|
| DoD-I-01 | met | US-017, US-018, US-019 and US-021 are "done" with DoD-S met (`docs/00_progress.md`). US-013's DoD-S-01, open since INC-1 (F-14 of INC-1), is met after US-018. The DoD-S-07 slips are recorded, except the three subjects in F-28. |
| DoD-I-02 | met | No Critical or High finding. |
| DoD-I-03 | met | Every INC-2 flow and E2E-04 passed on the running stack in both languages (table below). E2E-08, an INC-1 scenario, fails (F-05). |
| DoD-I-04 | met with findings | 66 screenshots at 375, 768 and 1440 px (UI-22). WF-17, WF-18, WF-07 ("Merged from", Undo merge) and WF-19 match their wireframes in structure, card order and English wording. Deviations: the header at 768 px (F-06), loading states (F-17), and the Low items F-18 … F-24. WF-17's Merge buttons are secondary rather than primary, which follows UI-10 (one primary action). |

### Flows exercised at runtime (DoD-I-03)

All passed on `http://127.0.0.1:8090`, signed in as the seeded Owner (Marko), an Editor and a Viewer, plus throwaway Users created for the review:

- Navigation: "Possible duplicates" in the header for every Role; the user menu holds the languages, Account, Deleted Groups and Sign out.
- US-017: the empty state, then all three reasons (same e-mail in another case, same phone in another format, similar name with same company, e.g. "Žiga Čuk"/"Ziga Cuk"). Merged Contacts are excluded and undone ones come back. A Viewer sees no Merge button. A forced 500 shows the error state, and "Try again" recovers.
- US-018 (AC-018-01 … 05):
  - The Primary contact is the Contact created first (D-74), and the Phone is preselected from the Other contact.
  - A missing Attribute choice gives "Choose a value for kompetenca." (400).
  - The same Contact twice gives "These Contacts cannot be merged."; a Viewer gets 403.
  - After the Merge, the Primary contact has Version 4 `merged`, the Other Version 2 `merged`, and WF-07 shows "Merged from J. Novak".
- US-019 (AC-019-01 … 03, EX-019-01): the confirmation, then both Contacts back with `mergeUndone` Versions. An edit after the Merge gives the R-019-02 text in the dialog. A second undo gives 409 `merge.alreadyUndone`, and a Viewer gets 403.
- E2E-04 end to end, in English and Slovenian.
- US-021:
  - WF-19 language choice, kept after reload.
  - A mismatched typed e-mail keeps "Delete" disabled; another letter case is accepted.
  - An Owner of a Group in the recycle bin is refused with the Group's name (EX-021-02).
  - A throwaway Editor deleted their account:
    - the browser went to sign-in, and signing in again failed;
    - the old token gets 401 on `/me` and on Group endpoints (EX-021-03);
    - the Membership is gone;
    - History and Version show "Deleted user" / "Izbrisan uporabnik" for its change.
  - AC-021-02 was not clicked on the seeded Owner's own account; EX-021-02 covers the same rule.
- Security, live:
  - Non-Members get 404 on duplicates, merge and undo; Viewers get 403 on merge and undo.
  - A Contact or Merge id of another Group under one's own Group path gives 404 (no existence leak).
  - Malformed merge bodies (empty, `null`, array, bad UUID, unknown Field, unknown choice) give 400, never 500.
  - Audit entries `contacts.merged` and `contacts.mergeUndone` hold ids only.
  - The backend log stayed empty during the run.

No horizontal overflow at any width, no untranslated catalog key, and no console error apart from the expected 4xx and the dialog warning in F-19.

### Commit checks

- All 28 `feat` commits since `38683e5` carry `Red:` lines (DoD-S-02).
- Two commits are over 400 lines (dd77b96, dfafe46) and ten subjects are over 72 characters. D-70 (i), D-72 (j), D-74 (c) and D-76 (i) record all of them except three subjects (F-28).

## Findings

Severity follows `general/quality/severity-scale.md`. This review gives no fixes; the next session decides them.

### Medium

| F | Story | Evidence | What is wrong |
|---|---|---|---|
| F-01 | US-018 (D-21, T-47, T-48) | `F/features/recycle-bin/DeletePermanently.tsx:39-44` (title and "This cannot be undone." only); `B/service/PermanentDeleteService.java:62-65, 80-83`; D-21 human answer (`decisions-log.md:299`: "the confirmation names them"); `docs/wireframes/WF-11_recycle-bin.html:22` ("Also deleted: the Contacts merged into it: …"); D-59 (d) deferred the line to T-48 | Permanently deleting a Primary contact also deletes every Contact merged into it, as D-21 requires. But the confirmation does not name them, and no API field gives the recycle bin their names. The Owner irreversibly deletes Contacts without being told. No decision records the omission (D-72 does not mention it). |
| F-02 | US-021 (R-021-01, D-20) | `B/service/UserService.java:103-117` (`accounts.purge` inside the `@Transactional` method, after `saveAndFlush`, before the commit); `B/auth/FirebaseAccountDeleter.java:40-46`; `B/service/UserService.java:128-135` (`create`) | The Firebase account is deleted before the database commits. In two cases Firebase has deleted the account but the database rolls back: (a) the commit fails after `purge`; (b) Firebase deletes the account but the call times out and throws. The User row then keeps its e-mail and display name (against D-20), and the Memberships stay. If the person signs up again with that e-mail, they get a new uid and hit `app_user_email_uq` in `create`. `findByFirebaseUid` finds nothing, so every request fails with 500 (as F-04 of INC-1). D-76 (b) covers only "Firebase refuses → roll back". No test covers either case. |
| F-03 | US-019 (R-019-02, R-019-03, EX-019-02) | `B/service/MergeService.java:155-162, 170-182`; `B/service/ContactService.java:278` (restoring writes a `restoredFromRecycleBin` Version); `ContactService.requireActive` :468-473 | The two rules conflict, and no decision records the conflict. The R-019-03 message tells the User to "restore the Contact from the recycle bin first", but restoring writes a new Version. Every later undo is therefore refused with R-019-02 `merge.changedAfter`, so following the message never leads to an undo. The "Other contact in the recycle bin" branch cannot be reached either: a merged Contact cannot be moved to the recycle bin (`contact.merged`). D-75 (c) does not address this. It needs a decision, e.g. whether a recycle-bin round trip counts as a change. |
| F-04 | all INC-2 (also INC-1) | `docs/02_tasks.md:55-62, 74`. No `AccessMatrixIT`, `OpenApiContractIT`, `LogHygieneIT`, `02_output/perf/` or `scripts/check-recovery.sh` exists, and no commit names T-36 … T-43. Of T-36, only `ArchitectureTest.java` exists. springdoc is off at runtime (`/v3/api-docs` → 404). | The cross-cutting tasks T-36 … T-43, planned "after the last story of INC-1", were never done, and so T-53 has nothing to extend. As a result: no automated test checks any endpoint as a non-Member (SR-02, specification §6.2); only Viewer-403 for merge and undo is tested end to end; no test compares the code with `openapi.yaml` (AR-02, DoD-P04, see F-15); no test checks the logs for personal data (SR-08); and the NFR checks (NFR-01 … NFR-04, NFR-06 … NFR-08) are missing. The live checks of this review found no hole, but nothing stops a regression. The INC-1 review did not notice the gap. No session in the plan picks these tasks up after the last increment. |
| F-05 | US-002, US-003 (INC-1) | `02_output/frontend/e2e/e2e-08-group-lifecycle.spec.ts:34-35`; `F/features/groups/GroupRoute.tsx:24` (last changed in ade85d4, before INC-2); `out/logs/implement-us019-e2e08-rerun.txt`; D-75 (j) | The frozen E2E-08 fails. After a reload, a Member of a Group in the recycle bin stays on `/groups/<id>/…`, and `GroupRoute` shows "This page cannot be found." instead of "You have no Groups yet." (R-002-05, EX-002-06). INC-2 did not cause it (no INC-2 change touches `GroupRoute` or the Group services). But the INC-1 goal "E2E-05 … E2E-08 pass" (`docs/02_tasks.md:11`) was never true: before US-019 only `acceptance/` was run. |
| F-06 | US-021 (navigation to WF-19), US-002 (header) | `nav_owner-user-menu_en_768.png`, `WF-17_owner_en_tablet.png` | At 768 px the Owner's header row is full: Group switcher cut to "Konfe…", the navigation items wrap onto two lines, and the user-menu trigger is still in the DOM but drawn as nothing. Clicking the empty spot opens the menu, but a User cannot see where Account (WF-19) and Sign out are (UI-22, UI-24). INC-1 F-07 ("user menu shows only A…") is still open; the new item "Possible duplicates" made it worse. |

### Low

| F | Story | Evidence | What is wrong |
|---|---|---|---|
| F-07 | US-018 (R-018-07, BR-06) | `BT/service/MergeServiceTest.java:68-75` (stubs `lockedCurrent`); `B/service/MergeService.java:200-207` | No test of the Merge asserts that a stale Version number gives `version.conflict` with nothing changed, or that a Contact of another Group gives 404. The shared check `ContactService.lockedCurrent` is tested through editing (US-007). If the Merge stopped calling it, its tests would fail with a NullPointerException, not on the rule. Only the frozen EX-018-04 checks the rule itself. |
| F-08 | US-018 (D-72 (a)) | `B/service/MergeService.java:213-217` (`lockedInIdOrder`) | The 404, stale and not-active checks run per Contact, so the order differs from D-72 (a) for mixed failures. For example, if the first Contact by id is stale and the second belongs to another Group, the answer is 409, not 404. No data leaks. |
| F-09 | US-018 (R-018-01, BR-03) | `F/app/routes.tsx` (route `merge`), `F/features/duplicates/MergePage.tsx`; `WF-18_viewer_en_desktop.png`, `WF-18_viewer-refused_en_desktop.png` | A Viewer who opens the merge URL directly gets the full WF-18 form with an enabled Merge button. Only the server refuses (403). R-018-01 says the Owner or an Editor opens the Merge screen. |
| F-10 | US-018 (WF-18) | `F/features/duplicates/MergePage.tsx:65` (Primary contact kept in `useState`); `F/api/duplicates.ts:121-133` (no refetch on error, unlike `useUndoMerge` :159) | After a `version.conflict`, every retry sends the old Version numbers until the page is reloaded. A background re-read can also pair a fresh Other contact with a stale Primary contact. |
| F-11 | US-018 | `WF-18_merge` (L-6 of the runtime check): `/groups/{g}/merge?first=…&second=00000000-…` and `/merge` without parameters | A missing or unknown Contact shows "Something went wrong. / Try again", although the API answered 404. It should show "This page cannot be found." (WF-20). |
| F-12 | US-018 | `WF-07_merged-other_{en,sl}_desktop.png`, `WF-08_edit-merged-contact_sl_desktop.png` | A Contact merged into another (state `merged`) opens on WF-07 like an active one. Nothing says it was merged or into which Contact, although the API returns `mergedInto`. Editors get "Edit", and saving is then refused with 409. |
| F-13 | US-017, US-018 (ES-13) | `F/i18n/sl.json:347` ("Enak e-naslov") vs "E-pošta" and "e-poštni naslov" (`:53`, `:194`, `:213`) | Two Slovenian terms for one Field. D-70 (h) approved the text, so this is a consistency note only. |
| F-14 | US-017 (ES-10, ES-11) | `B/service/Normaliser.java:55` ("The search text:the …", space lost in 12e656f); `B/service/DuplicateService.java:115` ("stored empty", but blank e-mails are `null`, `domain/ContactFields.java:15-17`); `BT/service/DuplicateServiceTest.java:195` (`ex01705NonMemberGetsNotFound` tests a non-Member; EX-017-05 is the same e-mail in two Groups) | A typo in an unrelated Javadoc, a Javadoc that contradicts the code, and a test name that does not match its case. |
| F-15 | US-017, US-018, US-019 | `docs/02_contracts/openapi.yaml:441-451, 467-480`; live: merge with `primaryVersionNumber: -5` → 409 `version.conflict` | The server and the contract disagree. The contract's `minimum: 1` should give 400, but the merge answers 409. Undo answers 400 `validation`, which the contract does not list. The contract lists no 403 for duplicates, although unverified accounts get 403 `auth.emailNotVerified`. The missing contract test (F-04) would catch these. |
| F-16 | US-021 (R-021-02) | `B/service/UserService.java:109-113`; `B/service/GroupService.java:62-65`; `B/service/InvitationService.java:168` | Nothing locks the User or their Memberships during `closeAccount`. If the same person creates a Group or accepts an Invitation in another tab at the same moment, the request can slip between the Owner check and `dropAllOfUser`. The result is a Group without an Owner, or a Membership of a deleted User. |
| F-17 | US-017, US-018, US-021 (UI-14) | `WF-17_state-loading_sl_mobile.png`, `WF-18_state-loading_sl_mobile.png`, `WF-19_state-loading_sl_mobile.png` | The loading state is plain text "Nalaganje …" from the shared `GroupsPending`, where the wireframes draw skeleton bars. React Query retries for about 7 s before the error state. |
| F-18 | US-018 (WF-18) | `WF-18_merge_en_desktop.png` (Tags), `WF-18_merge-novak_en_desktop.png` (Attributes) | An empty Attributes or Tags card is a bare heading, where WF-07 says "No Attributes." / "No Tags.". |
| F-19 | US-019 (UI-19) | console on WF-07 when the dialog opens | The Undo merge confirmation has no description or `aria-describedby`, and Radix warns about it. The wireframe draws no text, but D-66 (f) gave the Remove-from-Group dialog a description for the same reason. |
| F-20 | US-021 (UI-15) | `WF-19_account_after-delete_sl_desktop.png` | After deleting the account, the User lands on sign-in with no confirmation that the account was deleted. |
| F-21 | US-021 (WF-19; INC-1 F-08, F-32) | `WF-19_account_en_{desktop,tablet}.png` | On `/account` the header shows only "Contacts", where the wireframe has the full navigation. The content is a centred column of about 670 px, where the wireframe has full-width, left-aligned cards. |
| F-22 | US-021 (R-021-03) | `F/i18n/userName.ts:4-9`; `F/api/problem.ts` (`shownNames`) | "Deleted user" is signalled by the string `user.deleted` in the same field as real names, so a User whose display name is `user.deleted` is shown as "Deleted user". `shownNames` also rewrites every string parameter of every problem, not only names. |
| F-23 | US-021 (ES-13; D-76 (h)) | `B/service/UserService.java:104` `closeAccount`; `MembershipRepository.dropAllOfUser`; `AccountDeleter.purge` | Specification §4 names the method `deleteAccount`. The domain names were changed only to satisfy SpotBugs (EI_EXPOSE_REP2), as in D-59 (f) and D-66 (e). The specification and the code now disagree on the name. |
| F-24 | US-018 (D-74) | `docs/02_specification.md` §4 row US-018 ("First Contact = first in R-014-02 order (D-25 (b))") | D-74 (a) makes the Contact created first the Primary contact, so this row no longer describes the code. The specification can only change through a decision. |
| F-25 | US-019 (D-75 (e)) | `BT/service/MergeServiceTest.java` | No test checks the lock order "Merge, then the Contacts in id order". It is covered only indirectly, through the stubbed `findForUpdate`. |
| F-26 | US-009 (INC-1 F-13, D-67, D-74 (d)) | `out/logs/implement-us018-frontend-test-nocoverage.txt` | `TagsField.test.tsx` still times out at random under `vitest --coverage`; the last sessions ran the frontend tests without coverage (355/355). No session owns it. |
| F-27 | — (decision log) | `docs/decisions-log.md:221-222` vs `:296-299` | The D-20 record still says "Human response: none / Resolution: pending", although the batch answer at :296-299 resolved it. |
| F-28 | US-017, US-018 | `0ef6794` (subject 76 characters), `dd77b96` (73), `5f0d84f` (73) | DoD-S-07 slips that the stories' records do not mention (D-72 (j) records dd77b96's size, not its subject). History is not to be rewritten; this only records them. |

## Decisions pending review

| Decision | Review result |
|---|---|
| D-70 (answered) | No objection; (a) pairing company-only Contacts follows from the Normalised full name and is tested. |
| D-72 (a) … (j) | Accepted, with two reservations: the code's order of checks differs from (a) for mixed failures (F-08), and the D-21 confirmation that names the merged Contacts is neither built nor decided (F-01). |
| D-74 (a), (b) | Accepted: the Contact created first matches AC-018-01, AC-018-02 and AC-018-05, and it is only a preselection the User can change. Specification §4 row US-018 is now out of date (F-24). (d) is still unowned (F-26). |
| D-75 (answered) | (c) needs an addendum for the R-019-02 / R-019-03 conflict (F-03). (j) is right that E2E-08 is not caused by INC-2; it is raised as F-05. |
| D-76 (a), (c) … (g), (i) | Accepted. (d): "Deleted user" outside Versions as well is accepted (the sentinel is F-22). |
| D-76 (b) | Accepted for a Firebase refusal, but it leaves the partial failures of F-02 open. |
| D-76 (h) | Objection (Low, F-23): a technical rename of a domain action against ES-13. Either keep the specification's names (e.g. with a targeted SpotBugs exclusion) or align specification §4 through a decision. |

## Notes

- The live checks left data in the local stack on port 8090:
  - Emulator and app Users `secrev-a`, `secrev-e`, `secrev-v`, `secrev-x` @example.test, and `review.inc2.owner@example.si` (owns "Review INC-2 throwaway group", which is in the recycle bin). `secrev-d@example.test` and `review.inc2.throwaway@example.si` were deleted through `DELETE /me`.
  - Groups "SecRev G1" (Contacts, an undone Merge, Invitations) and "SecRev G2".
  - In "Konferenca 2025", Contacts Janez Novak, J. Novak, Maja Zupan, Maja Z., Žiga Čuk and Ziga Cuk (merged into Žiga Čuk; that Merge cannot be undone because Žiga Čuk was edited afterwards). Also the new Tag `vip`, Merge and undo Audit entries, and Marko's language set to `sl`. Marko's e-mail in this stack is not the one of the seed.
- Reset the stack's volumes before the next run that needs the seed state (`scripts/seed-local.sh` fails on Users created outside the seed, INC-1 notes).
- Screens: 66 screenshots in `out/logs/screens/review-inc2/`.
