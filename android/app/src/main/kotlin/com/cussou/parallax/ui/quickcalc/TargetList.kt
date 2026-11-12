package com.cussou.parallax.ui.quickcalc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.SettingGroup
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetResult
import com.cussou.parallax.domain.TargetStatus
import com.cussou.parallax.domain.TargetsSetting
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatAngularError
import com.cussou.parallax.format.formatDistance
import com.cussou.parallax.format.formatSetting
import com.cussou.parallax.format.formatSize
import com.cussou.parallax.ui.components.angleUnitLabel
import com.cussou.parallax.ui.components.currentLocale
import com.cussou.parallax.ui.components.distanceUnitLabel
import com.cussou.parallax.ui.components.StatusIcon
import com.cussou.parallax.ui.components.sizeUnitLabel
import com.cussou.parallax.ui.components.statusColor
import com.cussou.parallax.ui.components.statusText

/** Result card when the setting comes from targets (docs/03-ux.md §3), optimal or fixed by the user. */
@Composable
internal fun TargetsResultCard(
    result: TargetsSetting,
    optimalSettingM: Double?,
    suggestedSettings: List<SettingGroup>,
    targets: List<Target>,
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
) {
    ResultCardShell(modifier, compact = true) {
        // [optimalSettingM] is set when [result] is a fixed setting rather than the optimal one.
        HeroSetting(result.settingM, preferences, if (optimalSettingM == null) R.string.set_to else R.string.forced_setting)
        if (optimalSettingM != null) {
            Text(
                stringResource(
                    R.string.optimal_setting_value,
                    stringResource(
                        R.string.value_with_unit,
                        formatSetting(optimalSettingM, preferences.distanceUnit, currentLocale()),
                        distanceUnitLabel(preferences.distanceUnit),
                    ),
                ),
                modifier = Modifier.testTag(QuickCalcTags.OPTIMAL_SETTING),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MaxErrorLine(result.perTarget.maxOf { it.errorMrad }, preferences)
        statusSummary(result)?.let { StatusSummaryLine(it) }
        if (suggestedSettings.isNotEmpty()) SuggestedSettings(suggestedSettings, targets, preferences)
    }
}

/**
 * "2 suggested settings", collapsed until tapped: the fewest settings that leave no target red,
 * shortest first, each with the targets it covers (F-55).
 */
@Composable
private fun SuggestedSettings(groups: List<SettingGroup>, targets: List<Target>, preferences: UserPreferences) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) { expanded = !expanded }
            .testTag(QuickCalcTags.SUGGESTED_TOGGLE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            pluralStringResource(R.plurals.suggested_settings, groups.size, groups.size),
            style = MaterialTheme.typography.titleSmall,
        )
        Icon(
            if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
        )
    }
    if (!expanded) return
    val separator = stringResource(R.string.list_separator)
    groups.forEachIndexed { index, group ->
        val names = group.targetIndices.map { targetName(targets[it], preferences) }.joinToString(separator)
        Text(
            stringResource(
                R.string.suggested_setting_line,
                stringResource(
                    R.string.value_with_unit,
                    formatSetting(group.settingM, preferences.distanceUnit, locale),
                    distanceUnitLabel(preferences.distanceUnit),
                ),
                names,
            ),
            modifier = Modifier.testTag("${QuickCalcTags.SUGGESTED_SETTING}.$index"),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
            textAlign = TextAlign.Center,
        )
    }
}

/** The label of a target, or its distance when it has none. */
@Composable
private fun targetName(target: Target, preferences: UserPreferences): String =
    target.label.ifEmpty {
        stringResource(
            R.string.value_with_unit,
            formatDistance(target.distanceM, preferences.distanceUnit, currentLocale()),
            distanceUnitLabel(preferences.distanceUnit),
        )
    }

