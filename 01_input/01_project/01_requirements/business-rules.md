# Business rules

> For: agent (reads), human (reviews) · Owner: Product owner · Agent: read-only

Project-wide facts, rules shared by more than one story, the data model and the glossary. A rule that belongs to one story lives in that story. Format: `general/requirements-standard.md`.

## Project-wide facts

Facts that hold for the whole project, written once. Stories, rules and tests use these values and never repeat or override them.

| Fact | Value | Source |
|---|---|---|
| Time zone | Europe/Ljubljana for everything shown to a User; every timestamp is stored in UTC. | Q-29 |
| Currency | None (the product handles no money). | Q-29 |
| Tax (included in prices or not, rate) | None. | Q-29 |
| Date and time format | Date `dd.mm.yyyy` (e.g. `05.03.2026`); time `HH:mm`, 24-hour (e.g. `14:07`), in both languages. [ASSUMPTION] time format and "same format in English" not given; Slovenian convention used. | Q-29 |
| Number format | Slovenian: decimal comma, thousands separator dot (`10.000`); English: decimal point, thousands separator comma (`10,000`). [ASSUMPTION] not given; locale convention of each language. | — |
| Languages of the product | Slovenian (default) and English; every User chooses one, the choice is kept per User. [ASSUMPTION] "kept per User" not given. | Q-29 |
| Text encoding | UTF-8 | Q-29 |
| Units of measure | None. | — |
| Other: country codes | ISO 3166-1 alpha-2, shown as the country name in the User's language. | Q-10 |
| Other: recycle bin period | 30 days, counted from the UTC time the Contact or Group was moved to the recycle bin. OPEN — needs customer (Q-22, Q-33). | Q-22, Q-33 |
| Other: working hours | Monday to Friday, 08:00–16:00 Europe/Ljubljana, excluding Slovenian public holidays. [ASSUMPTION] Q-37 says "working hours" without values. | Q-37 |

## Shared rules

| ID | Rule (EARS) | Stories | Source |
|---|---|---|---|
| BR-01 | The system shall show or change Contacts, Tags, Attributes, Versions, Invitations and Audit entries of a Group only for a User who is a Member of that Group. | all | Q-08, Q-31 |
| BR-02 | If a User who is not a Member of a Group requests any data of that Group, through the UI or the API, then the system shall respond "not found" (HTTP 404) and return no data of that Group. [ASSUMPTION] 404 chosen over 403 so that the existence of a Group is not revealed. | all | Q-08, Q-41 |
| BR-03 | If a Viewer requests any change to a Group's data, then the system shall reject it as "not allowed" (HTTP 403) and change nothing. | US-006 … US-013, US-018, US-019 | Q-07 (OPEN — needs customer) |
| BR-04 | If a Member who is not the Owner requests to rename, delete or restore the Group, create or revoke an Invitation, change a Role, remove a Member, or permanently delete a Contact, then the system shall reject it as "not allowed" (HTTP 403) and change nothing. | US-003, US-004, US-005, US-013 | Q-18 (OPEN — needs customer) |
| BR-05 | When a change to a Contact is saved (create, edit of Fields, Attributes or Tags, restore of a Version, move to or restore from the recycle bin, merge, undo merge), the system shall create exactly one Version of that Contact. | US-006 … US-012, US-018, US-019 | Q-23, Q-31 |
| BR-06 | If a change to a Contact is sent with a Version number that is not the Contact's current Version number, then the system shall reject the change with the message "Contact was changed by <display name of the User of the current Version>, reload" (HTTP 409) and change nothing. | US-007 … US-012, US-018 | Q-24 |
| BR-07 | The system shall not merge, move to the recycle bin or permanently delete a Contact or a Group unless a User requested it, except the permanent removal defined in BR-08. | US-003, US-012, US-013, US-017, US-018 | Q-31 |
| BR-08 | When the recycle bin period of a Contact or a Group has passed, the system shall permanently delete it with all its Versions, Attributes and Tag assignments (and, for a Group, all its Contacts, Memberships, Invitations and Tags). | US-003, US-012 | Q-22, Q-33 (OPEN — needs customer) |
| BR-09 | While a Contact is in the recycle bin or merged into another Contact, the system shall exclude it from the Contact list, search, filters, Export and duplicate detection. [ASSUMPTION] not stated; follows from "deleted" and "merged into" meaning the Contact is no longer in use. | US-012, US-014, US-015, US-016, US-017, US-018 | Q-22, Q-26 |
| BR-10 | When an Export, a permanent delete, a Role change, a Member removal, an Invitation creation or revocation, a merge, an undo merge, or a Group move to or restore from the recycle bin happens, the system shall write one Audit entry with the User, the action, the target and the UTC time. | US-003, US-004, US-005, US-013, US-016, US-018, US-019 | Q-34 (OPEN — needs customer) |
| BR-11 | The system shall compare Tag names, Attribute keys and e-mail addresses case-insensitively. | US-008, US-009, US-015, US-017 | Q-25, Q-27 |

