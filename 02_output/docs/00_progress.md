# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 /preflight (run 2)
- Result: blocked
- Gate: phase 0 not passed (Node on PATH, `.env` DB_PASSWORD, D-10, D-11)
- Notes for the next session:
  - Pins in effect are `tech-stack.md` plus decisions D-01, D-02, D-07, D-08, D-09 (decisions-log); apply them, do not re-ask.
  - Re-check only: `node --version` in `bash -c` and `zsh -c` (expect 22.23.3); `.env` DB_PASSWORD; Trivy re-scan for the options chosen in D-10 and D-11.
  - Scan inputs: throw-away manifests (pom.xml, package.json + lock) were built in the session scratchpad; rebuild them from the pins if gone. Trivy runs as `aquasec/trivy:0.67.2` with the `trivy-cache` Docker volume.
  - Host port 9099 is held by an unrelated container `contacts-auth-1`.
  - Then write `docs/00_input-manifest.sha256` and commit.

## Next
- Command: /preflight
- Waiting for: D-10, D-11, Node 22.23.3 on PATH, `.env` with DB_PASSWORD

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
