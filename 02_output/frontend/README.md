# Frontend (Contacts web application)

> For: human, agent · Component `frontend` of `project/02_design/architecture.md` · Design: `docs/02_specification.md` ("User interface"), ADR-05, ADR-08

React single-page application in Slovenian and English, served by nginx, talking to the backend only through `/api/v1` (AR-01).

## Prerequisites

- Node.js 22.23.3 (first on `PATH`), npm.
- For browser tests: Playwright browsers (`npx playwright install chromium firefox webkit`) and the local stack (`docker compose up` in `02_output/`).

## Configuration

Runtime settings come from `/config.json` (contract `docs/02_contracts/runtime-config.schema.json`, ADR-05). Locally `public/config.json` points at the Auth emulator; the container writes it at start from:

| Variable               | Purpose                                                        | Source                             |
| ---------------------- | -------------------------------------------------------------- | ---------------------------------- |
| `FIREBASE_WEB_API_KEY` | Firebase web API key (not secret)                              | environment; local `fake-api-key`  |
| `FIREBASE_AUTH_DOMAIN` | Firebase auth domain                                           | environment; local `localhost`     |
| `FIREBASE_PROJECT_ID`  | Firebase project                                               | environment; local `demo-contacts` |
| `AUTH_EMULATOR_URL`    | Auth emulator for the browser; empty on staging and production | local `http://localhost:9199`      |

## Commands (ES-05)

| Task          | Command                                                               |
| ------------- | --------------------------------------------------------------------- |
| build         | `npm ci && npm run build` (type check, then `dist/`)                  |
| test          | `npm test` (Vitest with coverage in `coverage/`)                      |
| check         | `npm run check` (Prettier, ESLint with zero warnings, TypeScript)     |
| run           | `npm run dev` (Vite on port 5173, `/api` proxied to `localhost:8081`) |
| browser tests | `npm run e2e` (against `http://localhost:8080` or `E2E_BASE_URL`)     |
| mutation      | `npm run mutation` (Stryker, report in `reports/mutation/`)           |

## Deploy

The image `contacts-frontend:<tag>` (`Dockerfile`, nginx 1.30.7, user `nginx`, port 8080) is built by `../scripts/build-images.sh` and runs on staging and production through `../deploy/docker-compose.yml` (root `README.md`, "Deploy"), behind the TLS proxy. There it gets the Firebase web settings of the environment's project, and `AUTH_EMULATOR_URL` is empty, so `config.json` has no emulator entry and the CSP allows only Google's sign-in hosts. It proxies `/api/` to `http://backend:8080` (`BACKEND_URL`) and writes the redacted access log (F-38).

## Structure

`src/app` layout and routes · `src/api` API client · `src/auth` sign-in · `src/features/<concept>` screens · `src/components/ui` shadcn/ui primitives · `src/i18n` catalogs `sl.json`, `en.json` · `src/styles` tokens: `tokens.css` imports the active theme (`theme-wireframe.css` now, `theme-final.css` from `/design`, UI-03).

## Troubleshooting

- `npm ci` refuses to run: Node.js must be exactly 22.23.3 (`engine-strict`).
- Google sign-in fails on staging: add the domain to the Firebase project's authorized domains.
- Blank page with an error state: `/config.json` is missing or invalid.
