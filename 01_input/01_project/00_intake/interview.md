# Interview

> For: agent (reads), human (reviews) · Owner: Product owner · Written by: `/grill-me` · Agent: writes in intake only

Notes of the requirements interview. IDs never change once given. Source: `customer: <file>` (from `customer_requirement/`), `customer` (an answer the customer gave the developer) or `developer`. Status: `answered`, `deferred` (with reason) or `open`.

## 1. Problem

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-01 | What hurts today, and for whom? | Teams in a project or company need to keep shared contacts; existing CRM systems are too complex for this (the product competes on simplicity). Who exactly suffers and how much: not stated; the developer has only the brief. | customer: brief.txt | open — needs customer |
| Q-02 | How do they cope today and what does it cost? | Not known; the developer has only the brief. Not needed to build; needed only for a measurable baseline. | developer | deferred (no customer input; success is measured by Q-05 instead) |

## 2. Product

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-03 | What do we build? | A multi-tenant contact management system: a web UI (React) over a secured REST backend (Spring Boot, PostgreSQL). Users create their own contact groups (a project or a company, defined by name only); contacts are visible only inside their group, with data isolation between users and their projects. | customer: brief.txt | answered |
| Q-04 | Core outcome / competitive advantage? | Ease of use compared to competing CRM systems: friendly overview, search, filtering, Excel export. | customer: brief.txt | answered |
| Q-05 | Measurable success criteria? | (1) A new user creates a group, adds a contact, tags it and exports it to Excel in under 2 minutes without help. (2) Duplicate detection finds exact e-mail matches and similar names. (3) Every change of a contact can be traced in its history and an earlier version can be seen. | developer | answered |
| Q-06 | The brief says "design, upgrade, implement and deploy" and links github.com/anzo26/SCM as an example of an existing solution. Do we build from scratch or extend that code? | Not stated; "upgrade" (nadgraditi) suggests extending, "example" suggests reference only. Decision: build from scratch; SCM is a feature reference only (its code, stack and bugs are not taken over). | developer | answered |

## 3. Users and roles

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-07 | Who are the users and which roles exist? | Proposed default: a Group can have several members. The creator becomes **Owner**; the Owner invites others by e-mail as **Editor** (add and change contacts) or **Viewer** (read and export). | developer | open — needs customer |
| Q-08 | Contact visibility | Contacts are visible only within their group; data must be isolated between users and their projects. | customer: brief.txt | answered |
| Q-17 | Can one user belong to several groups? Is there a system administrator? | Proposed default: a user can belong to several groups and switches between them. No administrator can see contacts across groups; an operator only manages accounts. | developer | open — needs customer |
| Q-18 | What may only the Owner do; who may merge and restore? | Proposed default: only the Owner renames or deletes the group, manages members and permanently deletes contacts. Owner and Editor may merge duplicates and restore an earlier version. | developer | open — needs customer |

## 4. Features (user goals, priority, MVP)

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-09 | Features named by the customer (priorities and MVP not yet set) | F1 create a contact group (project/company, name only); F2 manage contacts with predefined fields; F3 arbitrary key-value attributes (e.g. competence=webProgramming); F4 arbitrary tags used as categories (e.g. doNotNotify, participant2025); F5 audit trail of all contact changes, with versioning; F6 find duplicate contacts; F7 merge contacts; F8 overview with search and filtering; F9 export of "označeni" contacts to MS Excel; F10 secured REST API to the backend. | customer: brief.txt | answered (priorities in Q-19) |
| Q-19 | Priority and increment of each feature | Must, MVP (increment 1): F1, F2, F3, F4, F5, F8, F9, F10. Should, increment 2: F6 find duplicates, F7 merge (they build on F5 history). | developer | answered |

