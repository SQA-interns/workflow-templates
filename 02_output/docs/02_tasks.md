# Tasks and increments

> For: agent (writes), human (reads) · Written in: phase 2 (`/plan`) · Source: stories, `docs/02_specification.md` · Agent: writes

Paths: `B/` = `02_output/backend/src/main/java/app/contacts/` (tests under `src/test/...` beside them), `F/` = `02_output/frontend/src/`. Every task is a vertical slice whose behaviour a test can observe; its tests name the `AC`, `EX` or `R` ids it covers. No story has more than six tasks, so one `/implement US-nnn` session covers each story.

## Increments

| Increment | Stories (in implement order) | Goal |
|---|---|---|
| INC-1 | US-001, US-020, US-002, US-006, US-014, US-007, US-008, US-009, US-010, US-011, US-012, US-013, US-015, US-016, US-003, US-004, US-005 | MVP (intent.md): sign in, Groups, Members, Contacts with Attributes, Tags, History, recycle bin, list, filters, Export; E2E-01 … E2E-03, E2E-05 … E2E-08 pass |
| INC-2 | US-017, US-018, US-019, US-021 | Possible duplicates, Merge, Undo merge, delete my account; E2E-04 passes |

Cross-cutting and NFR tasks (T-36 … T-43) run after the last story of INC-1; T-53 after INC-2.

## Tasks

