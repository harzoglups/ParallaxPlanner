package com.cussou.parallax.ui.quickcalc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cussou.parallax.ParallaxApp
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.displayToMeters
import com.cussou.parallax.format.formatDistance
import com.cussou.parallax.format.parseDecimal
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
)

class QuickCalcViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val locale: () -> Locale = Locale::getDefault,
) : ViewModel() {

    private val input = MutableStateFlow(RangeInput())

    /** Null until the preferences are loaded. */
    val uiState: StateFlow<QuickCalcUiState?> =
        combine(input, preferencesRepository.preferences) { input, preferences ->
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
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // When the distance unit changes, the typed range keeps its meaning: its text is converted (F-25).
        viewModelScope.launch {
            preferencesRepository.preferences.map { it.distanceUnit }.distinctUntilChanged().collect { unit ->
                input.update { it.convertedTo(unit, locale()) }
            }
        }
    }

    fun onMinChange(text: String) = input.update { it.copy(minText = text) }

    fun onMaxChange(text: String) = input.update { it.copy(maxText = text) }

    /** The setting the dial is on now, to compare with the optimal one (F-06). */
    fun onCurrentSettingChange(text: String) = input.update { it.copy(currentSettingText = text) }

    fun onTargetAdded(target: Target) = input.update {
        if (it.targets.size >= MAX_TARGETS) it else it.withTargets(it.targets + target)
    }

    fun onTargetEdited(index: Int, target: Target) = input.update {
        it.withTargets(it.targets.toMutableList().apply { set(index, target) })
    }

    fun onTargetDeleted(index: Int) = input.update {
        it.withTargets(it.targets.filterIndexed { i, _ -> i != index })
    }

    /** Undo of a deletion (F-66): the target goes back to its place in the sorted list. */
    fun onTargetRestored(target: Target) = onTargetAdded(target)

    /** The eye position is one global preference, also changed from this screen (F-31). */
    fun onEyePositionChange(eyePosition: EyePosition) {
        viewModelScope.launch { preferencesRepository.update { it.copy(eyePosition = eyePosition) } }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { QuickCalcViewModel((this[APPLICATION_KEY] as ParallaxApp).preferencesRepository) }
        }
    }
}

/** The distances as typed, with the unit it was typed in ([unit] is null until the preferences are loaded). */
private data class RangeInput(
    val minText: String = "",
    val maxText: String = "",
    val currentSettingText: String = "",
    val unit: DistanceUnit? = null,
    /** Stored in internal units, so they need no conversion when a unit changes (F-25). */
    val targets: List<Target> = emptyList(),
) {
    /** Rows are sorted by distance (docs/03-ux.md §3); the sort is stable for equal distances. */
    fun withTargets(newTargets: List<Target>) = copy(targets = newTargets.sortedBy { it.distanceM })

    fun convertedTo(newUnit: DistanceUnit, locale: Locale): RangeInput {
        val oldUnit = unit ?: return copy(unit = newUnit)
        if (oldUnit == newUnit) return this
        fun convert(text: String): String {
            val value = parseDecimal(text) ?: return text
            return formatDistance(displayToMeters(value, oldUnit), newUnit, locale)
        }
        return copy(
            minText = convert(minText),
            maxText = convert(maxText),
            currentSettingText = convert(currentSettingText),
            unit = newUnit,
        )
    }
}
