package com.cussou.parallax.ui.quickcalc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
}

@Composable
fun QuickCalcRoute(
    onOpenSettings: () -> Unit,
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
    )
}

/** Quick calc in simple mode (docs/03-ux.md §2). [state] is null while the preferences load. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCalcScreen(
    state: QuickCalcUiState?,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onCurrentSettingChange: (String) -> Unit,
    onEyePositionChange: (EyePosition) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_calc_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag(QuickCalcTags.SETTINGS)) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
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
            RangeFields(state, onMinChange, onMaxChange)
            ResultCard(state.outcome.result, state.preferences)
            if (state.outcome.result?.swapped == true) {
                Text(stringResource(R.string.range_swapped), style = MaterialTheme.typography.bodyMedium)
            }
            EyePositionSelector(state.preferences.eyePosition, onEyePositionChange)
            CurrentSettingComparison(state, onCurrentSettingChange)
            ObjectiveReminder(state.preferences.objectiveMm, onOpenSettings)
        }
    }
}

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
private fun DistanceField(
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

@Composable
private fun ResultCard(result: QuickCalcResult?, preferences: UserPreferences) {
    val locale = currentLocale()
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
        ) {
            if (result == null) {
                Text(
                    stringResource(R.string.quick_calc_empty),
                    modifier = Modifier.testTag(QuickCalcTags.EMPTY),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                return@Column
            }
            val distanceUnit = distanceUnitLabel(preferences.distanceUnit)
            val linearUnit = linearUnitLabel(preferences.distanceUnit)
            Text(stringResource(R.string.set_to).uppercase(locale), style = MaterialTheme.typography.labelLarge)
            // The one hero value of the screen (docs/03-ux.md, visual rules).
            Text(
                stringResource(
                    R.string.value_with_unit,
                    formatSetting(result.settingM, preferences.distanceUnit, locale),
                    distanceUnit,
                ),
                modifier = Modifier.testTag(QuickCalcTags.SETTING),
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(
                    R.string.max_error_value,
                    formatAngularError(result.maxErrorMrad, preferences.angleUnit, locale),
                    angleUnitLabel(preferences.angleUnit),
                ),
                modifier = Modifier.testTag(QuickCalcTags.MAX_ERROR),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
            )
            Text(
                stringResource(
                    R.string.linear_errors,
                    formatLinearError(result.linearErrorAtMinMm, preferences.distanceUnit, locale),
                    linearUnit,
                    formatDistance(result.minM, preferences.distanceUnit, locale),
                    formatLinearError(result.linearErrorAtMaxMm, preferences.distanceUnit, locale),
                    formatDistance(result.maxM, preferences.distanceUnit, locale),
                    distanceUnit,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Collapsed by default: optional comparison with the setting the dial is on now (F-06).
 * It shows the worst error over the range with that setting, to weigh against turning the dial.
 */
@Composable
private fun CurrentSettingComparison(state: QuickCalcUiState, onCurrentSettingChange: (String) -> Unit) {
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
            Text(stringResource(R.string.compare_with_my_setting), style = MaterialTheme.typography.titleSmall)
        }
        if (!expanded) return@Column
        DistanceField(
            text = state.currentSettingText,
            onChange = onCurrentSettingChange,
            label = stringResource(R.string.my_setting),
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

private const val MAX_INPUT_LENGTH = 7
private const val TABULAR_FIGURES = "tnum"

@Preview
@Composable
private fun QuickCalcScreenPreview() {
    val preferences = UserPreferences()
    ParallaxPlannerTheme {
        QuickCalcScreen(
            state = QuickCalcUiState("100", "200", "", preferences, quickCalcOutcome("100", "200", DistanceUnit.M, preferences)),
            onMinChange = {},
            onMaxChange = {},
            onCurrentSettingChange = {},
            onEyePositionChange = {},
            onOpenSettings = {},
        )
    }
}
