# Architecture

**V1 scope: Android only.** The web version is postponed to V2 (see the last section).

## Choices

| Topic | Choice | Why |
|---|---|---|
| Android | Kotlin, Jetpack Compose, Material 3. `applicationId` and root package: `com.cussou.parallax` | Current standard. The applicationId is permanent once published and deliberately brand-free |
| Domain (calculation) | **Pure Kotlin** module (`domain`), with no Android dependency at all | Clean architecture. In V2 it can become a Kotlin Multiplatform module and serve the web without a rewrite |
| Test cases | `spec/test-vectors.json`, a neutral JSON file at the repository root | NF-05. Reusable as is by the web version in V2 |
| Preferences | DataStore | Simple key-value |
| Matches | One JSON file per match in the app's private storage (`files/matches/<id>.json`), all kept in memory by `MatchRepository` | Same JSON schema as the export, so there is one format to maintain and no database. A shooter has a handful of matches of a few kilobytes each |
| i18n | `strings.xml` (values/, values-fr/) | Adding a language = adding a file (F-13) |
| In-app language | AppCompat per-app locales (`AppCompatDelegate.setApplicationLocales`), `MainActivity` is an `AppCompatActivity` | F-10 on every Android version from minSdk 26. On Android 13+ the choice also shows in the system per-app language settings (`res/xml/locales_config.xml`); a change made there is copied back to the `language` preference at startup |
| Navigation | A state in `MainActivity` switches between the quick calc and the settings | Simple mode has two screens. A navigation library can wait for the bottom bar of advanced mode |

## Repository layout

```
/docs                  ← these documents
/spec/test-vectors.json
/android               ← Gradle project
  domain/              ← calculation and entities, pure Kotlin (no Android plugin)
  app/                 ← Compose UI, view models, data (DataStore, match files, import/export)
CLAUDE.md
```

The `domain` module is a Kotlin/JVM module with no Android dependency. Its tests (JUnit) read `../spec/test-vectors.json`.

## Data model (JSON export format)

```json
{
  "schemaVersion": 1,
  "match": {
    "id": "uuid",
    "name": "Rimfire Cup #4",
    "date": "2026-12-14",
    "notes": "",
    "stages": [
      {
        "id": "uuid",
        "name": "Barricade",
        "order": 2,
        "notes": "",
        "rangeMinM": null,
        "rangeMaxM": null,
        "forcedSettingM": null,
        "targets": [
          { "label": "T1", "distanceM": 25.0,
            "size": { "value": 1.0, "unit": "mrad" } }
        ]
      }
    ]
  }
}
```

Model notes:

- Distances are stored in **meters** (double). They are converted for display.
- Size is stored **with the unit it was entered in**: a cm and a mrad do not have the same meaning (linear vs angular) and must be converted at the target's distance at calculation time. `size` can be `null`.
- `rangeMinM` and `rangeMaxM` are only used when `targets` is empty.
- `order` is the 1-based position of the stage in the match. On reading, stages are sorted by it.
- Targets have no id: their position in `targets` identifies them.
- `date` is an ISO 8601 date (`2026-12-14`) or `null`.
- Fields that may be missing from a file: `date`, `notes`, `rangeMinM`, `rangeMaxM`, `forcedSettingM`, `targets`, `label`, `size`. Unknown fields are ignored. A file is rejected when a required field is missing, a distance, size or setting is not positive, a size unit or date is unreadable, or `schemaVersion` is higher than the one the app knows.
- `schemaVersion` is used to migrate imports. The format is platform-independent, so the V2 web version can import the same files.

### Preferences

```
language: "en" | "fr"
distanceUnit: "m" | "yd"
angleUnit: "mrad" | "moa"
targetSizeUnit: "mrad" | "moa" | "cm" | "in"
objectiveMm: 50
eyePosition: "excellent" | "good" | "worstCase"   (default "good")
orangeThreshold: 0.5
advancedMode: false
theme: "system" | "light" | "dark"
```

These are the keys and values stored in DataStore. They map to the domain entity `UserPreferences`; stored values that cannot be read fall back to the defaults, and out-of-range numbers are brought back into their valid range (F-30, F-32). An extra `initialized` flag records that the first-launch detection has run: `PreferencesRepository.initializeOnFirstLaunch` stores the detected defaults once, and later launches leave the stored preferences alone (F-11).

A second flag, `onboardingDone`, records that the user has gone through the first-launch screen. That screen shows the detected defaults and edits the same preferences as the settings screen, so both share one view model and their fields.

## Domain API (indicative signatures, Kotlin)

```
effectiveOffsetMm(objectiveMm, eyePosition) -> Double   // f × D/2, f = 1/4, 1/3, 1
errorMrad(offsetMm, distanceM, settingM) -> Double
linearErrorMm(errorMrad, distanceM) -> Double
optimalSettingForRange(minM, maxM, offsetMm) -> { settingM, maxErrorMrad }
sizeToMrad(size, distanceM) -> Double
optimalSettingForTargets(targets, offsetMm, orangeThreshold)
    -> { settingM, maxRatio?, perTarget: [{ errorMrad, halfSizeMrad?, ratio?, status }] }
evaluateSetting(targets, offsetMm, orangeThreshold, settingM) -> same
minimalSettings(targets, offsetMm) -> [{ settingM, targetIndices }]
status: "green" | "orange" | "red" | "neutral"
detectDefaults(localeTags) -> { language, distanceUnit, angleUnit, targetSizeUnit }
```

`maxRatio` is the worst ratio among the targets that have a size; it is absent when none has one. `perTarget` follows the order of `targets`. `minimalSettings` returns its groups sorted from the shortest to the longest setting; `targetIndices` point into `targets`, and targets without size are in no group. The functions reject invalid input (no target, non-positive distance, size or setting): validation messages are the presentation layer's job.

All these functions are **pure** and take internal units only (m, mrad, mm). Display conversions and rounding live in the presentation layer (`format/`).

## Locale detection

- `LocaleListCompat` / `Resources.getConfiguration().locales`. Walk the list in order of preference and take the first supported language, otherwise `en`.
- The region (US, LR, MM) of the **first** locale in the list sets the default units (F-21), whatever language ends up selected. No region: metric.
- Detection happens on first launch only. After that, the preferences win.
- `detectDefaults` receives the list of locale tags in order of preference (e.g. `["fr-FR", "en-US"]`): reading the system locales happens outside the domain.

## Tests

1. **Domain**: JUnit unit tests driven by `spec/test-vectors.json`. Written **before** the code (TDD).
2. **Properties**:
   - the optimal setting always lies between min and max;
   - with equal tolerances, the advanced result equals the simple result;
   - the result does not depend on the order of the targets.
3. **UI**: a few Compose tests (min/max input → display), unit change without data loss, import/export round trip.

## Deployment

- Signed APK / AAB, then Play Store.

## V2 — web version (out of V1 scope)

To be decided when V2 starts:

- **Option A, Kotlin Multiplatform**: the `domain` module becomes KMP and is compiled for the web (Kotlin/JS or Wasm). The web UI can be Compose Multiplatform or a web framework. No calculation is rewritten.
- **Option B, rewrite the domain in TypeScript**: about 150 lines, locked down by `spec/test-vectors.json`, with a light UI (Svelte recommended, or React) and an offline PWA (`vite-plugin-pwa`).

Requirements that will then apply: NF-06 (Android/web consistency), the web part of NF-10, import of the same JSON files (F-65), and the favicon and PWA icons (`06-name-and-icons.md`).
