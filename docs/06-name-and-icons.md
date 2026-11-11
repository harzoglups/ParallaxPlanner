# Name and icon — options

Status: **chosen name: Parallax Planner. Package: `com.cussou.parallax`** (Q10). **Chosen icon: the P-reticle monogram** (source `ic-p` below, Q11). The icon can be changed at any time through an app and Play Store listing update.

Visual board (Design canvas): https://claude.ai/artifact/3NwZZ1Sfx16rRun42ftykh

## Proposed names

| Name | Idea | Remark |
|---|---|---|
| **Dial Once** | "Set it once": the app's promise | Favorite |
| **Zero Shift** | The reticle no longer moves | Speaks to shooters right away |
| **ParaLock** | Short, easy to remember, good for a brand | |
| **Gong Focus** | Very PRS | Less clear outside PRS |
| **StageFocus** | Matches the "stages" feature | |
| **Harmonix** | Nod to the harmonic mean | Obscure without explanation |

### Names containing "Parallax"

Advantage: it is the word people type in Play Store search. The name is not tied to PRS, so it also speaks to hunters, F-Class, benchrest and airgun shooting (field target).

| Name | Idea | Remark |
|---|---|---|
| **Parallax Planner** | Plan your settings, especially for a match | Fits advanced mode without excluding simple mode |
| **Parallax Sweet Spot** | The ideal setting for a range | Descriptive, a bit long |
| **Parallax Pilot** | The one who guides the setting | Catchy, neutral |
| **Smart Parallax** | A "smart" calculation rather than setting in the middle | Generic, may already be taken |
| **Parallax Point** | The optimal setting point | Short, neutral |
| **Parallax Zero** | Zero error, as in "zeroing" | Shooter's wink |
| **Parallax Range** | Setting for a distance range | Ambiguous in English (range = shooting range) |

### Brand + keyword format

A short brand can also be combined with a subtitle: "**Dial Once – Parallax Calculator**", "**Zero Shift – Parallax**", "**ParaLock – Parallax Calculator**". The brand stays distinctive and "Parallax" is present for search. The length of the title shown on the Play Store is limited (30 characters, to be checked at publishing time).

### Other options (wider brainstorm)

| Family | Names |
|---|---|
| Descriptive "Parallax" | Parallax Coach, Parallax Assist, Parallax Wise, ParallaxIQ, Parallax Dial, Parallax Set |
| Promise / action | Set Once, Set & Forget, One Dial, Focus Lock |
| Shooting jargon | No Float (the "floating" reticle), Reticle Lock, Still Reticle, Side Focus, Nod Test (the head-movement test) |
| Stage / match | Stage Dial, Match Dial, Steel Focus |
| Invented names (easier to trademark) | Parallo, Paraxo, Parafix |

Rejected after a quick search: **Paralign** (existing wellness app and industrial brand), **Plaxo** and **Plaxis** (existing brands).

### Package name (applicationId) — decoupled from the name

- The **display name** on the Play Store can be changed at any time. The **applicationId**, however, is permanent from the first release.
- So we choose a **neutral** applicationId, independent of the brand. **Decision: `com.cussou.parallax`.**

To do before choosing: check availability on the Play Store, with INPI and EUIPO, and for domain names.

## Icon options

| Icon | Concept | Legibility at 32 px |
|---|---|---|
| Gong Focus | Reticle over three gongs at different distances | Medium |
| Dial Once | Side-focus dial seen from the front, a single accent mark | Good |
| Zero Shift | Shifted ghost image and realigned reticle | Good |
| StageFocus | Gongs, reticle scale, range bracket, mark shifted toward the near end | Poor: better suited to an illustration |
| Harmonix | Error curve with its targeted minimum | Poor: better suited to an illustration |
| ParaLock | P monogram whose bowl is a reticle | Very good |

### Provisional style guide

- Accent color: `#F28C28` ("gong steel" orange)
- Icon background: `#1F2630`
- Light strokes: `#E6EDF3`, secondary: `#9DA7B3`
- Fonts: Space Grotesk (headings), Inter (body)

### Implemented app theme

The app theme (`ui/theme/Theme.kt`) applies this guide to Material 3:

- **Dark theme**: background `#1F2630`, text `#E6EDF3`, secondary text `#9DA7B3`, accent `#F28C28`.
- **Light theme**: near-white background `#F7F9FB`, text `#1F2630`, and a darker orange `#9A4F00` as accent. The guide's `#F28C28` only reaches 2.3:1 on a light background, below the AA minimum of NF-02.
- The setting, the one hero value of each screen, is drawn in the accent color.

`ColorContrastTest` checks that every text / background pair used on screen reaches 4.5:1 in both themes. The fonts of the guide (Space Grotesk, Inter) are not applied yet: the app uses the system font.

### Implementation constraints

- **Android**: produce an *adaptive icon*, with the pictogram in the foreground inside the safe zone (central 66 %) and the solid `#1F2630` background in the background layer. Add a monochrome variant for themed icons (Android 13+).
- **Web/PWA** *(V2)*: SVG favicon, plus 192 and 512 PNGs and a *maskable* version for the manifest.
- Rework the drawing at 32 px and below: thicken thin strokes, remove secondary details.

### Implemented launcher icon

The Android launcher icon is built from `ic-p` as an adaptive icon in `android/app/src/main/res`:

- `drawable/ic_launcher_foreground.xml`: the monogram, scaled by 0.78 and centered so that it stays inside the safe zone;
- `values/colors.xml`: the solid `#1F2630` background;
- `drawable/ic_launcher_monochrome.xml`: the one-color variant for themed icons;
- `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`: the adaptive icon itself. minSdk 26 supports adaptive icons everywhere, so there is no PNG fallback.

