package com.cussou.parallax.domain

/**
 * What the match view shows for a stage (F-63).
 *
 * @property settingM the setting to dial: the fixed one if the stage has one, otherwise the optimal one.
 * @property forced true when [settingM] is the fixed setting of the stage (F-56).
 * @property status the worst status among the sized targets; NEUTRAL when there is none to judge.
 * @property statusCount how many targets have that status; 0 when it is NEUTRAL.
 */
data class StageSummary(
    val settingM: Double,
    val forced: Boolean,
    val maxErrorMrad: Double,
    val status: TargetStatus,
    val statusCount: Int,
)

/**
 * Summary of [stage], or null when it has neither targets nor a complete range.
 * Targets take over the range, as on the stage screen (F-51).
 */
fun stageSummary(stage: Stage, offsetMm: Double, orangeThreshold: Double): StageSummary? {
    val forcedM = stage.forcedSettingM
    if (stage.targets.isNotEmpty()) {
        val result = if (forcedM != null) {
            evaluateSetting(stage.targets, offsetMm, orangeThreshold, forcedM)
        } else {
            optimalSettingForTargets(stage.targets, offsetMm, orangeThreshold)
        }
        val worst = listOf(TargetStatus.RED, TargetStatus.ORANGE, TargetStatus.GREEN)
            .firstOrNull { status -> result.perTarget.any { it.status == status } }
        return StageSummary(
            settingM = result.settingM,
            forced = forcedM != null,
            maxErrorMrad = result.perTarget.maxOf { it.errorMrad },
            status = worst ?: TargetStatus.NEUTRAL,
            statusCount = result.perTarget.count { it.status == worst },
        )
    }

    val first = stage.rangeMinM ?: return null
    val second = stage.rangeMaxM ?: return null
    val minM = minOf(first, second)
    val maxM = maxOf(first, second)
    val optimal = optimalSettingForRange(minM, maxM, offsetMm)
    return StageSummary(
        settingM = forcedM ?: optimal.settingM,
        forced = forcedM != null,
        maxErrorMrad = if (forcedM != null) {
            maxOf(errorMrad(offsetMm, minM, forcedM), errorMrad(offsetMm, maxM, forcedM))
        } else {
            optimal.maxErrorMrad
        },
        status = TargetStatus.NEUTRAL,
        statusCount = 0,
    )
}
