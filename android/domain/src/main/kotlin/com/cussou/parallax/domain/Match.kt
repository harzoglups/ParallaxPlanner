package com.cussou.parallax.domain

/**
 * A stage: targets engaged without changing position (F-62). It holds what the quick calc
 * holds: targets or, without any, a range typed by hand, plus an optional fixed setting.
 *
 * [rangeMinM] and [rangeMaxM] are only used while [targets] is empty (F-51).
 */
data class Stage(
    val id: String,
    val name: String,
    val notes: String = "",
    val rangeMinM: Double? = null,
    val rangeMaxM: Double? = null,
    val forcedSettingM: Double? = null,
    val targets: List<Target> = emptyList(),
)

/**
 * A match: an ordered list of stages (F-60, F-61).
 *
 * @property date optional day of the match, as an ISO 8601 date ("2026-12-14").
 */
data class Match(
    val id: String,
    val name: String,
    val date: String? = null,
    val notes: String = "",
    val stages: List<Stage> = emptyList(),
)
