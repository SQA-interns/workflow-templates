# Wireframes

> For: agent (writes), human (reads) · Written in: phase 2 (`/wireframes`) · Source: `docs/02_specification.md` ("User interface"), `general/design/ui-baseline.md` · Agent: writes

Grey wireframes of every screen at the three widths (UI-22), approved by the developer before any screen is built. Files: `WF-nn_<slug>.html`, `wireframe.css`, `WF-nn_<slug>_mobile.png`, `…_tablet.png`, `…_desktop.png`.

Conventions: wording is the final English text of the stories and the glossary (ES-13); the Slovenian catalog uses the same structure. Grey bars stand for dynamic content (UI-28). Dialogs, menus and panels that open on demand are drawn open and labelled "(open)". Each data screen shows its loading, empty and error states under "States" (UI-14). Elements of increment 2 are marked "(increment 2)". Actions limited by Role follow ADR-04; the notes on WF-06 and WF-11 say which.

| Id | Screen | Stories and AC | States | Approved (decision) |
|---|---|---|---|---|
| WF-01 | Sign in (`WF-01_sign-in`) | US-001 AC-001-01, AC-001-02, EX-001-01 | form, error | D-34, D-35 (developer) |
| WF-02 | Create an account (`WF-02_sign-up`) | US-001 AC-001-01, EX-001-05 | form, error | D-34, D-35 (developer) |
| WF-03 | Confirm your e-mail (`WF-03_confirm-email`) | US-001 AC-001-05 | message | D-34, D-35 (developer) |
| WF-04 | Create a Group (`WF-04_create-group`) | US-002 AC-002-01, AC-002-04 | form, error | D-34, D-35 (developer) |
| WF-05 | No Groups yet (`WF-05_no-groups`) | US-001 R-001-07, AC-001-01, EX-002-06 | empty | D-34, D-35 (developer) |
| WF-06 | Contact list (`WF-06_contact-list`) | US-014 AC-014-01 … 05; US-015 AC-015-01 … 05; US-016 AC-016-01 … 03, EX-016-02; navigation (Group switcher, user menu) | success, loading, empty, no match, error, export not possible | D-34, D-35 (developer) |
| WF-07 | Contact details (`WF-07_contact-details`) | US-014 R-014-06; US-012 AC-012-01; US-018 AC-018-02; US-019 AC-019-01, AC-019-02 | success, loading, error, not found | D-34, D-35 (developer) |
| WF-08 | Contact form (`WF-08_contact-form`) | US-006 AC-006-01 … 05; US-007 AC-007-01 … 04; US-008 AC-008-01 … 04; US-009 AC-009-01 … 05 | form, field errors, conflict, loading, error | D-34, D-35 (developer) |
| WF-09 | History (`WF-09_history`) | US-010 AC-010-01, EX-010-01, EX-010-02; US-011 AC-011-01 | success, loading, error | D-34, D-35 (developer) |
| WF-10 | Version (`WF-10_version`) | US-010 AC-010-02; US-011 AC-011-01 … 04, EX-011-04 | success, loading, error, not found | D-34, D-35 (developer) |
| WF-11 | Recycle bin (`WF-11_recycle-bin`) | US-012 AC-012-01 … 03, EX-012-02; US-013 AC-013-01; D-21 | success, loading, empty, error | D-34, D-35 (developer) |
| WF-13 | Members and Invitations (`WF-13_members`) | US-004 AC-004-01, AC-004-06, EX-004-04; US-005 AC-005-01 … 03, EX-005-01 | success, loading, empty, error | D-34, D-35 (developer) |
| WF-14 | Group settings (`WF-14_group-settings`) | US-003 AC-003-01, AC-003-03, EX-003-04, EX-003-06 | form, loading, error | D-34, D-35 (developer) |
| WF-15 | Deleted Groups (`WF-15_deleted-groups`) | US-003 AC-003-03, AC-003-04, EX-003-02 | success, loading, empty, error | D-34, D-35 (developer) |
| WF-16 | Invitation (`WF-16_invitation`) | US-004 AC-004-02 … 04, R-004-07, EX-003-05 | success, other e-mail, no longer valid, not signed in, loading | D-34, D-35 (developer) |
| WF-17 | Possible duplicates (`WF-17_possible-duplicates`) | US-017 AC-017-01 … 03, EX-017-03, EX-017-07 | success, loading, empty, error | D-34, D-35 (developer) |
| WF-18 | Merge (`WF-18_merge`) | US-018 AC-018-01, AC-018-03, AC-018-05, EX-018-02 | form, errors, loading, error | D-34, D-35 (developer) |
| WF-19 | Account (`WF-19_account`) | US-020 AC-020-02; US-021 AC-021-02, EX-021-01 | form, loading, error | D-34, D-35 (developer) |
| WF-20 | Not found (`WF-20_not-found`) | BR-02, E2E-03, AC-013-01 | message | D-34, D-35 (developer) |

WF-12 is not used (specification §9.1).

## Checks (step 5)

- Every screen of specification §9.1 has a page and three images (19 pages, 57 images).
- Navigation reaches every screen: header Contacts (WF-06), Recycle bin (WF-11), Possible duplicates (WF-17), Members (WF-13), Settings (WF-14); Group switcher → Create a Group (WF-04); user menu → language, Account (WF-19), Deleted Groups (WF-15), Sign out (WF-01); list row → WF-07 → Edit (WF-08), History (WF-09) → View (WF-10); WF-17 Merge → WF-18; Invitation link → WF-16; sign-in flow WF-01 → WF-02 → WF-03 → WF-05; any unknown address → WF-20.
- No colour: `wireframe.css` uses only neutral greys (`#262626` … `#ffffff`, equal RGB channels); no inline colour in any page; no image, shadow, gradient or animation (UI-26).
- No horizontal overflow at 375, 768 or 1440 px (checked by the render script); every image looked at.
