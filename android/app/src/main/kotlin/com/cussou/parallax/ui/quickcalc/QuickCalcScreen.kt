package com.cussou.parallax.ui.quickcalc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cussou.parallax.R
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatAngularError
import com.cussou.parallax.format.formatDistance
import com.cussou.parallax.format.formatLinearError
import com.cussou.parallax.format.formatObjective
import com.cussou.parallax.format.formatSetting
import com.cussou.parallax.ui.components.EyePositionSelector
import com.cussou.parallax.ui.components.angleUnitLabel
import com.cussou.parallax.ui.components.currentLocale
import com.cussou.parallax.ui.components.distanceUnitLabel
import com.cussou.parallax.ui.components.linearUnitLabel
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme
import kotlinx.coroutines.launch

/** Test tags of the quick calc screen. */
object QuickCalcTags {
    const val MIN_FIELD = "quickCalc.min"
    const val MAX_FIELD = "quickCalc.max"
    const val SETTING = "quickCalc.setting"
    const val MAX_ERROR = "quickCalc.maxError"
    const val EMPTY = "quickCalc.empty"
    const val SETTINGS = "quickCalc.settings"
    const val COMPARE_TOGGLE = "quickCalc.compare"
    const val CURRENT_SETTING_FIELD = "quickCalc.currentSetting"
    const val COMPARISON = "quickCalc.comparison"
    const val TARGET_ADD = "quickCalc.addTarget"
    /** Rows are tagged "quickCalc.target.0"… in display order, their parts ".0.status" and ".0.error". */
    const val TARGET_ROW = "quickCalc.target"
    const val STATUS_SUMMARY = "quickCalc.statusSummary"
    const val OPTIMAL_SETTING = "quickCalc.optimalSetting"
    const val SUGGESTED_TOGGLE = "quickCalc.suggested"
    /** Suggested settings are tagged "quickCalc.suggestedSetting.0"… shortest first. */
    const val SUGGESTED_SETTING = "quickCalc.suggestedSetting"
    const val EDITOR_DISTANCE = "targetEditor.distance"
    const val EDITOR_SIZE = "targetEditor.size"
    const val EDITOR_SIZE_UNIT = "targetEditor.sizeUnit"
    const val EDITOR_LABEL = "targetEditor.label"
    const val EDITOR_SAVE = "targetEditor.save"
    const val EDITOR_SAVE_AND_NEXT = "targetEditor.saveAndNext"
}