| Task | Story | Covers (R, AC, EX) | Expected files | Depends on |
|---|---|---|---|---|
| T-01 | US-001 | R-001-02, R-001-03, R-001-04; AC-001-03, AC-001-04; EX-001-02 … EX-001-04; SR-01 | `B/auth/FirebaseTokenFilter.java`, `B/auth/CurrentUser.java`, `B/service/UserService.java`, `B/domain/User.java`, `B/repository/UserRepository.java`, `B/web/MeController.java`, `B/config/SecurityConfig.java` | — |
| T-02 | US-001 | R-001-05 (D-16), D-20 (deleted User → 401); AC-001-05 | `B/auth/FirebaseTokenFilter.java`, `B/service/UserService.java` | T-01 |
| T-03 | US-001 | SR-03 (100/min, 429 + `Retry-After`) | `B/auth/RateLimitFilter.java`, `B/config/AppProperties.java` | T-01 |
| T-04 | US-001 | Error model §5 base (`auth.*`, `notFound`, `validation`); SB-07 | `B/web/ProblemHandler.java`, `B/service/*Exception.java`, `F/api/apiClient.ts`, `F/api/problem.ts`, `F/i18n/*.json` | T-01 |
| T-05 | US-001 | R-001-01, R-001-06, R-001-07; AC-001-01, AC-001-02, AC-001-05, AC-001-06; EX-001-01, EX-001-05 | `F/auth/firebase.ts`, `F/auth/runtimeConfig.ts`, `F/auth/AuthProvider.tsx`, `F/auth/RequireAuth.tsx`, `F/features/account/{SignInPage,SignUpPage,VerifyEmailPage}.tsx`, `F/features/groups/NoGroupsPage.tsx`, `F/app/routes.tsx` | T-02, T-04 |
| T-06 | US-020 | R-020-01 … R-020-03; AC-020-01 … AC-020-03; EX-020-01, EX-020-02 | `B/web/MeController.java`, `B/service/UserService.java`, `F/i18n/format.ts`, `F/i18n/index.ts`, `F/app/UserMenu.tsx` | T-05 |
| T-07 | US-002 | R-002-01 … R-002-03; AC-002-01, AC-002-03, AC-002-04; EX-002-01 … EX-002-05; BR-01, BR-02 | `B/domain/{Group,Membership,Role}.java`, `B/repository/{GroupRepository,MembershipRepository}.java`, `B/service/{GroupService,AccessGuard,Permission}.java`, `B/web/GroupController.java` | T-04 |
| T-08 | US-002 | R-002-04, R-002-05; AC-002-02, AC-002-05; EX-002-06 | `F/features/groups/{CreateGroupPage,GroupSwitcher}.tsx`, `F/features/groups/currentGroup.ts`, `F/api/groups.ts`, `F/app/AppLayout.tsx` | T-05, T-07 |
| T-09 | US-006 | R-006-01 … R-006-05, R-006-07; AC-006-01 … AC-006-04, AC-006-06; EX-006-01 … EX-006-09; D-23 (phone) | `B/domain/{Contact,ContactFields,ContactVersion}.java`, `B/repository/{ContactRepository,ContactVersionRepository}.java`, `B/service/{ContactService,ContactValidator,VersionWriter,Normaliser}.java`, `B/web/ContactController.java` | T-07 |
| T-10 | US-006 | R-006-06; AC-006-05 | `B/web/ContactController.java` (`email-matches`), `F/features/contacts/ContactForm.tsx`, `F/features/contacts/NewContactPage.tsx`, `F/api/contacts.ts` | T-09, T-08 |
| T-11 | US-014 | R-014-01 … R-014-04; AC-014-01 … AC-014-03, AC-014-05; EX-014-01, EX-014-03 … EX-014-07; BR-09 | `B/service/{ContactQueryService,ContactQuery}.java`, `B/repository/ContactQueryRepository.java`, `B/web/ContactController.java` | T-09 |
| T-12 | US-014 | R-014-05, R-014-06; AC-014-04; EX-014-02 | `F/features/contacts/{ContactListPage,ContactTable,ContactDetailsPage}.tsx` | T-11, T-10 |
| T-13 | US-007 | R-007-01 … R-007-04; AC-007-01 … AC-007-05; EX-007-01 … EX-007-05; BR-06; D-23 (merged) | `B/service/ContactService.java`, `B/web/ContactController.java` | T-09 |
| T-14 | US-007 | AC-007-02 (conflict message in the UI), AC-007-04 | `F/features/contacts/EditContactPage.tsx`, `F/features/contacts/ContactForm.tsx` | T-13, T-12 |
| T-15 | US-008 | R-008-01 … R-008-06; AC-008-01 … AC-008-04; EX-008-01 … EX-008-07 | `B/domain/Attribute.java`, `B/repository/AttributeRepository.java`, `B/service/{ContactService,ContactValidator}.java`, `F/features/contacts/AttributesField.tsx` | T-13, T-14 |
| T-16 | US-009 | R-009-01, R-009-03 … R-009-06; AC-009-01, AC-009-03 … AC-009-05; EX-009-01 … EX-009-03, EX-009-06, EX-009-07 | `B/domain/{Tag,ContactTag}.java`, `B/repository/TagRepository.java`, `B/service/{TagService,ContactService}.java` | T-13 |
| T-17 | US-009 | R-009-02; AC-009-02; EX-009-04, EX-009-05 | `B/web/TagController.java`, `F/features/contacts/TagsField.tsx`, `F/api/tags.ts` | T-16, T-14 |
| T-18 | US-010 | R-010-01 … R-010-03; AC-010-01 … AC-010-03; EX-010-01 … EX-010-05 | `B/service/HistoryService.java`, `B/web/HistoryController.java`, `F/features/history/{HistoryPage,VersionPage}.tsx` | T-13 |
| T-19 | US-011 | R-011-01 … R-011-04; AC-011-01 … AC-011-04; EX-011-01 … EX-011-04 | `B/service/HistoryService.java`, `B/web/HistoryController.java`, `F/features/history/VersionPage.tsx` | T-18, T-16 |
| T-20 | US-012 | R-012-01 … R-012-04; AC-012-01, AC-012-02, AC-012-04, AC-012-05; EX-012-01 … EX-012-06 | `B/service/RecycleBinService.java`, `B/web/RecycleBinController.java` | T-13 |
| T-21 | US-012 | R-012-05, BR-08 (Contacts); AC-012-03; NFR-05 | `B/service/RemovalJob.java`, `B/config/ApplicationConfig.java` (`@EnableScheduling`) | T-20 |
| T-22 | US-012 | AC-012-01, AC-012-02 (UI); EX-012-03 | `F/features/recycle-bin/RecycleBinPage.tsx`, `F/features/contacts/ContactDetailsPage.tsx` | T-20, T-12 |
| T-23 | US-013 | R-013-01 … R-013-03; AC-013-01 … AC-013-03; EX-013-01 … EX-013-04; SR-06; BR-10 | `B/service/{PermanentDeleteService,AuditService}.java`, `B/domain/AuditEntry.java`, `B/repository/AuditEntryRepository.java`, `B/web/RecycleBinController.java`, `F/features/recycle-bin/RecycleBinPage.tsx` | T-21, T-22 |
| T-24 | US-015 | R-015-01 … R-015-05, R-015-07; AC-015-01 … AC-015-03, AC-015-05; EX-015-01 … EX-015-05 | `B/service/{ContactQuery,ContactQueryService}.java`, `B/repository/ContactQueryRepository.java` | T-11, T-15, T-16 |
| T-25 | US-015 | R-015-06; AC-015-04 | `F/features/contacts/{FilterPanel,FilterChips}.tsx`, `F/features/contacts/useListQuery.ts` | T-24, T-12 |
| T-26 | US-016 | R-016-01 … R-016-06; AC-016-01 … AC-016-04; EX-016-01, EX-016-03 … EX-016-07; SR-07; BR-10 | `B/service/ExportService.java`, `B/web/ExportController.java` | T-24, T-23 |
| T-27 | US-016 | R-016-02, R-016-07; AC-016-02; EX-016-02; D-24 (b) | `F/features/export/ExportButtons.tsx`, `F/features/contacts/useSelection.ts` | T-26, T-25 |
| T-28 | US-003 | R-003-01 … R-003-06; AC-003-01 … AC-003-04; EX-003-01 … EX-003-04, EX-003-06; BR-10 | `B/service/GroupService.java`, `B/web/{GroupController,DeletedGroupController}.java` | T-23 |
| T-29 | US-003 | BR-08 (Groups, D-19); AC-003-05 | `B/service/RemovalJob.java` | T-28, T-21 |
| T-30 | US-003 | AC-003-01, AC-003-03, AC-003-04 (UI); EX-003-04 | `F/features/groups/{GroupSettingsPage,DeletedGroupsPage}.tsx` | T-28, T-08 |
| T-31 | US-004 | R-004-01 … R-004-03, R-004-08, R-004-09; AC-004-01, AC-004-05, AC-004-06; EX-004-04 … EX-004-07; SR-05; D-17 | `B/domain/Invitation.java`, `B/repository/InvitationRepository.java`, `B/service/{InvitationService,InvitationToken}.java`, `B/web/InvitationController.java` | T-28 |
| T-32 | US-004 | R-004-04 … R-004-07; AC-004-02 … AC-004-04; EX-004-01 … EX-004-03, EX-004-08; EX-003-05 | `B/service/InvitationService.java`, `B/web/InvitationController.java`, `F/features/invitations/InvitationPage.tsx`, `F/auth/RequireAuth.tsx` | T-31, T-05 |
| T-33 | US-004 | AC-004-01 (UI: link shown once, Copy link) | `F/features/members/InvitationsSection.tsx` | T-31, T-35 |
| T-34 | US-005 | R-005-01 … R-005-05; AC-005-01 … AC-005-05; EX-005-01 … EX-005-05; BR-10 | `B/service/MembershipService.java`, `B/web/MemberController.java` | T-31 |
| T-35 | US-005 | AC-005-01 (UI), EX-005-04 | `F/features/members/MembersPage.tsx` | T-34, T-08 |
| T-36 | all INC-1 | SR-02: every endpoint as non-Member (404) and as a Role without the permission (403); SR-04 architecture rule | `02_output/backend/src/test/java/app/contacts/AccessMatrixIT.java`, `ArchitectureTest.java` | T-35 |
| T-37 | all INC-1 | AR-02, DoD-P04: springdoc document equals `docs/02_contracts/openapi.yaml` (paths, operations, schemas) | `02_output/backend/src/test/java/app/contacts/OpenApiContractIT.java` | T-35 |
| T-38 | all INC-1 | SR-08, SB-07: log capture during the integration tests has no e-mail, Contact Field value or token | `02_output/backend/src/test/java/app/contacts/LogHygieneIT.java` | T-35 |
| T-39 | NFR | NFR-01, NFR-03: k6 load test and capacity seed | `02_output/perf/{contact-list.js,seed-10k.sql,seed-capacity.sql,README.md}` | T-27 |
| T-40 | NFR | NFR-02: timed Export of 10,000 Contacts | `02_output/perf/export.js` | T-39 |
| T-41 | NFR | NFR-04: restart and recovery demonstration script | `02_output/scripts/check-recovery.sh` | T-35 |
| T-42 | NFR | NFR-06, NFR-07: axe check of every screen at the three widths in the three browsers; keyboard walk-through of E2E-01 recorded | `02_output/frontend/e2e/accessibility.spec.ts` | T-35 |
| T-43 | NFR | NFR-08: screenshots of every screen in both languages | `02_output/frontend/e2e/languages.spec.ts` | T-42 |
| T-44 | US-017 | R-017-01 … R-017-05; AC-017-01 … AC-017-05; EX-017-01 … EX-017-07; D-25 (a) | `B/service/{DuplicateService,NameSimilarity}.java`, `B/web/DuplicateController.java` | INC-1 |
| T-45 | US-017 | AC-017-01 (UI), EX-017-07 | `F/features/duplicates/DuplicatesPage.tsx` | T-44 |
| T-46 | US-018 | R-018-01 … R-018-05; AC-018-01, AC-018-03, AC-018-05; EX-018-01, EX-018-07; BR-10 | `B/domain/Merge.java`, `B/repository/MergeRepository.java`, `B/service/MergeService.java`, `B/web/MergeController.java` | T-44 |
| T-47 | US-018 | R-018-06, R-018-07; AC-018-04; EX-018-02 … EX-018-06; D-21 (permanent delete and BR-08 erase Contacts merged into a Primary contact) | `B/service/{MergeService,PermanentDeleteService,RemovalJob}.java` | T-46 |
| T-48 | US-018 | AC-018-02 (Merged from), AC-018-05 (UI) | `F/features/duplicates/MergePage.tsx`, `F/features/contacts/ContactDetailsPage.tsx` | T-47, T-45 |
| T-49 | US-019 | R-019-01 … R-019-03; AC-019-01 … AC-019-03; EX-019-01, EX-019-02; D-25 (c) | `B/service/MergeService.java`, `B/web/MergeController.java` | T-47 |
| T-50 | US-019 | EX-019-03 (UI confirmation) | `F/features/contacts/ContactDetailsPage.tsx` | T-49, T-48 |
| T-51 | US-021 | R-021-01 … R-021-03; AC-021-01, AC-021-02; EX-021-02, EX-021-03; D-20 | `B/service/UserService.java`, `B/auth/FirebaseAccountDeleter.java`, `B/web/MeController.java` | INC-1 |
| T-52 | US-021 | EX-021-01 (typed e-mail), AC-021-01 (UI) | `F/features/account/AccountPage.tsx` | T-51 |
| T-53 | all INC-2 | SR-02 matrix and contract test extended to the INC-2 endpoints | `AccessMatrixIT.java`, `OpenApiContractIT.java` | T-52 |

US-018 is split into three tasks (T-46 … T-48), as D-25 (d) asked.
