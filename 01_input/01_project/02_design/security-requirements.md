# Security requirements

> For: agent (reads), human (reviews) · Owner: Security officer · Agent: read-only

Adds to `general/security/security-baseline.md` (SB-01 … SB-14); do not repeat those here.

## Verification standard

| Component | Standard | Level | Reason if above the default |
|---|---|---|---|
| backend | OWASP ASVS 5.0 | Level 2 | Multi-tenant storage of third parties' personal data (Q-08, Q-32). |
| frontend | OWASP ASVS 5.0 | Level 2 | Same data shown and exported; same reason. |

Level 2 because a leak between Groups or of Contacts' personal data is the product's main risk (principle P-1). [ASSUMPTION] level not given by the customer.

## Authentication and authorization

- Authentication: Firebase Authentication with e-mail/password and Google (US-001; Q-34 OPEN — needs customer). The backend verifies the Firebase ID token on every request.
- Authorisation: own, in the backend (Q-11), by Membership and Role per Group: Owner, Editor, Viewer (`intent.md` Users; BR-01 … BR-04; Q-07, Q-18 OPEN — needs customer).
- No administrator role inside the product; the Operator manages accounts in the Firebase console and cannot see Contacts through the product (Q-17).

## Personal data

| Data item | Purpose | Retention | Shown/exported to |
|---|---|---|---|
| Contact Fields (names, e-mail, phone, job title, address, country, note) | The Group's contact management (the Group is controller, we are processor; Q-32) | While the Contact is active; 30 days in the recycle bin; then erased with all Versions (BR-08, US-013) | Members of the Group; exported by any Member (US-016) |
| Attribute values | Same | Same | Same |
| Versions (copies of the above) | Traceability (Q-23) | As long as the Contact exists; erased with it | Members of the Group |
| User e-mail and display name | Sign-in; showing who made a Version; Invitations | Until the User deletes the account (US-021); display name replaced by "Deleted user" | Display name: Members of the User's Groups; e-mail: the Owner (Members screen) |
| Invitation e-mail | Binding an Invitation to its invitee | Until the Group is erased [ASSUMPTION] | The Owner |
| Audit entries (User id, action, target id, time) | Accountability for sensitive actions (Q-34) | Until the Group is erased [ASSUMPTION] | Nobody in the UI in this release [ASSUMPTION]; the Operator in the database |

All rows: Q-32, Q-33 OPEN — needs customer.

## Project requirements

| ID | Requirement |
|---|---|
| SR-01 | Every API request except the health check needs a valid Firebase ID token (signature, project id, expiry); otherwise HTTP 401 (US-001). |
| SR-02 | Every request to Group data checks Membership and Role on the server; a non-Member gets HTTP 404, a Member without the Role HTTP 403 (BR-02 … BR-04). A test calls every endpoint as a User of another Group and as a Viewer. |
| SR-03 | Rate limit: at most 100 API requests per minute per User; above it HTTP 429 with `Retry-After` (Q-34 OPEN — needs customer). |
| SR-04 | The actions of BR-10 write an Audit entry that cannot be changed or deleted through the product. |
| SR-05 | Invitation tokens have at least 128 bits of randomness, are stored only as a hash, are single-use and expire after 7 days (US-004; Q-35 OPEN — needs customer). |
| SR-06 | A permanent delete (US-013, BR-08) removes the Contact's Fields, Attributes, Tag assignments and all Versions from the database; only the Audit entry without personal data remains (Q-32). |
| SR-07 | In an Export, every cell value starting with `=`, `+`, `-`, `@`, tab or carriage return is written as text so that Excel does not evaluate it (formula injection). |
| SR-08 | Contact data, e-mails and ID tokens are never written to application logs; logs carry User ids and Contact ids only. |
| SR-09 | Staging and production serve only HTTPS; the backend accepts requests only from the frontend's origin (CORS). |

## Overrides

None.
