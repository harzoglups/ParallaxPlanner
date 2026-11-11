package com.cussou.parallax.ui.quickcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cussou.parallax.R
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatAngularError
import com.cussou.parallax.format.formatDistance
import com.cussou.parallax.format.formatLinearError
import com.cussou.parallax.format.formatObjective
import com.cussou.parallax.format.formatSetting
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme
import java.util.Locale

/** Test tags of the quick calc screen. */
object QuickCalcTags {
    const val MIN_FIELD = "quickCalc.min"
    const val MAX_FIELD = "quickCalc.max"
    const val SETTING = "quickCalc.setting"
    const val MAX_ERROR = "quickCalc.maxError"
    const val EMPTY = "quickCalc.empty"
}

@Composable
fun QuickCalcRoute(viewModel: QuickCalcViewModel = viewModel(factory = QuickCalcViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QuickCalcScreen(
        state = state,
        onMinChange = viewModel::onMinChange,
        onMaxChange = viewModel::onMaxChange,
        onEyePositionChange = viewModel::onEyePositionChange,
    )
}

/** Quick calc in simple mode (docs/03-ux.md §2). [state] is null while the preferences load. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCalcScreen(
    state: QuickCalcUiState?,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onEyePositionChange: (EyePosition) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.quick_calc_title)) }) },
    ) { innerPadding ->
        if (state == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            ObjectiveReminder(state.preferences.objectiveMm)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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

/** Segmented control, not a dropdown: the three levels stay visible and one tap is enough (F-31). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EyePositionSelector(selected: EyePosition, onChange: (EyePosition) -> Unit) {
    val options = listOf(
        EyePosition.EXCELLENT to R.string.eye_excellent,
        EyePosition.GOOD to R.string.eye_good,
        EyePosition.WORST_CASE to R.string.eye_worst,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.eye_position), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (position, label) ->
                SegmentedButton(
                    selected = position == selected,
                    onClick = { onChange(position) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(label), maxLines = 1)
                }
            }
        }
    }
}

/** A result without its objective diameter means nothing, so the assumption stays visible. */
@Composable
private fun ObjectiveReminder(objectiveMm: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.objective_reminder, formatObjective(objectiveMm, currentLocale())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun distanceUnitLabel(unit: DistanceUnit): String = stringResource(
    when (unit) {
        DistanceUnit.M -> R.string.unit_m
        DistanceUnit.YD -> R.string.unit_yd
    },
)

@Composable
private fun angleUnitLabel(unit: AngleUnit): String = stringResource(
    when (unit) {
        AngleUnit.MRAD -> R.string.unit_mrad
        AngleUnit.MOA -> R.string.unit_moa
    },
)

/** Linear errors are in cm with meters and in inches with yards (F-24). */
@Composable
private fun linearUnitLabel(distanceUnit: DistanceUnit): String = stringResource(
    when (distanceUnit) {
        DistanceUnit.M -> R.string.unit_cm
        DistanceUnit.YD -> R.string.unit_in
    },
)

private const val MAX_INPUT_LENGTH = 7
private const val TABULAR_FIGURES = "tnum"

@Preview
@Composable
private fun QuickCalcScreenPreview() {
    val preferences = UserPreferences()
    ParallaxPlannerTheme {
        QuickCalcScreen(
            state = QuickCalcUiState("100", "200", preferences, quickCalcOutcome("100", "200", DistanceUnit.M, preferences)),
            onMinChange = {},
            onMaxChange = {},
            onEyePositionChange = {},
        )
    }
}
