package com.cussou.parallax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cussou.parallax.ParallaxApp
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.UserPreferences
import com.cussou.parallax.format.formatObjective
import com.cussou.parallax.format.parseDecimal
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences,
    val objectiveText: String,
    val objectiveError: Boolean,
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val locale: () -> Locale = Locale::getDefault,
) : ViewModel() {

    /** What the user typed in the objective field; null until they edit it. */
    private val typedObjective = MutableStateFlow<String?>(null)

    /** Null until the preferences are loaded. */
    val uiState: StateFlow<SettingsUiState?> =
        combine(preferencesRepository.preferences, typedObjective) { preferences, typed ->
            SettingsUiState(
                preferences = preferences,
                objectiveText = typed ?: formatObjective(preferences.objectiveMm, locale()),
                objectiveError = typed != null && validObjectiveMm(typed) == null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onLanguageChange(language: Language) = update { it.copy(language = language) }

    fun onThemeChange(theme: ThemeMode) = update { it.copy(theme = theme) }

    fun onDistanceUnitChange(unit: DistanceUnit) = update { it.copy(distanceUnit = unit) }

    fun onAngleUnitChange(unit: AngleUnit) = update { it.copy(angleUnit = unit) }

    fun onEyePositionChange(eyePosition: EyePosition) = update { it.copy(eyePosition = eyePosition) }

    /** A valid diameter is stored at once; an invalid one is only flagged (F-30). */
    fun onObjectiveChange(text: String) {
        typedObjective.value = text
        validObjectiveMm(text)?.let { objectiveMm -> update { it.copy(objectiveMm = objectiveMm) } }
    }

    private fun update(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }

    private fun validObjectiveMm(text: String): Double? =
        parseDecimal(text)?.takeIf { it in UserPreferences.OBJECTIVE_MM_RANGE }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel((this[APPLICATION_KEY] as ParallaxApp).preferencesRepository) }
        }
    }
}