## Data model

| Entity | Field | Type and format | Required | Constraints | Personal data |
|---|---|---|---|---|---|
| User | id | UUID | yes | unique | no |
| User | firebaseUid | text | yes | unique; from the verified Firebase ID token | no |
| User | email | e-mail address | yes | from Firebase; unique | yes |
| User | displayName | text 1–100 | yes | from Firebase, or the part of the e-mail before `@` [ASSUMPTION] | yes |
| User | language | `sl` or `en` | yes | default `sl` | no |
| User | createdAt | timestamp UTC | yes | | no |
| Group | id | UUID | yes | unique | no |
| Group | name | text 1–100 characters, trimmed [ASSUMPTION] length not given | yes | not unique [ASSUMPTION] | no |
| Group | state | `active` or `inRecycleBin` | yes | | no |
| Group | deletedAt | timestamp UTC | when `inRecycleBin` | | no |
| Group | createdAt, createdBy | timestamp UTC, User id | yes | | no |
| Membership | groupId, userId | UUID, UUID | yes | unique pair | no |
| Membership | role | `OWNER`, `EDITOR`, `VIEWER` | yes | exactly one `OWNER` per Group [ASSUMPTION] | no |
| Invitation | id | UUID | yes | | no |
| Invitation | groupId | UUID | yes | | no |
| Invitation | email | e-mail address | yes | the invitee | yes |
| Invitation | role | `EDITOR` or `VIEWER` | yes | an Invitation never grants `OWNER` [ASSUMPTION] | no |
| Invitation | tokenHash | hash of a random token of at least 128 bits | yes | the token itself is never stored | no |
| Invitation | state | `open`, `accepted`, `expired`, `revoked` | yes | | no |
| Invitation | createdAt, createdBy, expiresAt | timestamp UTC, User id, timestamp UTC | yes | expiresAt = createdAt + 7 days | no |
| Invitation | acceptedAt, acceptedBy | timestamp UTC, User id | when `accepted` | | no |
| Contact | id | UUID | yes | unique | no |
| Contact | groupId | UUID | yes | never changes | no |
| Contact | firstName | text 0–100 [ASSUMPTION] length | see rule | at least one of firstName, lastName, company is non-blank (US-006) | yes |
| Contact | lastName | text 0–100 [ASSUMPTION] length | see rule | | yes |
| Contact | email | e-mail address, max 254 [ASSUMPTION] syntax check only | no | not unique (Q-21) | yes |
| Contact | phone | text 0–30: digits, spaces, `+`, `-`, `(`, `)`, `/` [ASSUMPTION] | no | | yes |
| Contact | company | text 0–200 [ASSUMPTION] length | see firstName | | no |
| Contact | jobTitle | text 0–100 [ASSUMPTION] length | no | | yes |
| Contact | street | text 0–200 [ASSUMPTION] length | no | | yes |
| Contact | postalCode | text 0–20 [ASSUMPTION] length | no | | yes |
| Contact | city | text 0–100 [ASSUMPTION] length | no | | yes |
| Contact | country | ISO 3166-1 alpha-2 | no | from the ISO list | yes |
| Contact | note | text 0–2000 [ASSUMPTION] length | no | | yes |
| Contact | state | `active`, `inRecycleBin`, `merged` | yes | | no |
| Contact | deletedAt | timestamp UTC | when `inRecycleBin` | | no |
| Contact | mergedInto | Contact id | when `merged` | same Group | no |
| Contact | versionNumber | integer ≥ 1 | yes | +1 per Version (BR-05) | no |
| Contact | createdAt, createdBy, updatedAt, updatedBy | timestamp UTC, User id | yes | | no |
| Attribute | contactId | UUID | yes | | no |
| Attribute | key | text 1–50, trimmed | yes | unique per Contact (BR-11); max 50 Attributes per Contact | no |
| Attribute | value | text 0–500 | yes | | yes (may hold any data) |
| Tag | id, groupId | UUID, UUID | yes | | no |
| Tag | name | text 1–50, trimmed | yes | unique per Group (BR-11) | no |
| ContactTag | contactId, tagId | UUID, UUID | yes | unique pair; Tag and Contact in the same Group | no |
| Version | contactId, number | UUID, integer | yes | unique pair; numbers start at 1 | no |
| Version | kind | `created`, `edited`, `restored`, `movedToRecycleBin`, `restoredFromRecycleBin`, `merged`, `mergeUndone` | yes | | no |
| Version | changedBy, changedAt | User id, timestamp UTC | yes | | no |
| Version | changes | list of (item, old value, new value); item = a Field, `attribute:<key>` or `tag:<name>` | yes | | yes (copies of contact data) |
| Version | snapshot | the full Contact (Fields, Attributes, Tags) after the change | yes | used to view and restore | yes |
| Merge | id, groupId | UUID, UUID | yes | | no |
| Merge | primaryId, mergedId | Contact ids | yes | different Contacts, same Group | no |
| Merge | mergedBy, mergedAt | User id, timestamp UTC | yes | | no |
| Merge | undoneAt, undoneBy | timestamp UTC, User id | when undone | | no |
| AuditEntry | id, groupId | UUID, UUID | yes | never changed or deleted while the Group exists [ASSUMPTION] | no |
| AuditEntry | userId, action, targetId, at | User id, text, UUID, timestamp UTC | yes | | no |

