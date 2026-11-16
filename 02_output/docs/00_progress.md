# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-020
- Result: done
- Gate: DoD-S not passed (DoD-S-01: UI tests of US-020 wait for US-002, US-014, US-010, D-44)
- Notes for the next session:
  - T-06 built test-first: `abde4e1`, `82f3625` (backend `PATCH /me`), `c3aa3f1` (`i18n/format.ts`: `formatDate`, `formatDateTime`, `formatNumber`), `766570f` (user menu Slovenščina/English, texts follow `GET /me`). Use `formatDateTime`/`formatNumber` on WF-06 (count) and WF-09 (time).
  - Frozen API `Us020LanguageAcceptanceIT` 3/3 pass. UI `us020-language.spec.ts` 0/4: all fail at `POST /groups` (US-002), then need WF-06 heading/count (US-014) and WF-09 (US-010). Re-run there; set US-020 done when they pass (D-44).
  - US-001 UI now: AC-001-01, 02, 05, EX-001-01, 05 pass; AC-001-06 and the 7 API tests wait only for `/groups` (US-002, D-39).
  - Full backend run: no regressions (`out/logs/implement-us020-backend-full.txt`); backend check clean; frontend lint, types, prettier on `src` clean, 64 unit tests green; gitleaks clean.
  - `npm run check` warns on `frontend/README.md` formatting (pre-existing since `381c42f`, not this story's file).
  - The frontend image copies `dist/`: run `npm run build` before `docker compose --build`. Stack: `APP_HOST_PORT=8180 docker compose -p kontakti --env-file ../.env up -d --build` (8080 is taken on this host).
  - Open non-blocking assumptions: D-45 (WF-19 language section left to US-021, wording, error feedback).

## Next
- Command: /implement US-002
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | in progress | DoD-S-01 open until US-002 (D-39); others met |
| US-002 | INC-1 | ready | |
| US-003 | INC-1 | ready | |
| US-004 | INC-1 | ready | |
| US-005 | INC-1 | ready | |
| US-006 | INC-1 | ready | |
| US-007 | INC-1 | ready | |
| US-008 | INC-1 | ready | |
| US-009 | INC-1 | ready | |
| US-010 | INC-1 | ready | |
| US-011 | INC-1 | ready | |
| US-012 | INC-1 | ready | |
| US-013 | INC-1 | ready | |
| US-014 | INC-1 | ready | |
| US-015 | INC-1 | ready | |
| US-016 | INC-1 | ready | |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until US-002, US-014, US-010 (D-44); others met |
| US-021 | INC-2 | ready | |
