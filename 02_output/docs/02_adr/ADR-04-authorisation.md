# ADR-04: Authorisation by Membership and Role

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: BR-01 … BR-04, SR-02, AR-05, R-003-03, D-15 (via D-27)

## Context
Every request to Group data must be checked on the server: a non-Member, or anyone for a Group in the recycle bin (R-003-03), gets 404; a Member without the Role gets 403.

## Options
1. One `AccessGuard` service called first by every service method: `require(groupId, userId, Permission)`.
2. Spring Security method annotations with SpEL.

## Decision
Option 1. `AccessGuard.require(groupId, userId, permission)` loads the Membership with the Group's state in one query; no Membership or Group not `active` → `NotFoundException` (404); Role lacks the permission → `ForbiddenException` (403). The permission table:

| Permission | Owner | Editor | Viewer |
|---|---|---|---|
| READ (list, search, filters, details, History, Tags, duplicates) | ✓ | ✓ | ✓ |
| EXPORT | ✓ | ✓ | ✓ |
| CHANGE_CONTACT (add, edit, Attributes, Tags, restore Version, recycle bin, merge, undo merge) | ✓ | ✓ | |
| VIEW_RECYCLE_BIN | ✓ | ✓ | |
| MANAGE_GROUP (rename, move Group to recycle bin, Members, Invitations) | ✓ | | |
| PERMANENTLY_DELETE | ✓ | | |

The Owner's "Deleted Groups" and restore use a separate check (Owner of a Group in `inRecycleBin`).

## Consequences
- One place to read and test the rights; every service method has a test as non-Member (404) and as a Role without the permission (403) (SR-02).
- The client never sends Role or ownership (AR-05).
