package com.cussou.parallax.ui.quickcalc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cussou.parallax.ParallaxApp
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.domain.addStage
import com.cussou.parallax.domain.replaceStage
import com.cussou.parallax.format.displayToMeters
import com.cussou.parallax.format.formatDistance
import com.cussou.parallax.format.parseDecimal
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The exact algorithm is cubic in the number of targets; 50 is its documented limit (docs/02-calculations.md §4). */
const val MAX_TARGETS = 50

data class QuickCalcUiState(
    val minText: String,
    val maxText: String,
    val currentSettingText: String,
    /** Targets of advanced mode, sorted by distance. */
    val targets: List<Target>,
    val preferences: UserPreferences,
    val outcome: QuickCalcOutcome,
    /** Name of the stage being edited; null on the quick calc itself. */
    val stageName: String? = null,
    /** Notes of the stage being edited; null on the quick calc, which has none. */
    val notes: String? = null,
)

/** The stage of a match that the screen edits, instead of the unsaved quick calc (F-62). */
class StageBinding(val matchRepository: MatchRepository, val matchId: String, val stageId: String)

/** Where "Save as stage" puts the quick calc (F-64). */
sealed interface SaveTarget {
    data class Existing(val matchId: String) : SaveTarget

    data class New(val matchName: String) : SaveTarget
}

/**
 * The quick calc, and the stage editor: a stage is just a named, saved quick calc (docs/03-ux.md).
 * With a [stage], the screen is loaded from it and every change is saved back into the match.
 */
class QuickCalcViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val locale: () -> Locale = Locale::getDefault,
    private val stage: StageBinding? = null,
    private val matchRepository: MatchRepository? = stage?.matchRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    /** The matches "Save as stage" can add to (F-64). */
    val matches: StateFlow<List<Match>> = (matchRepository?.matches ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Null until a stage is loaded; the quick calc starts empty. */
    private val input = MutableStateFlow(if (stage == null) RangeInput() else null)
    private val stageName = MutableStateFlow<String?>(null)
    private val saveMutex = Mutex()

    private val _stageMissing = MutableStateFlow(false)

    /** True when the stage to edit does not exist (any more). */
    val stageMissing: StateFlow<Boolean> = _stageMissing.asStateFlow()

    /** Null until the preferences, and the stage if any, are loaded. */
    val uiState: StateFlow<QuickCalcUiState?> =
        combine(input, preferencesRepository.preferences, stageName) { input, preferences, name ->
            if (input == null) return@combine null
            val typedUnit = input.unit ?: preferences.distanceUnit
            QuickCalcUiState(
                minText = input.minText,
                maxText = input.maxText,
                currentSettingText = input.currentSettingText,
                targets = input.targets,
                preferences = preferences,
                outcome = quickCalcOutcome(
                    input.minText,
                    input.maxText,
                    typedUnit,
                    preferences,
                    input.currentSettingText,
                    input.targets,
                ),
                stageName = name,
                notes = if (stage == null) null else input.notes,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            stage?.let { load(it) }
            // When the distance unit changes, the typed values keep their meaning: their text is converted (F-25).
            preferencesRepository.preferences.map { it.distanceUnit }.distinctUntilChanged().collect { unit ->
                input.update { it?.convertedTo(unit, locale()) }
            }
        }
    }

    private suspend fun load(binding: StageBinding) {
        val unit = preferencesRepository.preferences.first().distanceUnit
        val loaded = binding.matchRepository.get(binding.matchId)?.stages?.firstOrNull { it.id == binding.stageId }
        if (loaded == null) {
            _stageMissing.value = true
            return
        }
        stageName.value = loaded.name
        input.value = RangeInput.from(loaded, unit, locale())
    }

    fun onMinChange(text: String) = edit { it.copy(minText = text, minExactM = null) }

    fun onMaxChange(text: String) = edit { it.copy(maxText = text, maxExactM = null) }

    /** The setting to keep: compared with the optimal one on a range (F-06), imposed on targets (F-56). */
    fun onCurrentSettingChange(text: String) = edit { it.copy(currentSettingText = text, currentSettingExactM = null) }

    fun onNotesChange(text: String) = edit { it.copy(notes = text) }

    fun onTargetAdded(target: Target) = edit {
        if (it.targets.size >= MAX_TARGETS) it else it.withTargets(it.targets + target)
    }

    fun onTargetEdited(index: Int, target: Target) = edit {
        it.withTargets(it.targets.toMutableList().apply { set(index, target) })
    }

    fun onTargetDeleted(index: Int) = edit {
        it.withTargets(it.targets.filterIndexed { i, _ -> i != index })
    }

    /** Undo of a deletion (F-66): the target goes back to its place in the sorted list. */
    fun onTargetRestored(target: Target) = onTargetAdded(target)

    /** The eye position is one global preference, also changed from this screen (F-31). */
    fun onEyePositionChange(eyePosition: EyePosition) {
        viewModelScope.launch { preferencesRepository.update { it.copy(eyePosition = eyePosition) } }
    }

    /**
     * Saves what the quick calc holds as a new stage named [stageName], at the end of an existing
     * match or in a new one (F-64). The quick calc keeps its values. Blank names save nothing.
     */
    fun onSaveAsStage(stageName: String, target: SaveTarget) {
        val repository = matchRepository ?: return
        val current = input.value ?: return
        if (stageName.isBlank()) return
        viewModelScope.launch {
            val unit = current.unit ?: preferencesRepository.preferences.first().distanceUnit
            val newStage = current.applyTo(Stage(id = newId(), name = stageName.trim()), unit)
            val match = when (target) {
                is SaveTarget.Existing -> repository.get(target.matchId) ?: return@launch
                is SaveTarget.New -> Match(
                    id = newId(),
                    name = target.matchName.trim().ifEmpty { return@launch },
                )
            }
            repository.save(match.addStage(newStage))
        }
    }

    /** Applies a change made by the user and, when editing a stage, saves it. */
    private fun edit(transform: (RangeInput) -> RangeInput) {
        input.update { it?.let(transform) }
        val binding = stage ?: return
        viewModelScope.launch {
            // One save at a time, each with the latest input, so the file always ends up with the last change.
            saveMutex.withLock {
                val current = input.value ?: return@withLock
                val unit = current.unit ?: preferencesRepository.preferences.filterNotNull().first().distanceUnit
                val match = binding.matchRepository.get(binding.matchId) ?: return@withLock
                val saved = match.stages.firstOrNull { it.id == binding.stageId } ?: return@withLock
                binding.matchRepository.save(match.replaceStage(current.applyTo(saved, unit)))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ParallaxApp
                QuickCalcViewModel(app.preferencesRepository, matchRepository = app.matchRepository)
            }
        }

        fun stageFactory(matchId: String, stageId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ParallaxApp
                QuickCalcViewModel(
                    app.preferencesRepository,
                    stage = StageBinding(app.matchRepository, matchId, stageId),
                )
            }
        }
    }
}

