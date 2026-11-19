# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S7 /verify (run 1)
- Result: done
- Gate: phase 6 not passed (F-35, F-36 High; F-37 … F-39 Medium; F-40 … F-46 Low; D-90)
- Notes for the next session:
  - Report: `docs/06_verification-report.md` "Run 1"; raw output `out/logs/s7-*`. Hidden suite 123/123 at c56a1af; manifests match.
  - Fix order: F-35, F-36 (images; rebuild without cache, guava ≥ 33.7.2; a new base-image tag is a tech-stack change, so a blocking decision), F-37 (removal job < 60 s + the spec §8 NFR-05 integration test), F-38 (nginx `access_log` without paths/query), then F-40 … F-45. F-46 cannot be fixed (no history rewrite): needs an acceptance decision.
  - Local stack runs with `APP_HOST_PORT=8090` (8080 is taken on the host); `export APP_HOST_PORT=8090` before any `docker compose` call or the backend is recreated with the wrong `APP_PUBLIC_URL`.
  - The stack now holds the capacity seed; browser tests reset it themselves.
  - F-39 / D-90: Firefox and WebKit can only run after the human installs them; until then re-verify NFR-06 in Chromium only.
  - DoD-R-09, DoD-R-10, DoD-P04 (README part) belong to /release; DoD-P02, P03 (staging), P05 need a staging host (Q-39 OPEN).

## Next
- Command: /fix
- Waiting for: D-90 (for the next /verify; /fix can start)

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
