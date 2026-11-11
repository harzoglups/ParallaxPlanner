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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetsSetting
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatDistance
import com.cussou.parallax.format.formatSize
import com.cussou.parallax.ui.components.currentLocale
import com.cussou.parallax.ui.components.distanceUnitLabel
import com.cussou.parallax.ui.components.sizeUnitLabel

/** Result card when the setting comes from targets (docs/03-ux.md §3). */
@Composable
internal fun TargetsResultCard(result: TargetsSetting, preferences: UserPreferences) {
    ResultCardShell {
        HeroSetting(result.settingM, preferences)
        MaxErrorLine(result.perTarget.maxOf { it.errorMrad }, preferences)
    }
}

/**
 * The targets of advanced mode, sorted by distance: tap a row to edit it, swipe it to delete it.
 * [onEdit] and [onDelete] receive the index of the row.
 */
@Composable
internal fun TargetList(
    targets: List<Target>,
    preferences: UserPreferences,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.targets), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
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
                TargetRow(index, target, preferences, onEdit = { onEdit(index) }, onDelete = { onDelete(index) })
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
                    .testTag("${QuickCalcTags.TARGET_ROW}.$index"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    target.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        R.string.value_with_unit,
                        formatDistance(target.distanceM, preferences.distanceUnit, locale),
                        distanceUnitLabel(preferences.distanceUnit),
                    ),
                    // Fixed share of the row and right-aligned, so distances line up in a column.
                    modifier = Modifier.weight(0.7f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
                    textAlign = TextAlign.End,
                )
                val size = target.size
                Text(
                    if (size == null) {
                        stringResource(R.string.no_value)
                    } else {
                        // A size keeps the unit it was entered in (docs/04-architecture.md).
                        stringResource(R.string.value_with_unit, formatSize(size.value, locale), sizeUnitLabel(size.unit))
                    },
                    modifier = Modifier.weight(0.7f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
