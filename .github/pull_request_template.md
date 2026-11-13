## What and why

<!-- What does this change, and which requirement or issue does it serve? Reference F-xx / NF-xx IDs (docs/01-requirements.md). -->

## Checklist

- [ ] Tests pass: `./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug`, and `:app:connectedDebugAndroidTest` for UI changes
- [ ] Calculations: `docs/02-calculations.md` and `spec/test-vectors.json` updated if a formula changed
- [ ] The related docs are up to date in this pull request (requirements, UX, architecture, open questions)
- [ ] No hard-coded UI string: English and French in `strings.xml`, domain terms from `docs/07-i18n-glossary.md`
- [ ] Code, comments and docs in English
