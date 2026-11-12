# Intent

> For: agent (reads), human (reviews) · Owner: Product owner · Agent: read-only

Why the product exists and what "done" means for the customer. Sources: interview `Q` ids.

## Problem statement

People who work together on a project or in a company need one shared, trustworthy list of their contacts, kept apart from other projects. Existing CRM systems do this but are too complex for the job (Q-01, Q-04). Who exactly suffers today, how they cope and what it costs are not known (Q-01 OPEN — needs customer; Q-02 deferred).

## Product statement

A multi-tenant contact management web application ("Večnajemniški sistem za vodenje kontaktnih podatkov"). Users create Groups, keep Contacts with fixed Fields, free Attributes and Tags, see every change as a Version, find and merge Possible duplicates, and search, filter and export Contacts to Excel. The backend is reachable only through a secured REST API, and no Contact is ever visible outside its Group (Q-03, Q-08, Q-09).

## Users

| Role | Primary or secondary | Needs | Must not be able to |
|---|---|---|---|
| Owner | primary | Create and manage a Group, its Members and its Contacts; permanently delete Contacts. | See or change data of a Group they are not a Member of. |
| Editor | primary | Add, change, tag, restore, move to recycle bin and merge Contacts; export. | Rename, delete or restore the Group; manage Members or Invitations; permanently delete Contacts. |
| Viewer | secondary | Read, search, filter and export Contacts; see History. | Change anything. |
| Operator | secondary | Manages User accounts (in Firebase) and runs the system. | See any Contact (no administrator role inside the product). |

Roles and rights are developer proposals: Q-07, Q-17, Q-18 OPEN — needs customer.

## Core outcome

Contact management that is easier to use than a CRM (Q-04), measured by:

- a new User creates a Group, adds a Contact, tags it and exports it to Excel in under 2 minutes without help (Q-05);
- duplicate detection finds Contacts with the same e-mail and with a similar full name at the same company (Q-05, Q-25);
- every change of a Contact can be traced in its History and every earlier Version can be seen (Q-05, Q-23).

## MVP

Increment 1 (MVP): US-001 … US-016 and US-020 (Q-19).

Increment 2: US-017, US-018, US-019 (find Possible duplicates, Merge, Undo merge; Q-19) and US-021 (delete my account; [ASSUMPTION] placed in increment 2).

## Scope

- Sign-in, language and account: US-001, US-020, US-021.
- Groups and Members: US-002 (create, switch), US-003 (rename, move to recycle bin, restore), US-004 (Invitations), US-005 (Roles, removal).
- Contacts: US-006 (add), US-007 (edit Fields), US-008 (Attributes), US-009 (Tags).
- History: US-010 (view), US-011 (restore a Version).
- Deletion: US-012 (recycle bin), US-013 (permanent delete).
- Contact list: US-014 (list, search, sort), US-015 (filters), US-016 (Export to `.xlsx`).
- Duplicates: US-017 (find), US-018 (Merge), US-019 (Undo merge).
- Every capability is available through the secured REST API that the web application uses (Q-09 F10, Q-11).

## Non-goals (out of scope)

From Q-30:

- sending e-mails or newsletters (including e-mailing Invitations: the Owner copies the link, Q-35);
- importing Contacts (CSV or Excel), a later "could";
- syncing with Google, Outlook or other address books;
- a mobile app (the web application is responsive);
- billing, plans or payments;
- migrating data from the reference solution `github.com/anzo26/SCM`; its code is not reused (Q-06).

## Product principles

From Q-31, each checked by tests:

- **P-1** No Contact, Tag, Attribute, Version or Audit entry is ever shown to a User who is not a Member of its Group, through the UI or the API (BR-01, BR-02).
- **P-2** No change to a Contact is saved without a Version (BR-05).
- **P-3** The system never merges, moves to the recycle bin or permanently deletes anything unless a User asked for it, except removal after the recycle bin period (BR-07, BR-08).

## Open questions

Product-level questions; story-level questions are in the story.

| ID | Question | Owner | Due | Answer |
|---|---|---|---|---|
| OQ-P-01 | Who suffers today, how do they cope, what does it cost? (Q-01) | Customer | before release; not blocking | OPEN — needs customer |
| OQ-P-02 | Confirm the Roles Owner, Editor and Viewer, several Groups per User, no administrator who sees Contacts, and which Role may merge and restore (Q-07, Q-17, Q-18). | Customer | before `/clarify` | OPEN — needs customer |
| OQ-P-03 | Confirm deletion: recycle bin for 30 days, Owner-only permanent delete (Q-22, Q-33). | Customer | before `/clarify` | OPEN — needs customer |
| OQ-P-04 | Confirm GDPR roles (each Group is controller, we are processor), erasure of Versions on permanent delete, retention while the Group exists (Q-32, Q-33). | Customer | before `/clarify` | OPEN — needs customer |
| OQ-P-05 | Confirm sign-in with e-mail/password and Google, rate limit 100 requests per minute, the audited actions, and Invitation links copied by the Owner (Q-34, Q-35). | Customer | before `/clarify` | OPEN — needs customer |
| OQ-P-06 | Where does production run and who operates it? (Q-39) | Customer | before `/release` | OPEN — needs customer |

## Project Definition of Done

Project additions to the release Definition of Done (`general/quality/done-release.md`), checked in phase 6.

| ID | Criterion | How it is checked |
|---|---|---|
| DoD-P01 | Every end-to-end scenario of `e2e-scenarios.md` passes; E2E-04 only once increment 2 is released (Q-41, Q-42). | The end-to-end suite runs against the local stack; the report lists every E2E id as passed. |
| DoD-P02 | A User new to the product creates a Group, adds a Contact, tags it and exports it to Excel in under 2 minutes without help (Q-05). | A human runs the task with one person who has not used the product, timed, on the staging environment; the result is recorded in the release notes. |
| DoD-P03 | All `must` stories are done and deployed on the staging environment (Q-42). | The story table in `docs/00_progress.md` shows every `must` story done; a human opens the staging URL and repeats E2E-01. Staging host: Q-39 OPEN. |
| DoD-P04 | The customer receives the repository, a deployment guide and the API documentation as OpenAPI (Q-42). | The release contains `README` with deployment steps, and an OpenAPI file that a contract test checks against the running backend. |
| DoD-P05 | A database backup is restored once before handover (Q-39). | A human follows the deployment guide's restore steps on staging and records the date and result in the release notes. |
