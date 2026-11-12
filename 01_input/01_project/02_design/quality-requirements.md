# Quality requirements

> For: agent (reads), human (reviews) · Owner: QA lead · Agent: read-only

Adds to `general/quality/` (definition of done, test strategy, severity scale); do not repeat those here. Project done criteria (`DoD-P`) are in `project/01_requirements/intent.md`.

## Non-functional requirements

| ID | Category (performance, availability, accessibility, localisation, usability, …) | Requirement | How it is checked |
|---|---|---|---|
| NFR-01 | performance | Contact list, search and filters of a Group with 10,000 Contacts respond in under 500 ms at the 95th percentile (Q-37). | A load test (k6) on the local stack seeded with 10,000 Contacts, 20 concurrent Users for 2 minutes; p95 from the report. |
| NFR-02 | performance | An Export of 10,000 Contacts completes in under 10 s (Q-37). | Timed test on the local stack with 10,000 Contacts, each with 5 Attributes and 3 Tags [ASSUMPTION data shape]. |
| NFR-03 | capacity | The system serves 50 Groups, 200 Users and 10,000 Contacts per Group within NFR-01 and NFR-02 (Q-37). | The NFR-01 test runs with a database seeded with 50 Groups and 200 Users, one Group holding 10,000 Contacts. |
| NFR-04 | availability | 99 % availability during working hours (project-wide facts) (Q-37). | Runtime demonstration: health endpoint, container restart policy and recovery after a backend restart in under 1 minute; measured in operation after release. |
| NFR-05 | performance | The removal job (BR-08) finishes for 10,000 Contacts in the recycle bin in under 1 minute [ASSUMPTION]. | Timed test on the local stack. |
| NFR-06 | compatibility | The web application works in the latest stable Chrome, Firefox, Edge and Safari on desktop and on tablets; mobile widths follow UI-08 (Q-37). | Browser tests run in Chromium, Firefox and WebKit (Playwright); Edge is Chromium-based. |
| NFR-07 | accessibility | WCAG AA as the UI baseline defines (UI-19); Q-37 asked for WCAG 2.1 AA, which the baseline's 2.2 AA includes. | Automated accessibility check of every screen with no Critical or Serious violation; keyboard walk-through of E2E-01. |
| NFR-08 | localisation | Every text exists in Slovenian and English; no text is missing in either catalog (Q-29, UI-30). | A test compares the keys of both message catalogs; screenshots of each screen in both languages. |
| NFR-09 | usability | DoD-P02 (2-minute first task). | As DoD-P02. |

## Thresholds

These thresholds are the **quality gate** of the project, per component, over the scope and with the tests defined in `general/testing/measurement.md`. The agent works until each component meets them, then stops adding tests for that component (rules and stop rule: "Quality gate" in `general/testing/measurement.md`). Keep the format `>= <number> %`.

| Measure | Threshold |
|---|---|
| Line coverage (all levels together) | >= 95 % |
| Mutation score (adjusted) | >= 90 % |
| Branch coverage | record only |

## Mutation scope

The paths or packages whose code is mutated (default: all production code of the component). Selection rule: validation, security, persistence and business-rule code. Rules: `general/testing/measurement.md`.

| Component | Paths or packages | Exclusions and reason |
|---|---|---|
| backend | `app.contacts` (all production code under `src/main/java`) | None. |
| frontend | `src/` (all production code) | Style and token files (presentation is not logic, `general/testing/measurement.md`); generated shadcn/ui primitives under `src/components/ui/` unless changed. |

## Overrides

None.
