# Parallax Planner — Project context

App name: **Parallax Planner**. Android package (applicationId, permanent once published): **`com.cussou.parallax`**.

## Vision

An **Android** app (a website is planned for V2) that gives, for a multi-distance shooting stage (PRS, especially 22 LR), **the optimal parallax setting** for the scope and the **maximum error** that results from it. The shooter then does not need to turn the dial between targets, or knows exactly when they have to.

## The problem in two sentences

Parallax error does not depend on distance itself but on the **inverse of distance** (1/d). The optimal setting for a range is therefore not the average of the distances. Shooters often set the dial "in the middle", which is not optimal. Existing calculators (e.g. Leland-West) give the error for a given setting, but **not the optimal setting for a range, nor for a set of targets of different sizes**.

## Users

- **PRS / precision shooter** (core audience): prepares stages before a match, checks the app on the firing line.
- **Casual shooter**: just wants to know "what do I set for 100–200 m?".

Field conditions: outdoors, in full sun, wearing gloves, often under time pressure, sometimes without network.

## Scope

- **Range**: min/max range → optimal setting + max error.
- **Targets**: targets with distance **and size**, green/orange/red status per target. Preparation of **matches** made of **stages**.
- A single mode: the same screen does both. Without targets the setting comes from the range; adding a target switches to the targets, and "Clear all" goes back (Q19).
- Preferences: language, units, scope (objective diameter), eye position, orange threshold.

**Out of scope for V1**: web version (V2), user accounts, cloud sync, ballistics (drop, wind), dial calibration.

## Glossary

| Term | Definition |
|---|---|
| Parallax (error) | Apparent shift of the reticle on the target when the eye is not centered, if the target image is not formed in the reticle plane. |
| Parallax setting (s) | Distance the side-focus dial is set to. |
| Range | Min and max distance of a stage's targets. |
| Target / gong | One target of a stage: distance + optional size. |
| Stage | Set of targets engaged without changing position. |
| Match | Ordered set of stages. |
| mrad / MOA | Angular units. 1 MOA = 0.290888 mrad (true MOA). |
| D | Scope objective diameter, in mm. |
| Worst case | Eye at the edge of the exit pupil: maximum possible error. |

## Document index

| File | Content |
|---|---|
| `00-project-context.md` | This document |
| `01-requirements.md` | Functional and non-functional requirements (with IDs) |
| `02-calculations.md` | Formulas, algorithms, unit conventions: **the source of truth for calculations** |
| `03-ux.md` | Navigation, screens, wireframes, display rules |
| `04-architecture.md` | Stack, repository layout, data model, persistence, i18n, tests |
| `../spec/test-vectors.json` | Domain test cases (reusable by the web version in V2) |
| `../CLAUDE.md` | Instructions for Claude Code (at the repository root) |
| `05-open-questions.md` | Default decisions, to be confirmed |
| `06-name-and-icons.md` | Name and icon options, provisional style guide, SVG sources |
| `07-i18n-glossary.md` | Translation glossary for domain terms (mandatory for every language) |
