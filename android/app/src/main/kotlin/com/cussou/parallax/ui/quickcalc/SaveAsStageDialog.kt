package com.cussou.parallax.ui.quickcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.Match
import com.cussou.parallax.ui.matches.MAX_MATCH_NAME_LENGTH
import com.cussou.parallax.ui.matches.MAX_STAGE_NAME_LENGTH

/** Test tags of the dialog. Existing matches are tagged "saveAsStage.match.<id>". */
object SaveAsStageTags {
    const val STAGE_NAME = "saveAsStage.stageName"
    const val MATCH = "saveAsStage.match"
    const val NEW_MATCH = "saveAsStage.newMatch"
    const val NEW_MATCH_NAME = "saveAsStage.newMatchName"
    const val SAVE = "saveAsStage.save"
}

/**
 * Asks where to save the quick calc as a stage (F-64): an existing match, or a new one named here.
 * The stage name is proposed ("Stage N") until the user types one.
 */
@Composable
internal fun SaveAsStageDialog(
    matches: List<Match>,
    onSave: (stageName: String, target: SaveTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    // The first match of the list, or a new match when there is none.
    var selectedMatchId by remember { mutableStateOf(matches.firstOrNull()?.id) }
    var newMatchName by remember { mutableStateOf("") }
    var typedStageName by remember { mutableStateOf<String?>(null) }
    val stageCount = matches.firstOrNull { it.id == selectedMatchId }?.stages?.size ?: 0
    val stageName = typedStageName ?: stringResource(R.string.default_stage_name, stageCount + 1)
    val target = selectedMatchId?.let { SaveTarget.Existing(it) } ?: SaveTarget.New(newMatchName)
    val canSave = stageName.isNotBlank() && (target !is SaveTarget.New || target.matchName.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_as_stage)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = stageName,
                    onValueChange = { typedStageName = it.take(MAX_STAGE_NAME_LENGTH) },
                    modifier = Modifier.fillMaxWidth().testTag(SaveAsStageTags.STAGE_NAME),
                    label = { Text(stringResource(R.string.stage_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                )
                Text(stringResource(R.string.save_in), style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    matches.forEach { match ->
                        Choice(
                            label = match.name,
                            selected = match.id == selectedMatchId,
                            onSelect = { selectedMatchId = match.id },
                            tag = "${SaveAsStageTags.MATCH}.${match.id}",
                        )
                    }
                    Choice(
                        label = stringResource(R.string.new_match),
                        selected = selectedMatchId == null,
                        onSelect = { selectedMatchId = null },
                        tag = SaveAsStageTags.NEW_MATCH,
                    )
                }
                if (selectedMatchId == null) {
                    OutlinedTextField(
                        value = newMatchName,
                        onValueChange = { newMatchName = it.take(MAX_MATCH_NAME_LENGTH) },
                        modifier = Modifier.fillMaxWidth().testTag(SaveAsStageTags.NEW_MATCH_NAME),
                        label = { Text(stringResource(R.string.match_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(stageName, target)
                    onDismiss()
                },
                enabled = canSave,
                modifier = Modifier.testTag(SaveAsStageTags.SAVE),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A radio row: the whole row is the touch target (NF-02). */
@Composable
private fun Choice(label: String, selected: Boolean, onSelect: () -> Unit, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