## 5. Main flows, rules, exceptions, examples

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-20 | Required contact fields | At least one of first name, last name or company (a company-only contact is valid); all other fields optional. | developer | answered |
| Q-21 | E-mail uniqueness within a group | Not enforced; a repeated e-mail is only flagged as a possible duplicate. | developer | answered |
| Q-22 | Deleting a contact | Proposed default: soft delete to a recycle bin, restorable for 30 days; after that, or earlier, only the Owner can delete it permanently. | developer | open — needs customer |
| Q-23 | Versioning | Every save creates a version: who, when (UTC), old and new value of every changed field, attribute and tag. Any version can be viewed and restored; a restore is a new version. Versions are kept as long as the contact exists. | developer | answered |
| Q-24 | Concurrent edits of one contact | Optimistic locking: the second save is rejected with "changed by <user>, reload". | developer | answered |
| Q-25 | Duplicate rule | Possible duplicates: same e-mail (case-insensitive), or same normalised phone, or similar full name (fuzzy match above a threshold) at the same company. Detection only suggests; it never merges on its own. | developer | answered |
| Q-26 | Merge | The user chooses the primary contact and, field by field, which value to keep; tags and attributes are combined; history of both is kept; the other contact is marked "merged into <primary>", not deleted; a merge can be undone. | developer | answered |
| Q-27 | Tags and attributes | Tags: free text per group, case-insensitive, 1–50 characters, suggested from the group's existing tags. Attributes: key free text up to 50 characters, value text up to 500 characters, each key at most once per contact, at most 50 attributes per contact. | developer | answered |
| Q-28 | Excel export format | .xlsx, one row per contact: a column per fixed field, a column per attribute key present in the exported contacts, all tags in one comma-separated cell. Maximum 10,000 rows per export. | developer | answered |

## 6. Shared rules and data

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-14 | Term: "podatki o uporabnikih" (user data) — users or contacts? | It means contacts. **User** = a person who signs in to the app; **Contact** = a record kept in a group. | developer | answered |
| Q-15 | Term: contact group / project / company — one thing or several? | One term: **Group** = the tenant. It has only a name; "project" and "company" are examples of what a group stands for. No hierarchy. | developer | answered |
| Q-16 | Term: export of "označeni" contacts — tagged or selected? | Export exports the current list as filtered (tag filters included); the user can optionally export only the rows they checked. | developer | answered |
| Q-10 | Predefined contact fields | Brief: first name, last name, address, country, e-mail, company "etc.". Completed list: first name, last name, e-mail, phone, company, job title, street, postal code, city, country (ISO 3166-1 alpha-2 code), note. | customer: brief.txt, developer | answered |

## 7. Project-wide facts

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-29 | Languages, time zone, formats, currency | UI in Slovenian and English, Slovenian by default. Time zone Europe/Ljubljana for display, UTC stored. Date format dd.mm.yyyy. Encoding UTF-8. No currency, no tax. | developer | answered |

## 8. Out of scope, non-goals, principles

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-30 | Out of scope | Sending e-mail or newsletters; contact import (CSV/Excel) — a later "could"; sync with Google/Outlook; mobile app; billing or paid plans; migration of SCM data. | developer | answered |
| Q-31 | Principles never to break | (a) No contact is ever visible outside its group. (b) No change to a contact without a version. (c) The system never merges or deletes anything on its own. | developer | answered |

## 9. Personal data

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-32 | Personal data, roles under GDPR, consent, erasure | Proposed default: contacts are third-party personal data; each group is controller of its contacts, we are processor. No consent tracking in MVP; opt-out via tag doNotNotify. Permanent delete also removes all versions of the contact (right to erasure); the audit log keeps only "contact <id> permanently deleted by <user> at <time>". | developer | open — needs customer |
| Q-33 | Retention | Proposed default: data kept while the group exists; deleting a group goes to a 30-day recycle bin, then all its data is removed. User account data kept until the user deletes the account. | developer | open — needs customer |

## 10. Security

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-11 | Authentication and authorisation | Authentication via Firebase; authorisation is our own (in the backend); backend reachable only through a secured REST API. | customer: brief.txt | answered |
| Q-34 | Authentication methods, token check, rate limit, audited operations | Proposed default: Firebase sign-in with e-mail/password and Google. Backend verifies the Firebase ID token on every call; group roles checked on the server for every request. HTTPS only; 100 requests per minute per user. Export, permanent delete, role changes and merges are written to the audit log. | developer | open — needs customer |
| Q-35 | Inviting members | Proposed default: only the Owner invites; single-use invitation link valid 7 days. MVP: the Owner copies the link and sends it themselves (no e-mail service). | developer | open — needs customer |