One change from the source drawing: the reticle lines are 3 units wide instead of 2.2, to stay visible at small sizes. The 512 px Play Store icon is still to produce at publishing time.

## SVG sources (viewBox 100×100, background included)

### Gong Focus (`ic-gong`)

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<rect width="100" height="100" rx="22" fill="#1F2630"/>
<line x1="50" y1="12" x2="50" y2="88" stroke="#E6EDF3" stroke-width="1.8" opacity="0.5"></line>
<line x1="12" y1="50" x2="88" y2="50" stroke="#E6EDF3" stroke-width="1.8" opacity="0.5"></line>
<circle cx="29" cy="70" r="12" fill="#F28C28"></circle>
<circle cx="50" cy="50" r="8.5" fill="#F28C28"></circle>
<circle cx="69" cy="33" r="5.5" fill="#F28C28"></circle>
<circle cx="50" cy="50" r="19" fill="none" stroke="#E6EDF3" stroke-width="3"></circle>
</svg>
```

### Dial Once (`ic-knob`)

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<rect width="100" height="100" rx="22" fill="#1F2630"/>
<circle cx="50" cy="50" r="36" fill="#222A35" stroke="#E6EDF3" stroke-width="3"></circle>
<g stroke="#E6EDF3" stroke-width="2.4" stroke-linecap="round" opacity="0.7">
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(30 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(60 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(90 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(120 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(150 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(180 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(210 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(240 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(270 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(300 50 50)"></line>
<line x1="50" y1="19" x2="50" y2="25" transform="rotate(330 50 50)"></line>
</g>
<line x1="50" y1="17" x2="50" y2="32" stroke="#F28C28" stroke-width="5" stroke-linecap="round"></line>
<circle cx="50" cy="50" r="13" fill="#0D1117" stroke="#E6EDF3" stroke-width="2"></circle>
<circle cx="50" cy="50" r="3.5" fill="#F28C28"></circle>
</svg>
```

### Zero Shift (`ic-shift`)

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<rect width="100" height="100" rx="22" fill="#1F2630"/>
<circle cx="42" cy="50" r="24" fill="none" stroke="#F28C28" stroke-width="4" opacity="0.35" stroke-dasharray="5 5"></circle>
<circle cx="53" cy="50" r="24" fill="none" stroke="#F28C28" stroke-width="5"></circle>
<g stroke="#E6EDF3" stroke-width="3" stroke-linecap="round">
<line x1="53" y1="22" x2="53" y2="42"></line>
<line x1="53" y1="58" x2="53" y2="78"></line>
<line x1="25" y1="50" x2="45" y2="50"></line>
<line x1="61" y1="50" x2="81" y2="50"></line>
</g>
<circle cx="53" cy="50" r="2.5" fill="#E6EDF3"></circle>
</svg>
```

### StageFocus (`ic-stage`)

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<rect width="100" height="100" rx="22" fill="#1F2630"/>
<g stroke="#9DA7B3" stroke-width="2" stroke-linecap="round">
<line x1="28" y1="31" x2="28" y2="42"></line>
<line x1="50" y1="32" x2="50" y2="42"></line>
<line x1="72" y1="34" x2="72" y2="42"></line>
</g>
<circle cx="28" cy="24" r="7" fill="#F28C28"></circle>
<circle cx="50" cy="27" r="5.5" fill="#F28C28"></circle>
<circle cx="72" cy="30" r="4" fill="#F28C28"></circle>
<line x1="12" y1="54" x2="88" y2="54" stroke="#E6EDF3" stroke-width="3" stroke-linecap="round"></line>
<g stroke="#E6EDF3" stroke-width="2" stroke-linecap="round">
<line x1="20" y1="50" x2="20" y2="58"></line>
<line x1="28" y1="50" x2="28" y2="58"></line>
<line x1="36" y1="50" x2="36" y2="58"></line>
<line x1="64" y1="50" x2="64" y2="58"></line>
<line x1="72" y1="50" x2="72" y2="58"></line>
<line x1="80" y1="50" x2="80" y2="58"></line>
</g>
<circle cx="50" cy="54" r="5" fill="#0D1117" stroke="#E6EDF3" stroke-width="2.5"></circle>
<path d="M28 66 V76 H72 V66" fill="none" stroke="#F28C28" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"></path>
<line x1="44" y1="76" x2="44" y2="86" stroke="#F28C28" stroke-width="4" stroke-linecap="round"></line>
</svg>
```

### Harmonix (`ic-curve`)

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<rect width="100" height="100" rx="22" fill="#1F2630"/>
<g stroke="#9DA7B3" stroke-width="2" stroke-linecap="round" opacity="0.6">
<line x1="16" y1="16" x2="16" y2="84"></line>
<line x1="16" y1="84" x2="88" y2="84"></line>
</g>
<path d="M22 20 L42 70 L84 38" fill="none" stroke="#F28C28" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"></path>
<circle cx="42" cy="70" r="8" fill="none" stroke="#E6EDF3" stroke-width="2.5"></circle>
<circle cx="42" cy="70" r="2.5" fill="#E6EDF3"></circle>
</svg>
```

### ParaLock (`ic-p`)

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<rect width="100" height="100" rx="22" fill="#1F2630"/>
<rect x="24" y="18" width="11" height="66" rx="3" fill="#E6EDF3"></rect>
<circle cx="53" cy="38" r="18" fill="none" stroke="#F28C28" stroke-width="8"></circle>
<g stroke="#E6EDF3" stroke-width="2.2" stroke-linecap="round">
<line x1="53" y1="29" x2="53" y2="47"></line>
<line x1="44" y1="38" x2="62" y2="38"></line>
</g>
</svg>
```
