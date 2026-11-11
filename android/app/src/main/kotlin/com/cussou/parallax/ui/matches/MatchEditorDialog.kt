package com.cussou.parallax.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.Match
import com.cussou.parallax.format.formatDate
import com.cussou.parallax.format.isoDateFromUtcMillis
import com.cussou.parallax.format.utcMillisFromIsoDate
import com.cussou.parallax.ui.components.currentLocale

const val MAX_MATCH_NAME_LENGTH = 60

/**
 * Name, optional date and notes of a match (F-60). [original] is the match being edited,
 * or null for a new one. The name is required.
 */
@Composable
internal fun MatchEditorDialog(
    original: Match?,
    onSave: (name: String, date: String?, notes: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(original?.name.orEmpty()) }
    var date by remember { mutableStateOf(original?.date) }
    var notes by remember { mutableStateOf(original?.notes.orEmpty()) }
    var pickingDate by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (original == null) R.string.new_match else R.string.edit_match)) },
        text = {
            // Inside the dialog: its content is composed after the caller.
            LaunchedEffect(Unit) { nameFocus.requestFocus() }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_MATCH_NAME_LENGTH) },
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus).testTag(MatchListTags.EDITOR_NAME),
                    label = { Text(stringResource(R.string.match_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                )
                DateRow(date, onPick = { pickingDate = true }, onClear = { date = null })
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth().testTag(MatchListTags.EDITOR_NOTES),
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 2,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name, date, notes)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(MatchListTags.EDITOR_SAVE),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    if (pickingDate) {
        MatchDatePicker(date, onPicked = { date = it }, onDismiss = { pickingDate = false })
    }
}

@Composable
private fun DateRow(date: String?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(MatchListTags.EDITOR_DATE),
        ) {
            Icon(Icons.Outlined.DateRange, contentDescription = null)
            Text(
                date?.let { formatDate(it, currentLocale()) } ?: stringResource(R.string.no_date),
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
        }
        if (date != null) {
            IconButton(onClick = onClear, modifier = Modifier.testTag(MatchListTags.EDITOR_CLEAR_DATE)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.remove_date))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchDatePicker(date: String?, onPicked: (String) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = date?.let(::utcMillisFromIsoDate))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPicked(isoDateFromUtcMillis(it)) }
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state)
    }
}