@Composable
fun QuickCalcRoute(
    onOpenSettings: () -> Unit,
    showSettingsAction: Boolean = true,
    viewModel: QuickCalcViewModel = viewModel(factory = QuickCalcViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QuickCalcScreen(
        state = state,
        onMinChange = viewModel::onMinChange,
        onMaxChange = viewModel::onMaxChange,
        onCurrentSettingChange = viewModel::onCurrentSettingChange,
        onEyePositionChange = viewModel::onEyePositionChange,
        onOpenSettings = onOpenSettings,
        showSettingsAction = showSettingsAction,
        onTargetAdded = viewModel::onTargetAdded,
        onTargetEdited = viewModel::onTargetEdited,
        onTargetDeleted = viewModel::onTargetDeleted,
        onTargetRestored = viewModel::onTargetRestored,
    )
}

/**
 * Quick calc (docs/03-ux.md §2 and §3). Advanced mode adds the target list to the same screen;
 * without any target it behaves like simple mode. [state] is null while the preferences load.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCalcScreen(
    state: QuickCalcUiState?,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onCurrentSettingChange: (String) -> Unit,
    onEyePositionChange: (EyePosition) -> Unit,
    onOpenSettings: () -> Unit,
    showSettingsAction: Boolean = true,
    onTargetAdded: (Target) -> Unit = {},
    onTargetEdited: (Int, Target) -> Unit = { _, _ -> },
    onTargetDeleted: (Int) -> Unit = {},
    onTargetRestored: (Target) -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.target_deleted)
    val undoLabel = stringResource(R.string.undo)
    // The target being edited: null when the editor is closed, NEW_TARGET for a new one.
    var editedIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_calc_title)) },
                actions = {
                    if (showSettingsAction) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag(QuickCalcTags.SETTINGS)) {
                            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Leaves room for the keyboard, so the focused field can scroll above it.
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val targetsResult = state.outcome.shownTargetsResult
            if (targetsResult == null) {
                RangeFields(state, onMinChange, onMaxChange)
                ResultCard(state.outcome.result, state.preferences)
                if (state.outcome.result?.swapped == true) {
                    Text(stringResource(R.string.range_swapped), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // The range comes from the targets: the range fields are not shown (F-51).
                TargetsResultCard(
                    result = targetsResult,
                    // Shown next to a fixed setting, so the recommendation is never lost (F-56).
                    optimalSettingM = state.outcome.targetsResult?.settingM.takeIf { state.outcome.forcedResult != null },
                    suggestedSettings = state.outcome.suggestedSettings,
                    targets = state.targets,
                    preferences = state.preferences,
                )
            }
            if (state.preferences.advancedMode) {
                TargetList(
                    targets = state.targets,
                    results = targetsResult?.perTarget,
                    preferences = state.preferences,
                    onAdd = { editedIndex = NEW_TARGET },
                    onEdit = { editedIndex = it },
                    onDelete = { index ->
                        val deleted = state.targets[index]
                        onTargetDeleted(index)
                        // Every deletion can be undone for a few seconds (F-66).
                        scope.launch {
                            val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) onTargetRestored(deleted)
                        }
                    },
                )
            }
            EyePositionSelector(state.preferences.eyePosition, onEyePositionChange)
            // The same field: compared with the optimal setting of a range (F-06), or imposed on targets (F-56).
            CurrentSettingSection(state, fixesTargets = targetsResult != null, onCurrentSettingChange)
            ObjectiveReminder(state.preferences.objectiveMm, onOpenSettings)
        }
    }

    val index = editedIndex
    if (state != null && index != null) {
        TargetEditorSheet(
            original = state.targets.getOrNull(index),
            preferences = state.preferences,
            onSave = { target -> if (index == NEW_TARGET) onTargetAdded(target) else onTargetEdited(index, target) },
            onDismiss = { editedIndex = null },
        )
    }
}

private const val NEW_TARGET = -1

@Composable
private fun RangeFields(state: QuickCalcUiState, onMinChange: (String) -> Unit, onMaxChange: (String) -> Unit) {
    val unit = distanceUnitLabel(state.preferences.distanceUnit)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DistanceField(
            text = state.minText,
            onChange = onMinChange,
            label = stringResource(R.string.min_distance),
            unit = unit,
            isError = state.outcome.minError,
            imeAction = ImeAction.Next,
            modifier = Modifier.weight(1f).testTag(QuickCalcTags.MIN_FIELD),
        )
        DistanceField(
            text = state.maxText,
            onChange = onMaxChange,
            label = stringResource(R.string.max_distance),
            unit = unit,
            isError = state.outcome.maxError,
            imeAction = ImeAction.Done,
            modifier = Modifier.weight(1f).testTag(QuickCalcTags.MAX_FIELD),
        )
    }
}

@Composable
internal fun DistanceField(
    text: String,
    onChange: (String) -> Unit,
    label: String,
    unit: String,
    isError: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = { typed -> onChange(typed.filter { it.isDigit() || it == '.' || it == ',' }.take(MAX_INPUT_LENGTH)) },
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text(unit) },
        isError = isError,
        supportingText = if (isError) {
            {
                Text(
                    stringResource(
                        R.string.distance_out_of_range,
                        MIN_DISTANCE.toInt().toString(),
                        MAX_DISTANCE.toInt().toString(),
                        unit,
                    ),
                )
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
    )
}

/** The card that carries the setting. */
@Composable
internal fun ResultCardShell(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            // Set explicitly: the default would pick the dimmer secondary text color.
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/** "SET TO 133 m": the one hero value of the screen (docs/03-ux.md, visual rules). */
@Composable
internal fun HeroSetting(settingM: Double, preferences: UserPreferences, label: Int = R.string.set_to) {
    val locale = currentLocale()
    Text(stringResource(label).uppercase(locale), style = MaterialTheme.typography.labelLarge)
    Text(
        stringResource(
            R.string.value_with_unit,
            formatSetting(settingM, preferences.distanceUnit, locale),
            distanceUnitLabel(preferences.distanceUnit),
        ),
        modifier = Modifier.testTag(QuickCalcTags.SETTING),
        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.size(8.dp))
}

@Composable
internal fun MaxErrorLine(maxErrorMrad: Double, preferences: UserPreferences) {
    Text(
        stringResource(
            R.string.max_error_value,
            formatAngularError(maxErrorMrad, preferences.angleUnit, currentLocale()),
            angleUnitLabel(preferences.angleUnit),
        ),
        modifier = Modifier.testTag(QuickCalcTags.MAX_ERROR),
        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
    )
}

@Composable
private fun ResultCard(result: QuickCalcResult?, preferences: UserPreferences) {
    val locale = currentLocale()
    ResultCardShell {
        if (result == null) {
            Text(
                stringResource(R.string.quick_calc_empty),
                modifier = Modifier.testTag(QuickCalcTags.EMPTY),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            return@ResultCardShell
        }
        HeroSetting(result.settingM, preferences)
        MaxErrorLine(result.maxErrorMrad, preferences)
        Text(
            stringResource(
                R.string.linear_errors,
                formatLinearError(result.linearErrorAtMinMm, preferences.distanceUnit, locale),
                linearUnitLabel(preferences.distanceUnit),
                formatDistance(result.minM, preferences.distanceUnit, locale),
                formatLinearError(result.linearErrorAtMaxMm, preferences.distanceUnit, locale),
                formatDistance(result.maxM, preferences.distanceUnit, locale),
                distanceUnitLabel(preferences.distanceUnit),
            ),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Collapsed by default: the setting the user wants to keep.
 *
 * Over a range it is compared with the optimal one (F-06): the section shows the worst error
 * with that setting. With targets ([fixesTargets]) it is imposed (F-56): the card and the
 * target rows then show its errors, so the section itself only holds the field.
 */
@Composable
private fun CurrentSettingSection(
    state: QuickCalcUiState,
    fixesTargets: Boolean,
    onCurrentSettingChange: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Opening the section puts the cursor in its field, which also scrolls it above the keyboard.
    var focusField by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusField) {
        if (focusField) {
            focusRequester.requestFocus()
            focusField = false
        }
    }
    val preferences = state.preferences
    val locale = currentLocale()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) {
                    expanded = !expanded
                    focusField = expanded
                }
                .testTag(QuickCalcTags.COMPARE_TOGGLE),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
            )
            Text(
                stringResource(if (fixesTargets) R.string.forced_setting else R.string.compare_with_my_setting),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (!expanded) return@Column
        DistanceField(
            text = state.currentSettingText,
            onChange = onCurrentSettingChange,
            label = stringResource(if (fixesTargets) R.string.forced_setting else R.string.my_setting),
            unit = distanceUnitLabel(preferences.distanceUnit),
            isError = state.outcome.currentSettingError,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).testTag(QuickCalcTags.CURRENT_SETTING_FIELD),
        )
        val comparison = state.outcome.comparison ?: return@Column
        val result = state.outcome.result ?: return@Column
        Column(Modifier.padding(top = 8.dp).testTag(QuickCalcTags.COMPARISON)) {
            Text(
                stringResource(
                    R.string.max_error_value,
                    formatAngularError(comparison.maxErrorMrad, preferences.angleUnit, locale),
                    angleUnitLabel(preferences.angleUnit),
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
            )
            Text(
                stringResource(
                    R.string.linear_errors,
                    formatLinearError(comparison.linearErrorAtMinMm, preferences.distanceUnit, locale),
                    linearUnitLabel(preferences.distanceUnit),
                    formatDistance(result.minM, preferences.distanceUnit, locale),
                    formatLinearError(comparison.linearErrorAtMaxMm, preferences.distanceUnit, locale),
                    formatDistance(result.maxM, preferences.distanceUnit, locale),
                    distanceUnitLabel(preferences.distanceUnit),
                ),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
            )
        }
    }
}

/** A result without its objective diameter means nothing, so the assumption stays visible. Tap to change it. */
@Composable
private fun ObjectiveReminder(objectiveMm: Double, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onOpenSettings),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.objective_reminder, formatObjective(objectiveMm, currentLocale())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal const val MAX_INPUT_LENGTH = 7
internal const val TABULAR_FIGURES = "tnum"

@Preview
@Composable
private fun QuickCalcScreenPreview() {
    val preferences = UserPreferences()
    ParallaxPlannerTheme {
        QuickCalcScreen(
            state = QuickCalcUiState(
                minText = "100",
                maxText = "200",
                currentSettingText = "",
                targets = emptyList(),
                preferences = preferences,
                outcome = quickCalcOutcome("100", "200", DistanceUnit.M, preferences),
            ),
            onMinChange = {},
            onMaxChange = {},
            onCurrentSettingChange = {},
            onEyePositionChange = {},
            onOpenSettings = {},
        )
    }
}
