# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: R /review INC-1
- Result: done
- Gate: DoD-I not passed (DoD-I-01: US-012, US-020 DoD-S-01 open until D-65 is applied; US-013 deferred to INC-2 by D-68); DoD-I-02 … 04 met
- Notes for the next session:
  - `docs/04_review-INC-1.md`: 0 Critical, 0 High, 14 Medium (F-01 … F-14), 20 Low (F-15 … F-34). No fixes were made.
  - Medium and Low findings go to the next session of their story or to the release notes. The ones that touch INC-2 code paths (F-01 Tag count, F-02 Tag race, F-03 500 on malformed input, F-04 User provisioning, F-05 form reset) are worth fixing first.
  - F-14 answered (D-68 option 1): US-013's DoD-S-01 is accepted at the end of INC-2; EX-013-03 must pass after US-018 (T-47) and `/review INC-2` checks it.
  - D-65 (approved) is still to be applied by `/acceptance-tests`: frozen `stubGet` fulfils only fetch/xhr. Then re-run US-012 AC-012-01 and US-020 EX-020-02.
  - Local stack 8090 holds review data (Groups "secrev", "Review Beta", extra emulator Users); `scripts/seed-local.sh` fails on it, so reset the volumes before the next run that needs the seed state.
  - Screens: `out/logs/screens/review-inc1/` (119, at 375/768/1440).

## Next
- Command: /acceptance-tests D-65
- Waiting for: nothing (after D-65 is applied and AC-012-01, EX-020-02 pass: /implement US-017)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | INC-1 | done | met |
| US-002 | INC-1 | done | met (DoD-S-01 met after US-004: API 10/10, UI 5/5) |
| US-003 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 6/6); DoD-S-07 5ef6708 spotless, b07874a … 7b687c6 checkstyle/SpotBugs (fixed in dc2c564, 896ccb9), bdb4dec subject 75 characters; others met |
| US-004 | INC-1 | done | met; DoD-S-07 see D-63 (j) |
| US-005 | INC-1 | done | met (API 9/9, UI 2/2); DoD-S-07 577e900 subject 74 characters (D-66 (i)); others met |
| US-006 | INC-1 | done | DoD-S-01 met after US-004 (API 15/15, UI 5/5); DoD-S-07 see notes of US-006; others met |
| US-007 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 4/4); others met |
| US-008 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 one commit over 400 lines (2ae5475); others met |
| US-009 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); others met |
| US-010 | INC-1 | done | DoD-S-01 met after US-004 (API 8/8, UI 3/3); others met |
| US-011 | INC-1 | done | DoD-S-01 met after US-004 (API 7/7, UI 3/3); DoD-S-07 one subject over 72 characters (24e39f9); others met |
| US-012 | INC-1 | in progress | DoD-S-01 open: API 10/10, UI 3/4, AC-012-01 waits for the approved `stubGet` change (D-64, D-65); others met |
| US-013 | INC-1 | in progress | DoD-S-01 open until US-018 (API 5/6, EX-013-03 needs Merge; UI 2/2); DoD-S-07 one commit failed SpotBugs (527455f, fixed in ebd94c8); others met |
| US-014 | INC-1 | done | DoD-S-01 met after US-004 (API 12/12, UI 5/5); DoD-S-07 see notes of US-014; others met |
| US-015 | INC-1 | done | DoD-S-01 met after US-004 (API 10/10, UI 5/5); DoD-S-07 c1b76ff subject without IDs, bacf2c9 subject 73 characters, c1b76ff … bacf2c9 failed ArchitectureTest (fixed in 3f369fd); others met |
| US-016 | INC-1 | done | DoD-S-01 met after US-004 (API 11/11, UI 4/4); DoD-S-07 b7b36b7 554 lines, 5168f66 and daf3e85 subjects over 72 characters; others met |
| US-017 | INC-2 | ready | |
| US-018 | INC-2 | ready | |
| US-019 | INC-2 | ready | |
| US-020 | INC-1 | in progress | DoD-S-01 open until the approved `stubGet` change (D-51, D-65); US-010 part of D-44 passes; D-48 fixed; others met |
| US-021 | INC-2 | ready | |
