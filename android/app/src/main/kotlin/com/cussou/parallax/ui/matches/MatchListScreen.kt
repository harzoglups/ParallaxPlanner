package com.cussou.parallax.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cussou.parallax.R
import com.cussou.parallax.domain.Match
import com.cussou.parallax.format.formatDate
import com.cussou.parallax.ui.components.currentLocale
import kotlinx.coroutines.launch

/** Test tags of the match list. Rows are tagged "matches.row.0"…, their parts ".0.name" and ".0.menu". */
object MatchListTags {
    const val ADD = "matches.add"
    const val EMPTY = "matches.empty"
    const val ROW = "matches.row"
    const val MENU_EDIT = "matches.menu.edit"
    const val MENU_DUPLICATE = "matches.menu.duplicate"
    const val MENU_DELETE = "matches.menu.delete"
    const val EDITOR_NAME = "matchEditor.name"
    const val EDITOR_DATE = "matchEditor.date"
    const val EDITOR_CLEAR_DATE = "matchEditor.clearDate"
    const val EDITOR_NOTES = "matchEditor.notes"
    const val EDITOR_SAVE = "matchEditor.save"
}

@Composable
fun MatchListRoute(
    onOpenMatch: (matchId: String) -> Unit,
    viewModel: MatchListViewModel = viewModel(factory = MatchListViewModel.Factory),
) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    MatchListScreen(
        matches = matches,
        onOpen = onOpenMatch,
        onCreate = viewModel::onMatchCreated,
        onEdit = viewModel::onMatchEdited,
        onDuplicate = viewModel::onMatchDuplicated,
        onDelete = viewModel::onMatchDeleted,
        onRestore = viewModel::onMatchRestored,
    )
}

/** The list of matches (docs/03-ux.md §4). [matches] is null while they load. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    matches: List<Match>?,
    onOpen: (matchId: String) -> Unit,
    onCreate: (name: String, date: String?, notes: String) -> Unit,
    onEdit: (match: Match, name: String, date: String?, notes: String) -> Unit,
    onDuplicate: (match: Match, name: String) -> Unit,
    onDelete: (Match) -> Unit,
    onRestore: (Match) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.match_deleted)
    val undoLabel = stringResource(R.string.undo)
    var creating by remember { mutableStateOf(false) }
    var edited by remember { mutableStateOf<Match?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.matches)) },
                actions = {
                    IconButton(onClick = { creating = true }, modifier = Modifier.testTag(MatchListTags.ADD)) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.new_match))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (matches == null) return@Scaffold
        if (matches.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.no_matches),
                        modifier = Modifier.testTag(MatchListTags.EMPTY),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.no_matches_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val copyName = stringResource(R.string.copy_name)
            matches.forEachIndexed { index, match ->
                MatchRow(
                    index = index,
                    match = match,
                    onOpen = { onOpen(match.id) },
                    onEdit = { edited = match },
                    onDuplicate = { onDuplicate(match, copyName.format(match.name)) },
                    onDelete = {
                        onDelete(match)
                        // Every deletion can be undone for a few seconds (F-66).
                        scope.launch {
                            val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) onRestore(match)
                        }
                    },
                )
            }
        }
    }

    if (creating) {
        MatchEditorDialog(original = null, onSave = onCreate, onDismiss = { creating = false })
    }
    edited?.let { match ->
        MatchEditorDialog(
            original = match,
            onSave = { name, date, notes -> onEdit(match, name, date, notes) },
            onDismiss = { edited = null },
        )
    }
}

@Composable
private fun MatchRow(
    index: Int,
    match: Match,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val stages = pluralStringResource(R.plurals.stage_count, match.stages.size, match.stages.size)
    val locale = currentLocale()
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("${MatchListTags.ROW}.$index"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 72.dp).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    match.name,
                    modifier = Modifier.testTag("${MatchListTags.ROW}.$index.name"),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    match.date?.let { stringResource(R.string.date_and_stages, formatDate(it, locale), stages) } ?: stages,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("${MatchListTags.ROW}.$index.menu"),
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_actions))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem(R.string.edit, MatchListTags.MENU_EDIT) { menuOpen = false; onEdit() }
                    MenuItem(R.string.duplicate, MatchListTags.MENU_DUPLICATE) { menuOpen = false; onDuplicate() }
                    MenuItem(R.string.delete, MatchListTags.MENU_DELETE) { menuOpen = false; onDelete() }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: Int, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick, modifier = Modifier.testTag(tag))
}
