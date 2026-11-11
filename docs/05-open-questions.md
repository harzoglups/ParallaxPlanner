# Open questions — provisional decisions to confirm

## To decide

| # | Topic | Default decision | To decide |
|---|---|---|---|
| — | *No open question* | | |

## Decided

| # | Topic | Decision | Rationale |
|---|---|---|---|
| Q1 | Default units in the US | **yards + MOA** (F-21 unchanged) | The user can change distance and angle independently (F-20). A US shooter who uses mrad switches to mrad with one setting. |
| Q2 | "Staying on target" | **3 states**: red if error > half-size; orange if error > threshold × half-size (default 50 %, adjustable); green otherwise (F-32, F-53) | Red = aiming at the center of the gong, with no wind, parallax alone takes the shot off the gong. Orange = the shot stays on, but parallax eats more than half of the center-to-edge margin. |
| Q12 | Target shape | **No shape choice**: the entered size is the smallest dimension (diameter, side, smaller side of a rectangle) (F-50) | Conservative in every direction, no extra field, avoids over-engineering. |
| Q3 | Eye position | **3 levels**: Excellent (1/4), Good (1/3, default), Worst case (1). Segmented control on the result screen + preferences. The "Custom (δ mm, M)" mode is removed (F-31) | Worst case raises false alarms. Having the choice on the result screen shows the effect of head position directly. |
| Q4 | V1 languages | **en + fr only** in V1 (F-13). Glossary of domain terms (`07-i18n-glossary.md`). Play Store listing only in the app's languages (F-14). de, es, it are candidates for V1.x, to be chosen from Play Store statistics | The risk is badly translated shooting jargon, not volume (~150 strings). Only en and fr can be checked in-house. |
| Q5 | Scope profiles | **One single global profile in V1** (objective diameter, F-30). Several scopes with a choice per match: to reconsider after V1 | KISS: most shooters use a single scope in a match. |
| Q6 | Dial calibration | **No calibration**, neither in V1 nor planned in the data model. The app displays a distance; 100 m is 100 m | Knowing how one's dial maps to distances is the user's job. |
| Q7 | Setting rounding | **To the nearest meter (or yard)** (NF-07 unchanged) | Consistent with Q6. A fixed 5 m step would be too coarse up close (25 → 30 m changes parallax a lot) and useless far away. |
| Q8 | Sharing matches | **JSON export/import** through the phone's share sheet (F-65). No QR code, no share link | A match is usually prepared the day before, when the match book arrives, so remotely: a QR code only helps in person. A link would require a server (NF-04). |
| Q9 | Web version | **Postponed to V2.** V1 = Android only. The domain stays a pure Kotlin module and the test cases a neutral JSON file. The choice between KMP and a TS rewrite + framework will be made when V2 starts (`04-architecture.md`) | The real target is Android, and this halves the workload. Preparing and checking a match happens on a phone. |
| Q10 | App name | **Parallax Planner**, package **`com.cussou.parallax`** | "Parallax" helps Play Store search and speaks to every discipline; "Planner" covers both the quick calc and match preparation. Neutral, brand-free package, because it is permanent. Availability to check (Play Store, INPI, EUIPO) before publishing. |
| Q11 | Icon | **P-reticle monogram** (`ic-p` in `06-name-and-icons.md`), as an Android adaptive icon | P is the initial of Parallax Planner, and it is the most legible option at small sizes. Can be changed at any time through an update. |
| Q13 | Where a reversed range (min > max) is swapped | **In the presentation layer.** The domain function `optimalSettingForRange` requires 0 < min ≤ max and rejects anything else (F-05) | The swap has to be shown to the user, so the screen must know about it anyway. The domain stays a plain formula with one precondition. |
| Q14 | Number display and input details | Displayed values round **half up**, settings have **no thousands separator**, and number fields accept **a dot or a comma** as decimal separator in every language (NF-07) | Half up is what a shooter expects from a rounded figure. A separator in "1,234 m" reads as a decimal in French. Numeric keyboards do not always offer the locale's separator. |
