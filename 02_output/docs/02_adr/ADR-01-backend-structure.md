# ADR-01: Backend internal structure

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: AR-03, AR-04, AR-05, AR-06, ES-11, ES-13

## Context
One Spring Boot service (`app.contacts`) implements every story. AR-03 asks for `web → service → repository` layering checked by ArchUnit, AR-06 confines Firebase to `auth`. About ten entities; one team; one deployable.

## Options
1. Layer-first packages (`web`, `service`, `repository`, `domain`, `auth`, `config`), one class per glossary concept inside each.
2. Feature-first packages (`contact`, `group`, …) each with its own layers.
3. Hexagonal (ports and adapters).

## Decision
Option 1. Packages under `app.contacts`:

| Package | Holds | May use |
|---|---|---|
| `web` | REST controllers, request/response records (`*Request`, `*Response`), `ProblemHandler` | `service`, `auth` (current User id) |
| `service` | use cases per glossary concept (`GroupService`, `ContactService`, …), `AccessGuard`, validation, Version writing, Export, removal job | `repository`, `domain` |
| `repository` | Spring Data JPA repositories; every method on Group data takes the Group id (AR-04) | `domain` |
| `domain` | JPA entities and value types, named as the glossary (ES-13) | — |
| `auth` | Firebase ID token filter, current User, rate limit (only user of Firebase Admin SDK, AR-06) | `service` (User provisioning) |
| `config` | Spring configuration, typed properties (ES-01) | all |

Entities never leave `service`: controllers map from service result records.

## Consequences
- Simple ArchUnit rules (layered architecture, `auth`-only Firebase, no `repository` in `web`).
- Packages grow by file count, not depth; acceptable for ~10 concepts.
