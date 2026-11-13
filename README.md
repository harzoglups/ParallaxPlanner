# Parallax Planner

An Android app that tells a precision shooter where to set the riflescope parallax dial, so a whole stage can be shot without touching it.

Setting the parallax to the middle of a distance range is not the best choice. The app computes the optimal setting for a range, or for the targets of a PRS stage with their size, and shows how much error is left on each target.

## Features

- **Range**: type a minimum and a maximum distance, get the setting to dial and the largest error left, in mrad or MOA, and as a distance on the target.
- **Targets**: enter each target with its distance and size. Small targets weigh more; each target gets a green, orange or red status, always with an icon and a text. When some are off target, the app suggests the fewest settings that cover them all, and a fixed setting can be checked against them.
- **Matches**: prepare a match as an ordered list of stages, see the setting of each stage at a glance, and use **match mode** on the firing line (screen kept on, large type).
- **Sharing**: export a match as a JSON file and import it on a teammate's phone. No account, no server, no network.
- Metric or imperial units, English or French, light or dark theme, a choice of accent colors.

The model is simplified: it assumes a perfect scope and only accounts for the position of your eye behind it. The head-movement test at the range remains the reference.

## How it computes

The parallax error of a target at distance *d*, with the dial set to *s*, is `y × |1/d − 1/s|`, where *y* is the offset of the line of sight at the objective. Over a range, the best setting is the harmonic mean `2 × min × max / (min + max)`, closer to the near end than the middle. With sized targets, the setting minimizes the largest error relative to each target's size. Everything is detailed in [docs/02-calculations.md](docs/02-calculations.md), and checked by the platform-neutral test cases in [spec/test-vectors.json](spec/test-vectors.json).

## Build

Requirements: JDK 17 or newer (the one bundled with Android Studio works) and the Android SDK (compileSdk 36, minSdk 26).

```sh
cd android
./gradlew :app:assembleDebug                      # debug APK in app/build/outputs/apk/debug
./gradlew :domain:test :app:testDebugUnitTest     # unit tests, no device needed
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest          # UI tests on a connected device or emulator
```

The UI tests save and restore the app's preferences and matches, but they run against the installed app: prefer an emulator or a test device.

## Repository layout

| Path | Content |
|---|---|
| `android/domain` | The calculation, in pure Kotlin with no Android dependency |
| `android/app` | The Android app: Jetpack Compose screens, view models, storage |
| `spec/test-vectors.json` | Reference test cases, shared with a future web version |
| `docs/` | Requirements, calculations, UX, architecture and decisions |
| `tools/` | Small helper scripts |

Start with [docs/00-project-context.md](docs/00-project-context.md), then [docs/01-requirements.md](docs/01-requirements.md).

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and the [code of conduct](CODE_OF_CONDUCT.md). To report a vulnerability, see [SECURITY.md](SECURITY.md).

## License

The code is under the [Zero-Clause BSD license](LICENSE): use it for anything, no attribution required.

The bundled fonts, Space Grotesk and Inter (`android/app/src/main/res/font`), keep their own license, the SIL Open Font License 1.1, whose texts are in `android/app/src/main/assets/licenses`.