/**
 * The values as typed, with the unit they were typed in ([unit] is null until the preferences are loaded).
 *
 * A text shows a rounded value after a unit change or when loaded from a stage; the exact value in
 * meters is then kept next to it, until the user types in that field, so saving never moves it (F-25).
 */
private data class RangeInput(
    val minText: String = "",
    val maxText: String = "",
    val currentSettingText: String = "",
    val unit: DistanceUnit? = null,
    /** Stored in internal units, so they need no conversion when a unit changes (F-25). */
    val targets: List<Target> = emptyList(),
    val notes: String = "",
    val minExactM: Double? = null,
    val maxExactM: Double? = null,
    val currentSettingExactM: Double? = null,
) {
    /** Rows are sorted by distance (docs/03-ux.md §3); the sort is stable for equal distances. */
    fun withTargets(newTargets: List<Target>) = copy(targets = newTargets.sortedBy { it.distanceM })

    fun convertedTo(newUnit: DistanceUnit, locale: Locale): RangeInput {
        val oldUnit = unit ?: return copy(unit = newUnit)
        if (oldUnit == newUnit) return this
        fun exact(text: String, exactM: Double?) = exactM ?: parseDecimal(text)?.let { displayToMeters(it, oldUnit) }
        fun text(text: String, meters: Double?) = meters?.let { formatDistance(it, newUnit, locale) } ?: text
        val minM = exact(minText, minExactM)
        val maxM = exact(maxText, maxExactM)
        val settingM = exact(currentSettingText, currentSettingExactM)
        return copy(
            minText = text(minText, minM),
            maxText = text(maxText, maxM),
            currentSettingText = text(currentSettingText, settingM),
            unit = newUnit,
            minExactM = minM,
            maxExactM = maxM,
            currentSettingExactM = settingM,
        )
    }

    /** [stage] with the values of the screen; empty or unreadable fields become missing values. */
    fun applyTo(stage: Stage, typedUnit: DistanceUnit): Stage {
        fun meters(text: String, exactM: Double?) =
            exactM ?: parseDecimal(text)?.takeIf { it > 0.0 }?.let { displayToMeters(it, typedUnit) }
        return stage.copy(
            notes = notes,
            rangeMinM = meters(minText, minExactM),
            rangeMaxM = meters(maxText, maxExactM),
            forcedSettingM = meters(currentSettingText, currentSettingExactM),
            targets = targets,
        )
    }

    companion object {
        fun from(stage: Stage, unit: DistanceUnit, locale: Locale): RangeInput {
            fun text(meters: Double?) = meters?.let { formatDistance(it, unit, locale) }.orEmpty()
            return RangeInput(
                minText = text(stage.rangeMinM),
                maxText = text(stage.rangeMaxM),
                currentSettingText = text(stage.forcedSettingM),
                unit = unit,
                targets = stage.targets.sortedBy { it.distanceM },
                notes = stage.notes,
                minExactM = stage.rangeMinM,
                maxExactM = stage.rangeMaxM,
                currentSettingExactM = stage.forcedSettingM,
            )
        }
    }
}
