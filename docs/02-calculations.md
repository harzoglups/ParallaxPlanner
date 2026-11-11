# Calculations — source of truth

Every formula change MUST be reflected here **and** in `spec/test-vectors.json`.

## 1. Internal conventions

- Distances: **meters** (double). Conversion to or from yards happens only at input and display (1 yd = 0.9144 m exactly).
- Angles: **mrad**. 1 MOA = 0.290888 mrad (true MOA, π/10800 rad × 1000).
- Objective diameter, offsets: **mm**.
- Target sizes: converted to **mrad** for the calculation, at the target's distance:
  - cm: `size_mrad = cm × 10 / d_m`
  - inches: `size_mrad = in × 25.4 / d_m`
  - MOA: `size_mrad = moa × 0.290888`
- Working variable: **x = 1/d** (in m⁻¹). The setting s is represented by **u = 1/s**.

## 2. Parallax error

```
θ (mrad) = y (mm) × | 1/d − 1/s |      (d, s in meters)
```

- `y` = offset of the line of sight at the objective: `y = f × D / 2`, where `f` is the fraction of the exit pupil radius by which the eye is offset (F-31):

  | Eye position | f |
  |---|---|
  | Excellent | 1/4 |
  | Good (default) | 1/3 |
  | Worst case | 1 |

  Rationale: an eye offset by δ = f × (exit pupil radius) gives y = δ × M = f × D/2, because exit pupil = D/M. Magnification cancels out of the calculation.
- Linear error on the target: `e (mm) = θ (mrad) × d (m)`

Model: thin lenses, small angles. It is consistent with Tappy's formula and the Leland-West calculator (see vector V3).

## 3. Optimal setting — simple mode (range)

We minimize the maximum angular error over [dmin, dmax]. Since the error is linear in u, the optimum equalizes both ends:

```
s* = 2 × dmin × dmax / (dmin + dmax)      (harmonic mean)
θmax = y × (1/dmin − 1/dmax) / 2
```

Edge case: if dmin = dmax, then s* = d and θmax = 0.

## 4. Optimal setting — advanced mode (targets with size)

For each target i:

- `x_i = 1/d_i`
- `a_i = size_mrad_i / 2`: target half-size in mrad, i.e. the error beyond which the shot leaves the gong when aiming at the center. `size` is the smallest dimension of the target (F-50). Parallax error can go in any direction depending on the eye offset: for a square or a rectangle, the small dimension is the worst case, which is deliberately conservative (along the diagonal, a square gives about 1.41 times more margin).
- `b_i = a_i / y`: tolerance expressed in m⁻¹
- usage ratio: `r_i(u) = |x_i − u| / b_i` = error / half-size

Target status (F-53), with `k` = orange threshold (default 0.5):

| Condition | Status |
|---|---|
| r ≤ k | green |
| k < r ≤ 1 | orange |
| r > 1 | red |
| no size | neutral |

The orange threshold affects **display only**, never the optimal setting calculation.

We look for `u*` that minimizes `R(u) = max_i r_i(u)` (weighted minimax, one-dimensional).

Exact algorithm, with small n (O(n³) complexity, enough for n ≤ 50):

1. Candidates: each `x_i`, plus for each pair (i, j) the equality point `u_ij = (x_i·b_j + x_j·b_i) / (b_i + b_j)`.
2. Evaluate R at each candidate and keep the minimum. On a tie, keep the smallest u (the farthest setting).
3. `s* = 1/u*`. Display the error `θ_i = y·|x_i − u*|` and each target's status from the table above.

Note: if all tolerances are equal, this gives the harmonic mean of §3.

Note: the offset `y` scales every ratio by the same factor, so `u*` does not depend on it. The candidates can be computed with `a_i` in place of `b_i`; the result is the same.

If some targets have no size: apply §3 over the range of **all** targets. Targets without size have a neutral status.

Forced setting (F-56): u = 1/s_forced, then display θ_i and the statuses.

## 5. Minimum number of settings (F-55)

Each target defines an admissible interval `[x_i − b_i, x_i + b_i]` on the u axis (r ≤ 1, the target is not red). Finding the minimum number of settings so that no target is red amounts to **stabbing** these intervals with a minimum number of points. A greedy algorithm is optimal:

1. Sort the intervals by increasing right bound.
2. Iterate: if the current interval does not contain the last placed point, open a new group whose right bound is used as the point.
3. For each group, recompute the setting with §4 restricted to the group (better margin than the right bound).
4. Present the groups sorted by setting, from shortest to longest distance, with the targets of each.

If an interval's left bound is ≤ 0 (u ≤ 0, i.e. a setting at infinity or beyond), that group's setting is clamped to u > 0. Display "∞" if the minimax falls at u ≤ 0.

## 6. Reference examples (details in `spec/test-vectors.json`)

| ID | Input | Expected result |
|---|---|---|
| V1 | Range 100–200 m, D = 50 | s* = 133.33 m; θmax = 0.0625 mrad; 6.25 mm at 100 m, 12.5 mm at 200 m |
| V2 | Range 25–100 m, D = 50 | s* = 40 m; θmax = 0.375 mrad |
| V3 | D = 40, s = 100 yd, d = 25 yd (Tappy) | 0.6562 mrad = 2.256 MOA = 15.0 mm |
| V4 | D = 50, forced setting 150 m, range 100–200 m | 0.0833 mrad at 100 m, 0.0417 mrad at 200 m (8.33 mm at both) |
| V5 | D = 50, Good position, d = 25 m, s = 133.33 m | y = 8.33 mm; θ = 0.2708 mrad |
| V5b | D = 50, Excellent position, d = 50 m, s = 100 m | y = 6.25 mm; θ = 0.0625 mrad |
| V6 | D = 50, orange threshold 50 %, targets 25 m/1 mrad, 100 m/1 mrad, 200 m/0.5 mrad | s* = 60 m; errors 0.583 / 0.167 / 0.292 mrad; statuses red / green / red |
| V6b | V6 targets, forced setting 80 m | errors 0.6875 / 0.0625 / 0.1875 mrad; statuses red / green / orange |
| V7 | 5 cm at 50 m | 1.0 mrad |
| V8 | V6 targets | 2 settings: {100 m, 200 m} → 150 m; {25 m} → 25 m |
