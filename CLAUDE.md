# CLAUDE.md — Parallax Planner

App name: **Parallax Planner**. Android `applicationId` and root Kotlin package: **`com.cussou.parallax`** (permanent once published; do not change it).

Android app (a website is planned for V2, out of scope for now) that computes the optimal riflescope parallax setting for a distance range or a set of targets (PRS stages).

## Read before any work
1. `docs/00-project-context.md` — vision, glossary
2. `docs/01-requirements.md` — requirements F-xx / NF-xx
3. `docs/02-calculations.md` — **source of truth** for all formulas
4. `docs/03-ux.md` — screens and display rules
5. `docs/04-architecture.md` — stack, data model, core API
6. `docs/05-open-questions.md` — provisional decisions
7. `docs/06-name-and-icons.md` — naming and icon options
8. `docs/07-i18n-glossary.md` — translation glossary for domain terms

## Git — non-negotiable
- **Never commit or push without explicit permission from the user**, every time. Prepare the changes, show a summary and the proposed commit message, then wait for the go-ahead.
- **Every commit includes the related documentation, up to date.** If a change affects behavior, formulas, data model, UX or architecture, the matching doc (and `spec/test-vectors.json` for calculations) is updated in the same commit. A commit with stale docs is not done.

## Language
- **Everything that goes to GitHub is in English**: code, identifiers, comments, commit messages, README, docs, UI default strings.
- French exists only as a translation resource (i18n), never in code or repo docs.

## Design principles
- **Clean Architecture**, applied pragmatically:
  - `domain`: pure calculation and entities, no framework or I/O dependency.
  - `usecases` (application): orchestrates domain and repositories.
  - `data`: persistence, preferences, import/export.
  - `ui` / presentation: screens and view models; formatting and unit display live here.
  - Dependencies always point inward (ui → usecases → domain; data implements interfaces declared inward). The domain never depends on Android or storage: it is a **pure Kotlin module** (no Android plugin), so it can become Kotlin Multiplatform for the V2 website without a rewrite.
- **KISS — Keep It Simple, Stupid**:
  - Pick the simplest solution that meets the requirement. No speculative abstraction, no layer, interface or library "for later".
  - Clean Architecture sets the dependency direction. It does not require an interface for every class: add one only where there are two implementations or a test needs it.
  - Small files, small functions, explicit names. Readable code beats clever code.
- If the two principles conflict, choose the simpler option and note why in the PR or commit message.

## Engineering rules
- **Domain first, TDD**: implement the `domain` module (pure Kotlin) against `spec/test-vectors.json` before any UI. No screen until every vector passes. Keep the vectors file platform-neutral at the repo root.
- **The domain is pure**: no I/O, no UI, internal units only (m, mrad, mm). Unit conversion and rounding for display live in the presentation layer.
- **Never round internally**: rounding happens at display time only (NF-07).
- **Changing a formula** = update `docs/02-calculations.md` and `spec/test-vectors.json` in the same commit, and explain why.
- **No hard-coded UI strings**: everything goes through i18n (`strings.xml`, en + fr). Domain terms follow `docs/07-i18n-glossary.md`; add any new term there before using it.
- **Color is never the only signal**: green/orange/red always comes with an icon and text.
- Reference requirement IDs (F-xx, NF-xx) in commit messages and test names.
- If something isn't covered by the docs, choose the simplest option, record it in `docs/05-open-questions.md` and keep going.

## Suggested build order
1. Domain + tests (vectors + properties)
2. Quick calculation on a range + preferences + i18n + locale detection
3. Targets on the quick calculation (status, suggested settings)
4. Matches / stages + persistence
5. JSON export/import, match mode
