package com.cussou.parallax.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cussou.parallax.ParallaxApp
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.StageSummary
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.domain.addStage
import com.cussou.parallax.domain.duplicate
import com.cussou.parallax.domain.effectiveOffsetMm
import com.cussou.parallax.domain.insertStage
import com.cussou.parallax.domain.moveStage
import com.cussou.parallax.domain.removeStage
import com.cussou.parallax.domain.renameStage
import com.cussou.parallax.domain.stageSummary
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @property match null when the match does not exist (any more).
 * @property summaries one entry per stage, in order; null for a stage with nothing to compute yet.
 */
data class MatchUiState(
    val match: Match?,
    val summaries: List<StageSummary?>,
    val preferences: UserPreferences,
)

/** One match and its ordered stages (F-61, F-63). */
class MatchViewModel(
    private val matchId: String,
    private val matchRepository: MatchRepository,
    preferencesRepository: PreferencesRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    /** Null until the match and the preferences are loaded. */
    val uiState: StateFlow<MatchUiState?> =
        combine(matchRepository.matches, preferencesRepository.preferences) { matches, preferences ->
            val match = matches.firstOrNull { it.id == matchId }
            val offsetMm = effectiveOffsetMm(preferences.objectiveMm, preferences.eyePosition)
            MatchUiState(
                match = match,
                summaries = match?.stages.orEmpty().map { stageSummary(it, offsetMm, preferences.orangeThreshold) },
                preferences = preferences,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onStageAdded(name: String) = change { it.addStage(Stage(id = newId(), name = name.trim())) }

    fun onStageRenamed(stageId: String, name: String) = change { it.renameStage(stageId, name.trim()) }

    /** The copy goes right after the original. [name] comes from the screen because it is translated. */
    fun onStageDuplicated(stageId: String, name: String) = change { match ->
        val index = match.stages.indexOfFirst { it.id == stageId }
        if (index < 0) match else match.insertStage(index + 1, match.stages[index].duplicate(name, newId))
    }

    /** [offset] is -1 to move the stage up, 1 to move it down. */
    fun onStageMoved(stageId: String, offset: Int) = change { it.moveStage(stageId, offset) }

    fun onStageDeleted(stageId: String) = change { it.removeStage(stageId) }

    /** Undo of a deletion (F-66): the stage goes back to the position it had. */
    fun onStageRestored(stage: Stage, index: Int) = change { it.insertStage(index, stage) }

    private fun change(transform: (Match) -> Match) {
        viewModelScope.launch {
            val match = matchRepository.get(matchId) ?: return@launch
            matchRepository.save(transform(match))
        }
    }

    companion object {
        fun factory(matchId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ParallaxApp
                MatchViewModel(matchId, app.matchRepository, app.preferencesRepository)
            }
        }
    }
}
