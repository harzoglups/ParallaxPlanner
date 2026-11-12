package com.cussou.parallax.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cussou.parallax.R
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.StageSummary
import com.cussou.parallax.domain.TargetStatus
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatAngularError
import com.cussou.parallax.format.formatDate
import com.cussou.parallax.format.formatSetting
import com.cussou.parallax.ui.components.StatusIcon
import com.cussou.parallax.ui.components.angleUnitLabel
import com.cussou.parallax.ui.components.currentLocale
import com.cussou.parallax.ui.components.distanceUnitLabel
import com.cussou.parallax.ui.components.statusColor
import com.cussou.parallax.ui.components.statusText
import kotlinx.coroutines.launch

/** Test tags of the match screen. Stages are tagged "match.stage.0"…, their parts ".0.name", ".0.setting", ".0.menu". */
object MatchTags {
    const val TITLE = "match.title"
    const val BACK = "match.back"
    const val EMPTY = "match.empty"
    const val ADD_STAGE = "match.addStage"
    const val STAGE = "match.stage"
    const val MENU_RENAME = "match.menu.rename"
    const val MENU_DUPLICATE = "match.menu.duplicate"
    const val MENU_UP = "match.menu.up"
    const val MENU_DOWN = "match.menu.down"
    const val MENU_DELETE = "match.menu.delete"
    const val NAME_FIELD = "stageName.field"
    const val NAME_SAVE = "stageName.save"
}

const val MAX_STAGE_NAME_LENGTH = 40

