package com.cussou.parallax.domain

// The domain works in internal units only: meters, mrad and mm (docs/02-calculations.md §1).
// These constants are the only conversion factors; conversion itself happens at input and display.

/** One yard in meters, exact. */
const val YARD_M = 0.9144

/** One true MOA in mrad (π/10800 rad × 1000). */
const val MOA_MRAD = 0.290888
