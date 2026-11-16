# ADR-08: Frontend structure, routing and state

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: UI-01, UI-03, UI-22 … UI-25, UI-30, AR-01, R-020-01 … R-020-03, ES-11

## Context
React SPA (tech-stack) served by nginx, two languages, about 15 screens.

## Options
1. React Router (declarative SPA routes), TanStack Query for server state, react-hook-form + zod for forms, i18next catalogs, shadcn/ui + Tailwind tokens.
2. A framework with server rendering.

## Decision
Option 1. Folders under `src/`:

| Folder | Holds |
|---|---|
| `app/` | providers, router, layout (header with Group switcher, user menu, main) |
| `api/` | typed client for `/api/v1` (fetch + ID token), query hooks per resource |
| `auth/` | Firebase sign-in, current User, guards |
| `features/<concept>/` | screens and components per glossary concept (`groups`, `members`, `invitations`, `contacts`, `history`, `recycle-bin`, `export`, `duplicates`, `account`) |
| `components/ui/` | generated shadcn/ui primitives |
| `i18n/` | `sl.json`, `en.json`, setup, date and number formatters (project-wide facts) |
| `styles/` | `tokens.css` (imports the active theme), `theme-wireframe.css`, `theme-final.css` (UI-03) |

The language comes from `GET /api/v1/me` and is changed with `PATCH /api/v1/me`; before sign-in the UI is Slovenian (R-020-01). The last selected Group is kept in `localStorage` per User id (R-002-05, D-22).

## Consequences
- Screens own no styling values; switching the theme is one import (UI-03).
- A route list and a screen inventory in the specification drive `/wireframes`.
