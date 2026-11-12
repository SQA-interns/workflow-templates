# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 /preflight
- Result: blocked
- Gate: phase 0 not passed (tools, secrets, dependency vulnerabilities)
- Notes for the next session:
  - Re-run only the failed checks (`docs/00_preflight-report.md`): Node version, npm tools, Playwright screenshot, Firebase emulator, Trivy/gitleaks/k6, `.env` DB_PASSWORD, resolution and scan of the versions approved in D-02, D-05, D-06.
  - Run the Trivy scan on a throw-away manifest in the scratchpad (no lock file exists yet); transitive npm packages are unchecked so far.
  - Then write `docs/00_input-manifest.sha256`.

## Next
- Command: /preflight
- Waiting for: D-03, D-04, D-05, D-06, `.env` with DB_PASSWORD

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