@Composable
fun MatchRoute(matchId: String, onBack: () -> Unit, onOpenStage: (stageId: String) -> Unit) {
    // Keyed by the match, so opening another match never shows the previous one.
    val viewModel: MatchViewModel = viewModel(key = "match-$matchId", factory = MatchViewModel.factory(matchId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The match was deleted (elsewhere, or by an undo that failed): there is nothing left to show.
    LaunchedEffect(state) { if (state != null && state?.match == null) onBack() }
    MatchScreen(
        state = state,
        onBack = onBack,
        onOpenStage = onOpenStage,
        onStageAdded = viewModel::onStageAdded,
        onStageRenamed = viewModel::onStageRenamed,
        onStageDuplicated = viewModel::onStageDuplicated,
        onStageMoved = viewModel::onStageMoved,
        onStageDeleted = viewModel::onStageDeleted,
        onStageRestored = viewModel::onStageRestored,
    )
}

/** One match: its ordered stages, each with its summary (docs/03-ux.md §4). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    state: MatchUiState?,
    onBack: () -> Unit,
    onOpenStage: (stageId: String) -> Unit,
    onStageAdded: (name: String) -> Unit,
    onStageRenamed: (stageId: String, name: String) -> Unit,
    onStageDuplicated: (stageId: String, name: String) -> Unit,
    onStageMoved: (stageId: String, offset: Int) -> Unit,
    onStageDeleted: (stageId: String) -> Unit,
    onStageRestored: (stage: Stage, index: Int) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.stage_deleted)
    val undoLabel = stringResource(R.string.undo)
    val copyName = stringResource(R.string.copy_name)
    var adding by remember { mutableStateOf(false) }
    var renamed by remember { mutableStateOf<Stage?>(null) }
    val match = state?.match
    val locale = currentLocale()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            match?.name.orEmpty(),
                            modifier = Modifier.testTag(MatchTags.TITLE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        match?.date?.let {
                            Text(
                                formatDate(it, locale),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(MatchTags.BACK)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state == null || match == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (match.stages.isEmpty()) {
                Text(
                    stringResource(R.string.no_stages),
                    modifier = Modifier.testTag(MatchTags.EMPTY),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            match.stages.forEachIndexed { index, stage ->
                StageRow(
                    index = index,
                    stage = stage,
                    summary = state.summaries.getOrNull(index),
                    preferences = state.preferences,
                    isFirst = index == 0,
                    isLast = index == match.stages.lastIndex,
                    onOpen = { onOpenStage(stage.id) },
                    onRename = { renamed = stage },
                    onDuplicate = { onStageDuplicated(stage.id, copyName.format(stage.name)) },
                    onMove = { offset -> onStageMoved(stage.id, offset) },
                    onDelete = {
                        onStageDeleted(stage.id)
                        // Every deletion can be undone for a few seconds (F-66).
                        scope.launch {
                            val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) onStageRestored(stage, index)
                        }
                    },
                )
            }
            OutlinedButton(
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(MatchTags.ADD_STAGE),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(stringResource(R.string.add_stage), Modifier.padding(start = 8.dp))
            }
        }
    }

    if (adding && match != null) {
        StageNameDialog(
            title = R.string.new_stage,
            // Proposed and selected, so one tap saves it and typing replaces it.
            initialName = stringResource(R.string.default_stage_name, match.stages.size + 1),
            onSave = onStageAdded,
            onDismiss = { adding = false },
        )
    }
    renamed?.let { stage ->
        StageNameDialog(
            title = R.string.rename_stage,
            initialName = stage.name,
            onSave = { name -> onStageRenamed(stage.id, name) },
            onDismiss = { renamed = null },
        )
    }
}

/** "1  Prone  133 m ✓": the stage with the setting to dial, the hero value (F-63). */
@Composable
private fun StageRow(
    index: Int,
    stage: Stage,
    summary: StageSummary?,
    preferences: UserPreferences,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: (offset: Int) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val locale = currentLocale()
    val tag = "${MatchTags.STAGE}.$index"
    Card(
        // Opens the stage, on the same screen as the quick calc (docs/03-ux.md).
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 72.dp).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                (index + 1).toString(),
                modifier = Modifier.widthIn(min = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stage.name,
                    modifier = Modifier.testTag("$tag.name"),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (summary == null) {
                        stringResource(R.string.stage_empty)
                    } else {
                        stringResource(
                            R.string.max_error_value,
                            formatAngularError(summary.maxErrorMrad, preferences.angleUnit, locale),
                            angleUnitLabel(preferences.angleUnit),
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary != null) {
                Text(
                    stringResource(
                        R.string.value_with_unit,
                        formatSetting(summary.settingM, preferences.distanceUnit, locale),
                        distanceUnitLabel(preferences.distanceUnit),
                    ),
                    modifier = Modifier.testTag("$tag.setting"),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                StageStatus(summary, tag)
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("$tag.menu")) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_actions))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    StageMenuItem(R.string.rename, MatchTags.MENU_RENAME) { menuOpen = false; onRename() }
                    StageMenuItem(R.string.duplicate, MatchTags.MENU_DUPLICATE) { menuOpen = false; onDuplicate() }
                    if (!isFirst) StageMenuItem(R.string.move_up, MatchTags.MENU_UP) { menuOpen = false; onMove(-1) }
                    if (!isLast) StageMenuItem(R.string.move_down, MatchTags.MENU_DOWN) { menuOpen = false; onMove(1) }
                    StageMenuItem(R.string.delete, MatchTags.MENU_DELETE) { menuOpen = false; onDelete() }
                }
            }
        }
    }
}

/** The status icon, with the number of targets when they are red or orange ("✗ 2"). */
@Composable
private fun StageStatus(summary: StageSummary, tag: String) {
    if (summary.status == TargetStatus.NEUTRAL) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusIcon(summary.status, statusText(summary.status), Modifier.testTag("$tag.status"))
        if (summary.status != TargetStatus.GREEN) {
            Text(
                summary.statusCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = statusColor(summary.status),
            )
        }
    }
}

@Composable
private fun StageMenuItem(label: Int, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick, modifier = Modifier.testTag(tag))
}

/** Asks for the name of a stage. The proposed name is selected, ready to be replaced. */
@Composable
private fun StageNameDialog(title: Int, initialName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue(initialName, TextRange(0, initialName.length))) }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            // Inside the dialog: its content is composed after the caller.
            LaunchedEffect(Unit) { focus.requestFocus() }
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.text.length <= MAX_STAGE_NAME_LENGTH) name = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag(MatchTags.NAME_FIELD),
                label = { Text(stringResource(R.string.match_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name.text)
                    onDismiss()
                },
                enabled = name.text.isNotBlank(),
                modifier = Modifier.testTag(MatchTags.NAME_SAVE),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
