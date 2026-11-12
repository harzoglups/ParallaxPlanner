package com.cussou.parallax.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cussou.parallax.R
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.StageSummary
import com.cussou.parallax.domain.TargetStatus
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatSetting
import com.cussou.parallax.ui.components.StatusIcon
import com.cussou.parallax.ui.components.currentLocale
import com.cussou.parallax.ui.components.distanceUnitLabel
import com.cussou.parallax.ui.components.statusColor
import com.cussou.parallax.ui.components.statusText

/** Test tags of match mode. Stages are tagged "matchMode.stage.0"…, their setting ".0.setting". */
object MatchModeTags {
    const val BACK = "matchMode.back"
    const val STAGE = "matchMode.stage"
}

/**
 * The screen used on the firing line (docs/03-ux.md §4): each stage with its setting in very
 * large type, and the screen kept on while it is shown (NF-03). Nothing to edit here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchModeScreen(state: MatchUiState, onBack: () -> Unit) {
    KeepScreenOn()
    val match = state.match ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(match.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(MatchModeTags.BACK)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            match.stages.forEachIndexed { index, stage ->
                MatchModeStage(index, stage, state.summaries.getOrNull(index), state.preferences)
                HorizontalDivider()
            }
        }
    }
}

/** Keeps the screen on for as long as this is shown, and only then (NF-03). */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun MatchModeStage(index: Int, stage: Stage, summary: StageSummary?, preferences: UserPreferences) {
    val tag = "${MatchModeTags.STAGE}.$index"
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag(tag)) {
        Text(
            stringResource(R.string.stage_number_and_name, index + 1, stage.name),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (summary == null) {
            Text(
                stringResource(R.string.stage_empty),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // The value read on the line: as large as the quick calc's hero setting.
            Text(
                stringResource(
                    R.string.value_with_unit,
                    formatSetting(summary.settingM, preferences.distanceUnit, currentLocale()),
                    distanceUnitLabel(preferences.distanceUnit),
                ),
                modifier = Modifier.testTag("$tag.setting"),
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (summary.status != TargetStatus.NEUTRAL) {
                StatusIcon(summary.status, statusText(summary.status), Modifier.size(40.dp))
                if (summary.status != TargetStatus.GREEN) {
                    Text(
                        summary.statusCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = statusColor(summary.status),
                    )
                }
            }
        }
        if (summary.forced) {
            Text(
                stringResource(R.string.forced_setting),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
