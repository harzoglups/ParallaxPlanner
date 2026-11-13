# Contributing to Parallax Planner

Thanks for your interest. Bug reports, ideas, translations and code are all welcome.

## Before you start

- **Open an issue first** for anything larger than a small fix, so we can agree on the approach before you spend time on it.
- Read [docs/00-project-context.md](docs/00-project-context.md) and [docs/01-requirements.md](docs/01-requirements.md). Requirements have IDs (F-xx, NF-xx) that commits and tests reference.
- Everything in the repository is in **English**: code, comments, commit messages and docs. Other languages only exist as translation resources.

## Building and testing

See the [README](README.md#build). Before opening a pull request, run:

```sh
cd android
./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug
```

and, for a change to the screens, `./gradlew :app:connectedDebugAndroidTest` on an emulator or a test device.

Every pull request also runs the CI ([.github/workflows/ci.yml](.github/workflows/ci.yml)): unit tests, lint and a debug build in one job, the UI tests on an Android emulator in another. A maintainer can run it by hand on any branch from the Actions tab.

## Rules of the code base

- **The domain is pure Kotlin** (`android/domain`): no Android, no I/O, internal units only (meters, mrad, mm). Unit conversion and rounding happen in the presentation layer, at display time only.
- **Calculations are test-first.** [docs/02-calculations.md](docs/02-calculations.md) is the source of truth. A formula change updates that doc and [spec/test-vectors.json](spec/test-vectors.json) in the same commit, and explains why.
- **Docs move with the code.** A change to behavior, UX, data model or architecture updates the matching doc in the same commit. A choice the docs do not cover goes to [docs/05-open-questions.md](docs/05-open-questions.md).
- **No hard-coded UI string.** Every text goes through `strings.xml`, in English and French. Domain terms follow [docs/07-i18n-glossary.md](docs/07-i18n-glossary.md); add a new term there first.
- **Color is never the only signal**: a green, orange or red status always comes with an icon and a text, and text reaches AA contrast in both themes.
- **Keep it simple**: the simplest solution that meets the requirement, no abstraction "for later", small files and functions with explicit names.
- Reference requirement IDs in test names and commit messages.

## Translations

To add a language, first add its column to [docs/07-i18n-glossary.md](docs/07-i18n-glossary.md), using the words shooters in that country actually use, then translate `android/app/src/main/res/values/strings.xml` into a new `values-<lang>` folder. A review by a native-speaking shooter is very welcome.

## Pull requests

- One topic per pull request, with tests.
- Describe what changes and why, and fill in the checklist of the template.
- By contributing, you agree that your contribution is released under the project's [Zero-Clause BSD license](LICENSE).

Please follow the [code of conduct](CODE_OF_CONDUCT.md).
