# Translation glossary (i18n)

**Mandatory** reference for any translation of the UI or of the Play Store listing, whether done by a human, Claude Code or Gemini in Play Console. The goal is that domain terms stay the ones shooters actually use in each language.

## Rules

1. Every **new domain term** shown in the UI is added here **before** it is used in the translation files.
2. To add a language:
   - first add a column to this table, following the usage of precision shooters in that country (forums, federation rules) rather than a literal translation;
   - have it reviewed by a native-speaking shooter if possible;
   - only then translate the string files.
3. Terms in the "Never translate" section stay as they are in every language.
4. The Play Store listing is published **only in the languages the app supports** (F-14).

## Never translate

`mrad`, `MOA`, `PRS`, `m`, `yd`, `cm`, `in`, `mm`, the app name.

## Terms

| Key | EN | FR | Note |
|---|---|---|---|
| parallax | Parallax | Parallaxe | |
| parallax_error | Parallax error | Erreur de parallaxe | |
| parallax_setting | Parallax setting | Réglage de parallaxe | The distance the dial is set to |
| side_focus | Side focus | Molette latérale (side focus) | The English term is common in France; keep it in parentheses |
| set_to | Set to | Régler sur | Label of the main result |
| max_error | Max error | Erreur max | |
| range | Range | Plage | Distance range (min–max), not the shooting range ("stand") |
| min_distance | Min distance | Distance min | |
| max_distance | Max distance | Distance max | |
| target | Target | Cible | Generic term |
| gong | Gong / steel | Gong | In English, "steel" is common too; the UI uses "Target" |
| target_size | Target size | Taille de la cible | Always the smallest dimension (F-50) |
| stage | Stage | Atelier | ⚠ FR: do not translate as "étape" or "scène" |
| match | Match | Match | |
| quick_calc | Quick calc | Calcul rapide | |
| advanced_mode | Advanced mode | Mode avancé | |
| match_mode | Match mode | Mode match | Firing-line screen, screen kept on |
| my_setting | My setting | Mon réglage | The setting the dial is on now, compared with the optimal one (F-06) |
| forced_setting | Fixed setting | Réglage imposé | F-56 |
| suggested_settings | Suggested settings | Réglages suggérés | F-55 |
| eye_position | Eye position | Position de l'œil | F-31 |
| eye_excellent | Excellent | Excellente | |
| eye_good | Good | Bonne | |
| eye_worst | Worst case | Pire cas | |
| orange_threshold | Orange threshold | Seuil orange | F-32 |
| status_green | On target | Dans la cible | Always with ✓ |
| status_orange | Marginal | Limite | Always with ⚠ |
| status_red | Off target | Hors cible | Always with ✗ |
| objective_diameter | Objective diameter | Diamètre de l'objectif | |
| exit_pupil | Exit pupil | Pupille de sortie | Help text only |
| reticle | Reticle | Réticule | |
| scope | Scope | Lunette | |
| head_test | Head-movement test | Test de la tête | The range check mentioned in the disclaimer (NF-11) |
