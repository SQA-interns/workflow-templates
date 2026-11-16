# ADR-03: Authentication and rate limit

- Status: accepted · Date: 2026-11-16 · Session: S3 /plan
- Drivers: R-001-01 … R-001-06, SR-01, SR-03, SB-02, SB-06, D-16, D-20 (via D-27), AR-06

## Context
Firebase Authentication with e-mail/password and Google (D-16). The backend must verify the Firebase ID token on every request, provision the User on first use, refuse unverified e-mail accounts except `GET /api/v1/me`, refuse deleted Users for good (D-20), and limit each User to 100 requests per minute.

## Options
1. Servlet filter in `auth` that verifies the token with the Firebase Admin SDK (`verifyIdToken`, emulator via `FIREBASE_AUTH_EMULATOR_HOST`) and sets a Spring Security authentication holding the User id.
2. Spring Security OAuth2 resource server with Google's JWKS.

## Decision
Option 1, because the Admin SDK handles the emulator transparently and is also needed to delete Firebase accounts (US-021).
- Filter order: token verification → User lookup or creation (R-001-04; a User marked deleted → 401, never re-created) → e-mail verified check (only `GET /api/v1/me` allowed while `email_verified` is false and the sign-in provider is `password`; else 403 `auth.emailNotVerified`) → rate limit.
- Rate limit: Bucket4j in-memory token bucket per User id, `RATE_LIMIT_PER_MINUTE` (default 100); exceeding → 429 with `Retry-After` seconds (SR-03). One backend instance (ADR-11), so in-memory state is enough.
- Public: `/actuator/health` only (SR-01).
- The frontend uses the `firebase` web SDK (modular `firebase/app` and `firebase/auth` imports only), attaches `Authorization: Bearer <ID token>` and refreshes the token through the SDK.

## Consequences
- Revocation is not checked per request (no network call); deleted Users are refused through the User row (D-20).
- Moving to several backend instances requires a shared rate-limit store (new ADR).
