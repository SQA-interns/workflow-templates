# Requirements check

> For: agent (writes), human (reads) · Written in: phase 1 (`/clarify`) · Source: `project/01_requirements/*`, `general/requirements-standard.md`, `general/quality/definition-of-ready.md` · Agent: writes (results and gaps only; do not copy the requirements)

Run 1: 2026-11-16, session S2: gate not passed, D-15 … D-21 asked. Run 2: 2026-11-16, session S2: the human answered option 1 for each (D-27). **Phase 1 gate passed:** every story meets DoR-S, the run meets DoR-R, no Block decision is open. Non-blocking decisions D-22 … D-26 apply with their defaults (pending review).

## Run readiness (DoR-R)

| Item | Result | Note |
|---|---|---|
| DoR-R-01 | Pass | Problem, users, outcome, MVP, non-goals, principles, DoD-P01 … DoD-P05 present. "Who suffers" (OQ-P-01) and production host (OQ-P-06) stay open; both due before release, not blocking. |
| DoR-R-02 | Pass | Recycle bin period confirmed 30 days (D-18 via D-27); `[ASSUMPTION]` facts take D-22. |
| DoR-R-03 | Pass | Every story ready after D-27 (table below). |
| DoR-R-04 | Pass | Every scenario names stories and criteria; every MVP story (US-001 … US-016, US-020) is in at least one of E2E-01 … E2E-08. |
| DoR-R-05 | Pass with D-26 | NFR-01 … NFR-09 and DoD-P01 … DoD-P05 say how they are checked; SR-02 does; SR-01, SR-03 … SR-09 did not (methods proposed in D-26). |
| DoR-R-06 | Pass | Phase 0 gate passed (preflight run 4, `c8581ac`). |

## Stories

Rules → criteria: rule number → the AC/EX that cite it (`AC01` = `AC-nnn-01`); shared rules listed after. **none** = no criterion cites the rule (finding).

| Story | Ready | Failing DoR-S items | Rules → criteria | Findings (decision ids) |
|---|---|---|---|---|
| US-001 | yes | — | 01→AC01,AC02,EX01,EX05; 02→**none**; 03→AC03,AC04,EX02,EX04; 04→AC01,AC02,EX03; 05→AC05; 06→AC06; 07→AC01 | D-15, D-16, D-20, D-27 |
| US-002 | yes | — | 01→AC01,EX04; 02→AC04,EX01,EX02,EX03; 03→AC03; 04→AC02; 05→AC05,EX06; BR-01→AC02; BR-02→EX05 | D-15, D-22, D-27 |
| US-003 | yes | — | 01→AC01,EX06; 02→AC03,EX04; 03→AC03,EX05; 04→AC03; 05→AC04,EX01; 06→EX02; BR-04→AC02,EX03; BR-07→EX04; BR-08→AC05; BR-10→AC03,AC04 | D-15, D-18, D-19, D-26, D-27 |
| US-004 | yes | — | 01→AC01,EX06,EX07; 02→EX04; 03→EX05; 04→AC02,EX01,EX03; 05→AC03; 06→AC04,AC06,EX02,EX08; 07→**none**; 08→AC01; 09→AC06; BR-04→AC05; BR-10→AC01,AC06; BR-11→EX03 | D-15, D-16, D-17, D-26, D-27 |
| US-005 | yes | — | 01→AC01,AC04; 02→AC02,EX03; 03→AC03,EX04,EX05; 04→AC03; 05→EX01,EX02; BR-02→AC03; BR-03→AC02; BR-04→AC05; BR-10→AC02,AC03 | D-15, D-26, D-27 |
| US-006 | yes | — | 01→AC01,AC02,EX08; 02→AC02,AC03,EX05; 03→EX01 … EX04; 04→AC04; 05→EX06; 06→AC05; 07→EX07; BR-02→EX09; BR-03→AC06; BR-11→AC05 | D-15, D-23, D-27 |
| US-007 | yes | — | 01→AC01,EX01,EX03; 02→AC03; 03→AC04,EX02; 04→AC02,EX04; BR-02→EX05; BR-03→AC05; BR-06→AC02 | D-15, D-23, D-27 |
| US-008 | yes | — | 01→AC01,AC02,AC04,EX06; 02→EX01,EX02; 03→EX03,EX04; 04→AC03; 05→EX05; 06→**none**; BR-06→EX07 | D-15, D-23, D-27 |
| US-009 | yes | — | 01→AC01,AC04; 02→AC02,EX04,EX05; 03→AC03; 04→AC01,EX06; 05→AC05,EX01,EX02; 06→EX03; BR-01→EX04; BR-06→EX07 | D-15, D-22, D-23, D-27 |
| US-010 | yes | — | 01→AC01,EX01,EX02,EX04; 02→AC02,EX05; 03→EX03; BR-02→AC03 | D-15, D-27 |
| US-011 | yes | — | 01→AC01,AC02,EX03,EX04; 02→AC01; 03→AC04; 04→EX02; BR-03→AC03; BR-06→EX01 | D-15, D-23, D-27 |
| US-012 | yes | — | 01→AC01,EX03,EX06; 02→AC05; 03→AC02,EX01; 04→EX02; 05→AC03; BR-03→AC04; BR-06→EX04; BR-07→EX03; BR-08→AC03; BR-09→AC01,EX05 | D-15, D-18, D-23, D-27 |
| US-013 | yes | — | 01→AC01,EX01,EX02,EX04; 02→AC01; 03→AC03; 04→EX03; BR-04→AC02; BR-07→EX01 | D-15, D-18, D-19, D-21, D-26, D-27 |
| US-014 | yes | — | 01→AC01,AC05,EX01,EX02,EX06; 02→AC01,EX04; 03→**none**; 04→AC02,AC03,EX03,EX05; 05→AC04; 06→**none**; BR-01→EX07; BR-09→AC01 | D-15, D-22, D-24, D-27 |
| US-015 | yes | — | 01→AC01,EX01; 02→AC02; 03→AC03,EX03; 04→AC05; 05→AC02,AC05,EX02; 06→AC04; 07→**none**; BR-02→EX04; BR-09→EX05 | D-15, D-22, D-27 |
| US-016 | yes | — | 01→AC01,EX06; 02→AC02; 03→AC01,EX04; 04→AC04; 05→AC03,EX01; 06→AC01,EX05; 07→EX02; BR-09→EX07; BR-10→AC01; SR-07→EX03 | D-15, D-19, D-24, D-26, D-27 |
| US-017 | yes | — | 01→AC01,EX03,EX07; 02→AC01,EX01; 03→AC02,EX02; 04→AC03,AC04,EX06; 05→AC05; BR-01→EX05; BR-09→EX04 | D-15, D-22, D-25, D-27 |
| US-018 | yes | — (US-018: 02 met through D-25) | 01→AC05; 02→AC01,EX07; 03→AC03,EX01; 04→AC01; 05→AC02; 06→EX02; 07→EX03,EX04,EX05; BR-03→AC04; BR-07→EX06 | D-15, D-21, D-25, D-26, D-27 |
| US-019 | yes | — | 01→AC01,EX01,EX03; 02→AC02; 03→EX02; BR-03→AC03 | D-15, D-21, D-25, D-27 |
| US-020 | yes | — | 01→AC01; 02→AC02,EX01; 03→AC03,EX02 | D-22 |
| US-021 | yes | — | 01→AC01,EX01,EX03; 02→AC02,EX02; 03→AC01; R-001-03→EX03 | D-15, D-20, D-27 |