Lifecycles:

- **Contact:** `active` → `inRecycleBin` (move to recycle bin, US-012) → `active` (restore from recycle bin) or permanently deleted (US-013, or BR-08). `active` → `merged` (US-018) → `active` (Undo merge, US-019). A permanently deleted Contact leaves no data except its Audit entry.
- **Group:** `active` → `inRecycleBin` (US-003) → `active` (Owner restores) or permanently deleted (BR-08).
- **Invitation:** `open` → `accepted` | `expired` (7 days passed) | `revoked` (Owner). Only `open` can change.

## Glossary

The ubiquitous language of the project (`general/requirements-standard.md`): every requirement, the code, the API, the tests and the UI use these terms.

| Term | Meaning | Name in code | Do not use |
|---|---|---|---|
| User | A person who signs in to the product. | `User` | contact, "uporabnik" for a Contact, account |
| Group | The tenant: a named set of Contacts and Members, standing for a project or a company. No hierarchy. | `Group` (table `contact_group`) | project, company, tenant (in the UI), workspace, organisation, "skupina kontaktov" as a second term |
| Member | A User who belongs to a Group with one Role. | `Membership` | participant, collaborator |
| Role | What a Member may do in a Group: Owner, Editor or Viewer. | `Role` (`OWNER`, `EDITOR`, `VIEWER`) | permission level, access level |
| Owner | The Member who created the Group (or was made Owner); manages the Group and its Members. | `OWNER` | admin, creator |
| Editor | A Member who may add and change Contacts. | `EDITOR` | writer |
| Viewer | A Member who may read and export Contacts only. | `VIEWER` | reader, guest |
| Invitation | A single-use link, valid 7 days, that makes the invited User a Member with a given Role. | `Invitation` | invite code |
| Contact | A record about a person or a company kept in one Group. | `Contact` | user, person, entry, "uporabnik" |
| Field | One of the fixed data items of a Contact (Data model). | `field` | property, column |
| Attribute | A free key–value pair on a Contact (e.g. `kompetenca=spletnoProgramiranje`). | `Attribute` (`key`, `value`) | custom field, property, metadata |
| Tag | A free label of a Group used as a category of Contacts (e.g. `neObvescaj`, `udelezenec2025`). | `Tag` | label, category, "kategorija"; UI Slovenian text: "značka" |
| Version | The state of a Contact after one saved change, with who, when and what changed. | `ContactVersion` | revision, snapshot, history entry |
| History | The list of all Versions of a Contact. | `history` | audit trail (that is the Audit log), log |
| Restore (a Version) | Make an earlier Version's content current again, as a new Version. | `restoreVersion` | rollback, revert |
| Recycle bin | Where a Contact or Group stays for the recycle bin period after a User deletes it; restorable there. | `recycleBin` | trash, archive, soft delete (in the UI) |
| Move to recycle bin | The "delete" action of a User. | `moveToRecycleBin` | delete, remove (alone) |
| Permanent delete | Removing a Contact and all its Versions for good. | `permanentlyDelete` | purge, hard delete (in the UI) |
| Possible duplicate | A pair of Contacts that the duplicate rule says may be the same. | `DuplicateCandidate` | match, double |
| Merge | Combining a Possible duplicate pair into the Primary contact; the other Contact becomes `merged`. | `merge` | join, combine, deduplicate |
| Primary contact | The Contact that remains after a Merge. | `primary` | master, survivor |
| Undo merge | Reversing a Merge. | `undoMerge` | unmerge, split |
| Contact list | The list of a Group's active Contacts, with search and filters. | `ContactList` | overview, table, "pregled" as a second term |
| Search | Free-text matching over a Contact's Fields. | `search` | find, query |
| Filter | A condition on Tags, Attributes or Fields that narrows the Contact list. | `filter` | facet |
| Checked contacts | Rows the User ticked in the Contact list. | `selection` | marked, "označeni" (ambiguous) |
| Export | Downloading Contacts from the Contact list as an `.xlsx` file. | `export` | download, "izvoz označenih" |
| Audit entry | A record of a sensitive action (BR-10). | `AuditEntry` | log line, history |
