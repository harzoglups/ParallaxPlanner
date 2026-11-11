package com.cussou.parallax.ui.quickcalc

import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import com.cussou.parallax.format.displayToMeters
import com.cussou.parallax.format.parseDecimal

const val MAX_LABEL_LENGTH = 20

/** The fields of the target editor, as typed. */
data class TargetDraft(
    val distanceText: String = "",
    val sizeText: String = "",
    val sizeUnit: SizeUnit,
    val label: String = "",
)

/** [target] is null as long as the draft cannot be saved; a flag is set for each field typed wrong. */
data class TargetDraftCheck(
    val target: Target?,
    val distanceError: Boolean = false,
    val sizeError: Boolean = false,
)

/**
 * From the editor fields to a target (F-50): the distance is required, size and label are optional.
 *
 * [untouchedDistanceM] is the stored distance of a target being edited, passed while its distance
 * field still shows the text it opened with. The field shows a rounded value, so reading it back
 * would move the target slightly (F-25).
 */
fun checkTargetDraft(
    draft: TargetDraft,
    distanceUnit: DistanceUnit,
    untouchedDistanceM: Double? = null,
): TargetDraftCheck {
    val distance = typedDistance(draft.distanceText)
    val size = typedSize(draft.sizeText)
    val distanceError = untouchedDistanceM == null && distance is Typed.Invalid
    val sizeError = size is Typed.Invalid
    val distanceM = untouchedDistanceM ?: (distance as? Typed.Valid)?.let { displayToMeters(it.value, distanceUnit) }
    if (distanceM == null || distanceError || sizeError) {
        return TargetDraftCheck(target = null, distanceError = distanceError, sizeError = sizeError)
    }
    return TargetDraftCheck(
        target = Target(
            distanceM = distanceM,
            size = (size as? Typed.Valid)?.let { TargetSize(it.value, draft.sizeUnit) },
            label = draft.label.trim().take(MAX_LABEL_LENGTH),
        ),
    )
}

private fun typedSize(text: String): Typed {
    if (text.isBlank()) return Typed.Empty
    val value = parseDecimal(text) ?: return Typed.Invalid
    return if (value > 0.0) Typed.Valid(value) else Typed.Invalid
}