DoR-S-01, 03, 04, 06, 08, 10 pass for every story (front matter, story, purpose, EARS rules, Given/When/Then with values, out of scope, cited IDs exist, screens named). DoR-S-07 and DoR-S-09: every `OPEN` and `[ASSUMPTION]` item is confirmed by D-27 (Block topics) or recorded in D-22 … D-26 (Open).

## Findings

| ID | Story or file | Property or DoR item | What is missing | Consequence of a wrong reading | Class | Decision |
|---|---|---|---|---|---|---|
| Q-07, Q-17, Q-18 | intent, BR-02 … BR-04, US-002, US-005, US-011, US-012, US-016, US-018 | DoR-S-07, 09 | Roles and their rights not confirmed by the customer | A Member can see, export or change data they must not (P-1) | Block → answered | D-15, D-27 |
| R-001-05 | US-001 | unambiguous, DoR-S-07 | E-mail verification unconfirmed; "every other request" names no allowed request | UI cannot load the User, or unverified accounts take over Invitations | Block → answered | D-16, D-27 |
| SR-03, ASVS level | security-requirements | DoR-S-07, 09 | Rate limit and verification level unconfirmed (Q-34) | Legitimate Users locked out, or abuse not limited | Block → answered | D-16, D-27 |
| R-004-03, R-004-05 | US-004 | DoR-S-07, 09 | Invitation binding and replacement unconfirmed (Q-35) | A forwarded link admits the wrong person | Block → answered | D-17, D-27 |
| BR-10 | business-rules, US-004 | complete | Automatic revocation (R-004-03) and acceptance not in the audited actions | No record of who joined a Group | Block → answered | D-17, D-27 |
| Recycle bin period, BR-08 | business-rules, US-003, US-012, US-013 | DoR-R-02, DoR-S-07 | 30 days, Owner-only permanent delete, hourly removal unconfirmed | Data kept too long or erased too early | Block → answered | D-18, D-27 |
| R-003-05 / BR-08 | US-003 | consistent | Contact removal dates inside a Group in the recycle bin | Restored Group silently lacks Contacts, or data outlives its period | Block → answered | D-18, D-27 |
| OQ-P-04, BR-08 | intent, security-requirements | DoR-S-07, complete | Controller/processor, retention unconfirmed; Group erasure does not list Audit entries and Merge records | Personal data kept without a basis | Block → answered | D-19, D-27 |
| R-021-01, R-001-04 | US-021, US-001 | consistent, feasible | "Delete the User" breaks references; a still-valid ID token re-creates a deleted User | Deleted account comes back for up to 1 hour | Block → answered | D-20, D-27 |
| R-013-04, R-012-01, BR-08 | US-012, US-013, US-018 | complete, consistent | A `merged` Contact cannot be erased; BR-08 on a Primary contact leaves merged Contacts dangling | Right to erasure not met | Block → answered | D-21, D-27 |
| `[ASSUMPTION]`s, OQ-009-01, OQ-015-01, OQ-017-01, OQ-017-02 | all | DoR-S-07, 09 | No recorded default | Silent choices in code | Open | D-22 |
| Field | glossary, US-006, US-016, US-018 | unambiguous | Which Data-model items are Fields | Form, Export columns and Merge choices show ids, state or Version numbers | Open | D-23 |
| phone | Data model, US-006 | complete, DoR-S-05 | No rule or edge case for disallowed phone characters | Invalid phones stored or rejected without a message | Open | D-23 |
| R-007-01, R-008-01, R-009-01, R-011-01, R-012-01 | US-007 … US-012 | complete | Changes to a `merged` Contact | Hidden Contact changed; Undo merge corrupts data | Open | D-23 |
| R-014-03 | US-014 | unambiguous | "Sort by name" by first or last name | List order differs from tests | Open | D-24 |
| R-016-02 | US-016 | complete | Lifetime of Checked contacts | Export contains rows the User no longer sees | Open | D-24 |
| R-016-06 | US-016 | unambiguous | Set of characters "not allowed in file names" | File cannot be saved on Windows | Open | D-24 |
| R-016-04, R-016-06 | US-016 vs project-wide facts | consistent | Country as code and `yyyy-mm-dd` contradict the facts no story may override | Export differs from the rest of the product | Open | D-24 |
| Name similarity, AC-017-03 | US-017 | unambiguous, consistent | Trigram measure undefined; with `pg_trgm` AC-017-03 scores 0.67 < 0.8 | AC-017-03 fails, or the threshold is tuned silently | Open | D-25 |
| R-018-01 | US-018 | unambiguous | "First Contact" of a pair | Primary contact preselection differs from tests | Open | D-25 |
| EX-019-01, R-019-01 … 03 | US-019 | complete, consistent | No rule for "already undone"; no stale-Version check; Primary itself merged later | Wrong message, lost concurrent change | Open | D-25 |
| US-018 | US-018 | DoR-S-02 (small) | 8 points > a third of increment 2 (19) | Does not fit one implement session | Open | D-25 |
| AC with "an Audit entry is written" | US-003, 004, 005, 013, 016, 018, 019 | verifiable | Audit entries have no public interface | Criteria cannot be tested as written | Open | D-26 |
| SR-01, SR-03 … SR-09 | security-requirements | DoR-R-05 | How each is checked | Requirement silently untested | Open | D-26 |
| R-001-02, R-004-07, R-008-06, R-014-03, R-014-06, R-015-07 | US-001, 004, 008, 014, 015 | traceability | No AC or EX cites the rule | Rule untested | Allow: tests cite the `R` id (requirements-standard, Traceability) | — |
| R-001-05, R-004-05, R-006-04, R-011-03, R-013-03, R-014-05, R-019-02 | several | DoR-S-05 | `If … then` rule with an AC but no edge-case row | — | Allow: the cited AC is the oracle | — |
| "delete" (US-003 title and story, BR-04, US-012 story), "Audit log" (US-010, AC-013-01) | several | DoR-S-11 | Glossary synonyms ("delete" alone; "Audit log" is not a term) | UI copy uses the wrong word | Allow: glossary meaning applies (Move to recycle bin; Audit entry); UI and code use glossary terms | — |
| Removal date (US-003, US-012), Normalised text (US-014, used by US-017) | several | DoR-S-11 | Shared terms defined in stories, not the glossary | Two meanings drift | Allow: identical meaning; one implementation | — |
| `rules` front matter | US-005 (BR-03), US-006, US-007 (BR-02), US-015 (BR-02) | traceability | Front matter omits a BR the body cites | Traceability matrix incomplete | Allow: the body citation is authoritative | — |
| 30 days, 7 days | US-012 story and purpose; R-004-01, glossary, SR-05 | "no fact in two places" | Facts repeated with the same value | A later change misses a copy | Allow: Data model and project-wide facts are the source | — |
