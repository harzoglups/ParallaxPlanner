# Requirements

Convention: **MUST** = mandatory in V1, **SHOULD** = desired in V1, **MAY** = later.
The IDs (F-xx, NF-xx) are used as references in code, tests and PRs.

## 1. Calculation — simple mode

- **F-01** The user MUST be able to enter a **min** distance and a **max** distance.
- **F-02** The app MUST display the **optimal parallax setting** for the range (see `02-calculations.md` §3).
- **F-03** The app MUST display the **maximum error** in the chosen angular unit, together with the equivalent linear error (cm or inches) at the min distance and at the max distance.
- **F-04** The calculation MUST update live, without a "Calculate" button.
- **F-05** Validation:
  - distances between 5 and 2000 m (or the equivalent in yards);
  - if min > max, swap them automatically and say so;
  - if min = max, the setting is that distance and the error is zero.
- **F-06** The user SHOULD be able to enter a **current setting** (optional) to see the error they would get by keeping that setting.
- **F-07** The app MAY display a curve of the error over the whole range.

## 2. Preferences

### Language
- **F-10** The user MUST be able to choose the language in the preferences.
- **F-11** On first launch, the default language MUST be derived from the **device language** (system locale, no GPS geolocation).
- **F-12** If the detected language is not supported or detection fails, the default language MUST be **English**.
- **F-13** V1 languages: **English, French**. Adding a language MUST only require adding a translation file, following the glossary `07-i18n-glossary.md`.
- **F-14** The Play Store listing MUST be published only in the languages the app supports (no translated listing for an untranslated app).

### Units
- **F-20** The user MUST be able to choose independently:
  - the **distance** unit: meters / yards;
  - the **angular** unit: mrad / MOA;
  - the **target size** unit: mrad / MOA / cm / inches.
- **F-21** Default units MUST be derived from the locale **region**:
  - US, LR, MM → yards + MOA;
  - all other regions → meters + mrad.
- **F-22** If detection fails, the defaults MUST be **meters + mrad**.
- **F-23** The default target size unit MUST be **mrad**, whatever the region.
- **F-24** The linear error MUST be displayed in cm if the distance is in meters, in inches if it is in yards.
- **F-25** Changing a unit MUST NOT modify the entered data. Only the display is converted.

### Scope and assumptions
- **F-30** The user MUST be able to enter the **objective diameter** in mm (default 50 mm, valid range 20–80 mm).
- **F-31** **Eye position**, three levels (see `02-calculations.md` §2):
  - "**Excellent**": eye offset by at most 1/4 of the exit pupil radius;
  - "**Good**" (default): 1/3 of the radius;
  - "**Worst case**": eye at the edge of the exit pupil.
  The choice MUST be changeable **directly on the result screen** (3-button segmented control, see `03-ux.md`) and in the preferences. It is one single global preference, remembered. There is no input in mm and no magnification (KISS).
- **F-32** **Orange threshold** (advanced mode): fraction of the target half-size beyond which the error turns orange. Default 50 %, range 10–90 %. The red threshold is fixed: 100 % of the half-size, meaning that when aiming at the center of the gong, with no wind, parallax alone takes the shot off the target.

### Advanced mode
- **F-40** The user MUST be able to turn **advanced mode** on or off in the preferences.
- **F-41** Turning advanced mode off MUST NOT delete saved matches. They are only hidden.

## 3. Advanced mode — targets and stages

- **F-50** The user MUST be able to define a list of **targets**, each with:
  - a **distance** (required);
  - a **size** (optional) in the chosen size unit (mrad / MOA / cm / inches). The size is always the **smallest dimension** of the target: the diameter for a round gong, the side for a square, the smaller of width and height for a rectangle. There is no shape choice (KISS). The field's help text says so in one sentence;
  - a **label** (optional, e.g. "T3", "red gong").
- **F-51** A stage's range (min/max) MUST be derived automatically from its targets. A stage without targets can keep a manually entered range, as in simple mode.
- **F-52** Optimal setting:
  - if every target has a size: **weighted** calculation (`02-calculations.md` §4);
  - otherwise: simple-mode calculation over the targets' range.
- **F-53** For each target, the app MUST display the **error** in the chosen angular unit, with a status:
  - **green** if error ≤ orange threshold × target half-size;
  - **orange** if the error exceeds that threshold but stays ≤ half-size (the shot stays on the gong, but parallax uses up a large part of the margin);
  - **red** if error > half-size (aiming at the center, with no wind, parallax alone takes the shot off the gong);
  - **neutral** if the target has no size.
- **F-54** The status MUST also be shown by an icon and text (✓ / ⚠ / ✗), not by color alone (color blindness).
- **F-55** If at least one target is red, the app SHOULD suggest the **minimum number of settings** so that no target is red, with the targets covered by each setting (`02-calculations.md` §5).
- **F-56** The user SHOULD be able to **force a setting** for a stage (e.g. "I stay at 100 m") and see the resulting errors.

## 4. Matches

- **F-60** The user MUST be able to create, rename, duplicate and delete a **match** (name, optional date, notes).
- **F-61** A match MUST contain an **ordered** list of stages. The user can add, rename, reorder, duplicate and delete them.
- **F-62** Each stage holds all the information from sections 1 and 3: range or targets, sizes, optional forced setting, notes.
- **F-63** The match view MUST display a **summary** per stage: name, recommended setting, max error, overall status.
- **F-64** From the quick calc, the user SHOULD be able to **save** the input as a stage in a match, existing or new.
- **F-65** The user SHOULD be able to **export and import** a match as JSON (sharing between teammates or between devices). Export goes through the system share sheet (messaging, email…) and import accepts a received file. No server, no QR code. The format is platform-independent, so the future web version (V2) reads the same files.
- **F-66** Every deletion MUST be confirmed or undoable ("Undo" for a few seconds).

## 5. Non-functional requirements

- **NF-01** **Offline**: the app MUST work entirely without network.
- **NF-02** **Field readability**: main result very large, AA contrast minimum, dark and light themes, touch targets ≥ 48 dp.
- **NF-03** **"Keep screen on" option** while viewing a match (SHOULD).
- **NF-04** **Local persistence** only in V1. No data is sent to a server; no account, no tracking.
- **NF-05** **Accuracy**: the domain MUST pass every case in `test-vectors.json`, with a relative tolerance of 1e-6.
- **NF-06** *(V2)* **Consistency**: once the web version exists, Android and web MUST show the same displayed results for the same input.
- **NF-07** **Display rounding**:
  - setting: to the unit (m or yd);
  - angular error: 2 decimals in mrad, 1 decimal in MOA;
  - linear error: 1 decimal.
  Calculations use double precision and are never rounded internally.
- **NF-08** **Accessibility**: screen reader labels, support for the system font size.
- **NF-09** **Performance**: result displayed less than 50 ms after input.
- **NF-10** **Android**: minSdk 26 or higher. *(V2: web, last 2 versions of Chrome, Firefox and Safari, mobile first.)*
- **NF-11** **Disclaimer**: a short text MUST remind the user that the model is simplified and that the head-movement test at the range remains the reference ("About" screen and help).
