package com.cussou.parallax.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.EyePosition

/**
 * A titled choice between a few options, all visible at once: one tap is enough, with gloves too.
 * Each button gets the test tag "[tagPrefix].[option]".
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
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
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

/** The eye position control, the same on the result screen and in the settings (F-31). */
@Composable
fun EyePositionSelector(selected: EyePosition, onChange: (EyePosition) -> Unit, modifier: Modifier = Modifier) {
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
    )
}
