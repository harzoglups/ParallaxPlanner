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

/** A copy that is a stage of its own: same content, another id and name (F-61). */
fun Stage.duplicate(name: String, newId: () -> String): Stage = copy(id = newId(), name = name)

/** A copy that is a match of its own, down to its stages: nothing is shared with the original (F-60). */
fun Match.duplicate(name: String, newId: () -> String): Match =
    copy(id = newId(), name = name, stages = stages.map { it.copy(id = newId()) })

// Changes to the stages of a match (F-61). A stage is designated by its id; an unknown id changes nothing.

fun Match.addStage(stage: Stage): Match = copy(stages = stages + stage)

/** Puts [stage] at [index], or last when the index is beyond the end. */
fun Match.insertStage(index: Int, stage: Stage): Match =
    copy(stages = stages.toMutableList().apply { add(index.coerceIn(0, size), stage) })

fun Match.removeStage(stageId: String): Match = copy(stages = stages.filter { it.id != stageId })

fun Match.renameStage(stageId: String, name: String): Match =
    copy(stages = stages.map { if (it.id == stageId) it.copy(name = name) else it })

/** Replaces the stage that has the id of [stage] (F-62). */
fun Match.replaceStage(stage: Stage): Match = copy(stages = stages.map { if (it.id == stage.id) stage else it })

/** Moves a stage by [offset] positions (-1 is up, 1 is down), staying within the list. */
fun Match.moveStage(stageId: String, offset: Int): Match {
    val from = stages.indexOfFirst { it.id == stageId }
    if (from < 0) return this
    val to = (from + offset).coerceIn(0, stages.lastIndex)
    if (to == from) return this
    return copy(stages = stages.toMutableList().apply { add(to, removeAt(from)) })
}