/** "✗ 2 targets off target": icon, text and color together, never color alone (F-54). */
@Composable
private fun StatusSummaryLine(summary: StatusSummary) {
    val text = when (summary.status) {
        TargetStatus.RED -> pluralStringResource(R.plurals.targets_off_target, summary.count, summary.count)
        TargetStatus.ORANGE -> pluralStringResource(R.plurals.targets_marginal, summary.count, summary.count)
        else -> stringResource(R.string.targets_all_on_target)
    }
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusIcon(summary.status, contentDescription = null)
        Text(
            text,
            modifier = Modifier.testTag(QuickCalcTags.STATUS_SUMMARY),
            style = MaterialTheme.typography.titleMedium,
            color = statusColor(summary.status),
        )
    }
}

/**
 * The targets, sorted by distance: tap a row to edit it, swipe it to delete it, or clear them all.
 * [results] holds the error and status of each target, in the same order (F-53).
 * [onEdit] and [onDelete] receive the index of the row.
 */
@Composable
internal fun TargetList(
    targets: List<Target>,
    results: List<TargetResult>?,
    preferences: UserPreferences,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.targets), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            // Back to a plain range in one tap; undone from the snackbar (F-66).
            if (targets.isNotEmpty()) {
                TextButton(onClick = onClearAll, modifier = Modifier.heightIn(min = 48.dp).testTag(QuickCalcTags.TARGETS_CLEAR)) {
                    Text(stringResource(R.string.clear_targets))
                }
            }
            if (targets.size < MAX_TARGETS) {
                TextButton(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp).testTag(QuickCalcTags.TARGET_ADD)) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(stringResource(R.string.add_target), Modifier.padding(start = 4.dp))
                }
            }
        }
        if (targets.isEmpty()) {
            Text(
                stringResource(R.string.no_targets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        targets.forEachIndexed { index, target ->
            // Keyed by content too: a restored target must get a fresh, unswiped row.
            key(index, target) {
                TargetRow(
                    index = index,
                    target = target,
                    result = results?.getOrNull(index),
                    preferences = preferences,
                    onEdit = { onEdit(index) },
                    onDelete = { onDelete(index) },
                )
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetRow(
    index: Int,
    target: Target,
    result: TargetResult?,
    preferences: UserPreferences,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val locale = currentLocale()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete()
            value != SwipeToDismissBoxValue.Settled
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        // Opaque, so the delete icon only shows where the row has been swiped away.
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 8.dp)
                    .testTag("${QuickCalcTags.TARGET_ROW}.$index"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val status = result?.status ?: TargetStatus.NEUTRAL
                StatusIcon(
                    status,
                    contentDescription = statusText(status),
                    modifier = Modifier.testTag("${QuickCalcTags.TARGET_ROW}.$index.status"),
                )
                // Two lines rather than a four-column table: it stays readable with a large system font (NF-08).
                Column(Modifier.weight(1f)) {
                    if (target.label.isNotEmpty()) {
                        Text(
                            target.label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        distanceAndSize(target, preferences),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
                    )
                }
                if (result != null) {
                    Text(
                        stringResource(
                            R.string.value_with_unit,
                            formatAngularError(result.errorMrad, preferences.angleUnit, locale),
                            angleUnitLabel(preferences.angleUnit),
                        ),
                        modifier = Modifier.testTag("${QuickCalcTags.TARGET_ROW}.$index.error"),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
                        color = statusColor(status),
                    )
                }
            }
        }
    }
}

/** "25 m · 1 mrad", or the distance alone for a target without size. */
@Composable
private fun distanceAndSize(target: Target, preferences: UserPreferences): String {
    val locale = currentLocale()
    val distance = stringResource(
        R.string.value_with_unit,
        formatDistance(target.distanceM, preferences.distanceUnit, locale),
        distanceUnitLabel(preferences.distanceUnit),
    )
    // A size keeps the unit it was entered in (docs/04-architecture.md).
    val size = target.size ?: return distance
    return stringResource(
        R.string.distance_and_size,
        distance,
        stringResource(R.string.value_with_unit, formatSize(size.value, locale), sizeUnitLabel(size.unit)),
    )
}
