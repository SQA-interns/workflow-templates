# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S9 /verify (run 2)
- Result: done
- Gate: phase 6 passed
- Notes for the next session:
  - Run 2 in `docs/06_verification-report.md`: no finding open; F-35 … F-45 closed, F-46 accepted (D-93). Hidden evaluation Run 2 at `71dcc9c`: 123/123, manifest ok.
  - D-94 has no `Human response` yet, but its option 1 was carried out: `libwoff1` is installed and WebKit passes 152/152. Record the human's answer (`/release D-94: 1`) before closing the decision.
  - The host JDK was upgraded in place by unattended-upgrades on 2026-11-20 (21.0.13 → 21.0.13.1; D-01 pins 21.0.13). Pinned tool reporting another version: record a non-blocking decision. PIT ran on 21.0.13.1; the other backend runs used 21.0.13.
  - After the host restart, background shells used the system Node 18; Node 22.23.3 (D-07) comes from nvm. Check `node -v` first.
  - The local stack (`kontakti`) is stopped (it was stopped for the mutation runs to free memory). Start it with `APP_HOST_PORT=8090 scripts/up.sh`.
  - Mutation: backend 97.07 %, frontend 94.87 % (raw = adjusted). `docs/03_test-strategy.md` still shows the phase 5 figures.
  - Release work: staging (Q-39 OPEN) for DoD-P02, DoD-P03, DoD-P05, TLS; root README, release notes, staging/production compose files (ADR-11).

## Next
- Command: /release
- Waiting for: D-94 (Human response only; option 1 already done)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | done | met (DoD-S-01 met after US-004: API 10/10, UI 5/5); F-05 fixed (a264259, E2E-08 passes), F-06 header fixed (089a5a8, D-81) |
| US-003 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 6/6); DoD-S-07 5ef6708 spotless, b07874a … 7b687c6 checkstyle/SpotBugs (fixed in dc2c564, 896ccb9), bdb4dec subject 75 characters; others met |
| US-004 | INC-1 | done | met; DoD-S-07 see D-63 (j) |
| US-005 | INC-1 | done | met (API 9/9, UI 2/2); DoD-S-07 577e900 subject 74 characters (D-66 (i)); others met |
| US-006 | INC-1 | done | DoD-S-01 met after US-004 (API 15/15, UI 5/5); DoD-S-07 see notes of US-006; others met |
| US-007 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 4/4); others met |
| US-008 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 one commit over 400 lines (2ae5475); others met |
| US-009 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); others met |
| US-010 | INC-1 | done | DoD-S-01 met after US-004 (API 8/8, UI 3/3); others met |
| US-011 | INC-1 | done | DoD-S-01 met after US-004 (API 7/7, UI 3/3); DoD-S-07 one subject over 72 characters (24e39f9); others met |
| US-012 | INC-1 | done | DoD-S-01 met after D-65 (API 10/10, UI 4/4, D-69); others met |
| US-013 | INC-1 | done | DoD-S-01 met after US-018 (API 6/6, UI 2/2; EX-013-03 needed 4657746); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); DoD-S-07 see notes of US-014; others met |
| US-015 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 5/5); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 b7b36b7 554 lines, 5168f66 and daf3e85 subjects over 72 characters; others met |
| US-017 | INC-2 | done | DoD-S-01 met after US-018 (API 12/12, UI 4/4; D-71); DoD-S-07 see D-70 (i); others met |
| US-018 | INC-2 | done | met (API 10/10, UI 4/4; WF-17, WF-18, WF-07 at three widths, axe clean); DoD-S-07 see D-72 (j), D-74 (c); F-01 fixed (8021943, D-79) |
| US-019 | INC-2 | done | met (API 5/5, UI 3/3; WF-07 Undo merge at three widths, axe clean); DoD-S-07 see D-75 (i) |
| US-020 | INC-1 | done | DoD-S-01 met after D-65 (UI 4/4, D-69); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | done | met (API 5/5, UI 3/3 after D-77; WF-19 at three widths, axe clean); DoD-S-07 see D-76 (i); F-06 fixed (983123a, header part 089a5a8); F-02 fixed (58affcd, D-80); cross-cutting T-36 … T-43, T-53 done (D-82, D-83) |
