package com.cussou.parallax.domain

/**
 * How far the eye sits from the scope axis (F-31).
 *
 * @property fraction offset of the eye as a fraction of the exit pupil radius.
 */
enum class EyePosition(val fraction: Double) {
    EXCELLENT(1.0 / 4.0),
    GOOD(1.0 / 3.0),
    WORST_CASE(1.0),
}