## 11. Integrations and external services

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-12 | Which integrations? | Firebase Authentication; Excel (export to MS Excel format). | customer: brief.txt | answered |
| Q-36 | Firebase project and Excel library | Firebase Authentication with a separate test project for development and CI; Excel generated on the server (Apache POI), no external service. Whether a Firebase project already exists is not known: the developer must create or provide it before /preflight. | developer | open — developer to provide Firebase project |

## 12. Quality

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-37 | Load, response times, availability, devices, accessibility | Up to 50 groups, 200 users, 10,000 contacts per group. List and search p95 < 500 ms; export of 10,000 rows < 10 s. Availability 99% during working hours. Desktop browsers: latest Chrome, Firefox, Edge, Safari; responsive down to tablet width. WCAG 2.1 AA. | developer | answered |

## 13. Technology

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-13 | Required technology | Backend Spring Boot; frontend React (brief says "Resct", read as React); database PostgreSQL; authentication Firebase; authorisation own. Versions: see Q-38. | customer: brief.txt | answered |
| Q-38 | Versions, repository, licence | Java 21, Spring Boot 3.x, React 18 + TypeScript + Vite, PostgreSQL 16, Flyway migrations. One repository with backend/ and frontend/. Product proprietary; only open-source dependencies under MIT, Apache or BSD licences. | developer | answered |

## 14. Environments and deployment

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-39 | Environments, hosting, operation, backups | Local: Docker Compose (app + PostgreSQL). Staging for customer acceptance: Docker containers on a Linux VPS or a cloud the customer chooses. Production location and operator: not known. Backups: daily pg_dump, kept 30 days, one restore tested before handover. | developer | open — needs customer (production hosting and operator) |

## 15. Design constraints

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-40 | Brand, tone, reference products | No colours, logo or reference products in the brief; only "simple, friendlier than a CRM". Clean neutral light theme on the UI baseline; customer logo later if supplied; calm, plain tone. No reference products known. | developer | answered |

## 16. End-to-end journeys

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-41 | Journeys that prove the product works | E2E-1: sign up → create group "Konferenca 2025" → add contact with an attribute and tag udelezenec2025 → filter by the tag → export .xlsx → the file contains that row. E2E-2: Owner invites an Editor → Editor edits the contact → history shows 2 versions → Owner restores version 1. E2E-3: a user of another group cannot see or reach the contact via UI or API (403/404). E2E-4 (increment 2): two contacts with the same e-mail suggested as duplicates → merge → undo merge. E2E-5: soft delete → restore from the recycle bin. | developer | answered |

## 17. Project done

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-42 | What the customer must see to accept (DoD-P) | All must stories done on staging; E2E-1, 2, 3, 5 pass (and E2E-4 for increment 2); no open Critical or High finding; Q-05 success criteria met; customer receives the repository, a deployment guide and the API documentation (OpenAPI). | developer | answered |

## Contradictions resolved

| IDs | Contradiction | Resolution |
|---|---|---|
| Q-14 | The brief calls contact data "podatki o uporabnikih" (user data), using one word for app users and contacts. | User = signs in; Contact = record in a group. |
| Q-15 | Group, project and company are used for the same thing. | One term: Group (tenant), name only, no hierarchy. |
| Q-16 | "Označeni" can mean tagged or selected. | Export = current filtered list, optionally only checked rows. |
| Q-06 | "Nadgraditi" (upgrade) vs SCM given as "an example". | Build from scratch; SCM is a feature reference only. |

## Open items

| ID | Why open | Who must answer |
|---|---|---|
| Q-01 | Who suffers today and what it costs is not in the brief. | Customer |
| Q-07, Q-17, Q-18 | Roles and rights (Owner/Editor/Viewer, multi-group, no admin, merge/restore rights) are a Block topic; proposed defaults recorded. | Customer |
| Q-22 | Deletion (soft delete, 30-day bin, Owner-only permanent delete) is a Block topic; proposed default recorded. | Customer |
| Q-32, Q-33 | Personal data: GDPR roles, erasure, retention are a Block topic; proposed defaults recorded. | Customer |
| Q-34, Q-35 | Security: sign-in methods, rate limit, audit, invitations are a Block topic; proposed defaults recorded. | Customer |
| Q-36 | Firebase project for development/test not known to exist. | Developer, before /preflight |
| Q-39 | Production hosting and who operates it are not stated. | Customer |
