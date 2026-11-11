package com.cussou.parallax.ui.quickcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatDistance
import com.cussou.parallax.format.formatSize
import com.cussou.parallax.ui.components.SegmentedChoice
import com.cussou.parallax.ui.components.currentLocale
import com.cussou.parallax.ui.components.distanceUnitLabel
import com.cussou.parallax.ui.components.sizeUnitLabel

/**
 * Bottom sheet with the three fields of a target: distance, size and label (docs/03-ux.md §3).
 * [original] is the target being edited, or null for a new one. A new target can be added with
 * "Add and next", which keeps the sheet open to enter several in a row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TargetEditorSheet(
    original: Target?,
    preferences: UserPreferences,
    onSave: (Target) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    val openedDistanceText = remember {
        original?.let { formatDistance(it.distanceM, preferences.distanceUnit, locale) }.orEmpty()
    }
    var draft by remember {
        mutableStateOf(
            TargetDraft(
                distanceText = openedDistanceText,
                sizeText = original?.size?.let { formatSize(it.value, locale) }.orEmpty(),
                // The unit defaults to the preference; an existing size keeps its own.
                sizeUnit = original?.size?.unit ?: preferences.targetSizeUnit,
                label = original?.label.orEmpty(),
            ),
        )
    }
    val check = checkTargetDraft(
        draft,
        preferences.distanceUnit,
        untouchedDistanceM = original?.distanceM?.takeIf { draft.distanceText == openedDistanceText },
    )
    val distanceFocus = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // Inside the sheet: its content is composed after the caller, and the field must exist
        // before it can take the focus.
        LaunchedEffect(Unit) { distanceFocus.requestFocus() }
        Column(
            modifier = Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (original == null) R.string.new_target else R.string.edit_target),
                style = MaterialTheme.typography.titleLarge,
            )
            DistanceField(
                text = draft.distanceText,
                onChange = { draft = draft.copy(distanceText = it) },
                label = stringResource(R.string.distance),
                unit = distanceUnitLabel(preferences.distanceUnit),
                isError = check.distanceError,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth().focusRequester(distanceFocus).testTag(QuickCalcTags.EDITOR_DISTANCE),
            )
            SizeField(draft, check.sizeError, onChange = { draft = it })
            OutlinedTextField(
                value = draft.label,
                onValueChange = { draft = draft.copy(label = it.take(MAX_LABEL_LENGTH)) },
                modifier = Modifier.fillMaxWidth().testTag(QuickCalcTags.EDITOR_LABEL),
                label = { Text(stringResource(R.string.target_label_optional)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val target = check.target
                if (original == null) {
                    OutlinedButton(
                        onClick = {
                            target?.let(onSave)
                            // Ready for the next target, in the same size unit.
                            draft = TargetDraft(sizeUnit = draft.sizeUnit)
                            distanceFocus.requestFocus()
                        },
                        enabled = target != null,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(QuickCalcTags.EDITOR_SAVE_AND_NEXT),
                    ) {
                        Text(stringResource(R.string.add_and_next), maxLines = 1)
                    }
                }
                Button(
                    onClick = {
                        target?.let(onSave)
                        onDismiss()
                    },
                    enabled = target != null,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(QuickCalcTags.EDITOR_SAVE),
                ) {
                    Text(stringResource(if (original == null) R.string.add_target else R.string.save), maxLines = 1)
                }
            }
        }
    }
}

/** Size with its own unit selector: changing it here only changes this target. */
@Composable
private fun SizeField(draft: TargetDraft, isError: Boolean, onChange: (TargetDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = draft.sizeText,
            onValueChange = { typed ->
                onChange(draft.copy(sizeText = typed.filter { it.isDigit() || it == '.' || it == ',' }.take(MAX_INPUT_LENGTH)))
            },
            modifier = Modifier.fillMaxWidth().testTag(QuickCalcTags.EDITOR_SIZE),
            label = { Text(stringResource(R.string.target_size_optional)) },
            suffix = { Text(sizeUnitLabel(draft.sizeUnit)) },
            isError = isError,
            supportingText = {
                Text(stringResource(if (isError) R.string.target_size_invalid else R.string.target_size_hint))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        )
        SegmentedChoice(
            title = null,
            options = SizeUnit.entries.map { it to sizeUnitLabel(it) },
            selected = draft.sizeUnit,
            onSelect = { onChange(draft.copy(sizeUnit = it)) },
            tagPrefix = QuickCalcTags.EDITOR_SIZE_UNIT,
        )
    }
}
