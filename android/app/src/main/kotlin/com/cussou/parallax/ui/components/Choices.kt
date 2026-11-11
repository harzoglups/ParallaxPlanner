package com.cussou.parallax.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.EyePosition

/**
 * A titled choice between a few options, all visible at once: one tap is enough, with gloves too.
 * Each button gets the test tag "[tagPrefix].[option]". [titleAction] sits right after the title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedChoice(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
    titleAction: (@Composable () -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            titleAction?.invoke()
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("$tagPrefix.$option"),
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
}

object EyePositionTags {
    const val HELP_BUTTON = "eyePosition.help"
    const val HELP_DIALOG = "eyePosition.helpDialog"
    const val HELP_CLOSE = "eyePosition.helpClose"
}

/**
 * The eye position control, the same on the result screen and in the settings (F-31).
 * The info button next to its title explains the three levels.
 */
@Composable
fun EyePositionSelector(selected: EyePosition, onChange: (EyePosition) -> Unit, modifier: Modifier = Modifier) {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    if (showHelp) EyePositionHelpDialog(onDismiss = { showHelp = false })
    SegmentedChoice(
        title = stringResource(R.string.eye_position),
        options = listOf(
            EyePosition.EXCELLENT to stringResource(R.string.eye_excellent),
            EyePosition.GOOD to stringResource(R.string.eye_good),
            EyePosition.WORST_CASE to stringResource(R.string.eye_worst),
        ),
        selected = selected,
        onSelect = onChange,
        tagPrefix = "eyePosition",
        modifier = modifier,
        titleAction = {
            IconButton(onClick = { showHelp = true }, modifier = Modifier.testTag(EyePositionTags.HELP_BUTTON)) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.eye_position_help),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun EyePositionHelpDialog(onDismiss: () -> Unit) {
    val levels = listOf(
        R.string.eye_excellent to R.string.eye_excellent_help,
        R.string.eye_good to R.string.eye_good_help,
        R.string.eye_worst to R.string.eye_worst_help,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(EyePositionTags.HELP_DIALOG),
        title = { Text(stringResource(R.string.eye_position)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.eye_position_intro))
                levels.forEach { (name, help) ->
                    Column {
                        Text(stringResource(name), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(help))
                    }
                }
                Disclaimer()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(EyePositionTags.HELP_CLOSE)) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
