package com.cussou.parallax.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.UserPreferences
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private fun withViewModel(
        locale: Locale = Locale.ENGLISH,
        block: suspend (SettingsViewModel, PreferencesRepository) -> Unit,
    ) = scope.runTest {
        val job = Job()
        val repository = PreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(dispatcher + job),
                produceFile = { File(folder.root, "test.preferences_pb") },
            ),
        )
        block(SettingsViewModel(repository) { locale }, repository)
        job.cancelAndJoin()
    }

    private suspend fun SettingsViewModel.stateWhen(condition: (SettingsUiState) -> Boolean) =
        uiState.filterNotNull().first(condition)

    @Test
    fun `F-10 F-20 F-31 each choice is stored`() = withViewModel { viewModel, repository ->
        viewModel.onLanguageChange(Language.FR)
        viewModel.onThemeChange(ThemeMode.DARK)
        viewModel.onDistanceUnitChange(DistanceUnit.YD)
        viewModel.onAngleUnitChange(AngleUnit.MOA)
        viewModel.onEyePositionChange(EyePosition.EXCELLENT)

        val expected = UserPreferences(
            language = Language.FR,
            theme = ThemeMode.DARK,
            distanceUnit = DistanceUnit.YD,
            angleUnit = AngleUnit.MOA,
            eyePosition = EyePosition.EXCELLENT,
        )
        assertEquals(expected, viewModel.stateWhen { it.preferences == expected }.preferences)
        assertEquals(expected, repository.preferences.first())
    }

    @Test
    fun `finishing the onboarding is stored`() = withViewModel { viewModel, repository ->
        assertFalse(repository.onboardingDone.first())
        viewModel.onOnboardingCompleted()
        assertTrue(repository.onboardingDone.first { it })
    }

    @Test
    fun `F-40 advanced mode is off by default and can be turned on and off`() = withViewModel { viewModel, repository ->
        assertFalse(viewModel.stateWhen { true }.preferences.advancedMode)
        viewModel.onAdvancedModeChange(true)
        assertTrue(viewModel.stateWhen { it.preferences.advancedMode }.preferences.advancedMode)
        assertTrue(repository.preferences.first().advancedMode)
        viewModel.onAdvancedModeChange(false)
        assertFalse(viewModel.stateWhen { !it.preferences.advancedMode }.preferences.advancedMode)
    }

    @Test
    fun `F-20 F-23 the target size unit is mrad by default and independent of the other units`() =
        withViewModel { viewModel, repository ->
            assertEquals(SizeUnit.MRAD, viewModel.stateWhen { true }.preferences.targetSizeUnit)
            viewModel.onTargetSizeUnitChange(SizeUnit.CM)
            val state = viewModel.stateWhen { it.preferences.targetSizeUnit == SizeUnit.CM }
            assertEquals(DistanceUnit.M, state.preferences.distanceUnit)
            assertEquals(AngleUnit.MRAD, state.preferences.angleUnit)
            assertEquals(SizeUnit.CM, repository.preferences.first().targetSizeUnit)
        }

    @Test
    fun `F-32 the orange threshold is 50 percent by default and stays within 10-90 percent`() =
        withViewModel { viewModel, repository ->
            assertEquals(0.5, viewModel.stateWhen { true }.preferences.orangeThreshold)
            viewModel.onOrangeThresholdChange(0.3)
            assertEquals(0.3, viewModel.stateWhen { it.preferences.orangeThreshold == 0.3 }.preferences.orangeThreshold)
            viewModel.onOrangeThresholdChange(0.95)
            assertEquals(0.9, viewModel.stateWhen { it.preferences.orangeThreshold == 0.9 }.preferences.orangeThreshold)
            viewModel.onOrangeThresholdChange(0.0)
            assertEquals(0.1, viewModel.stateWhen { it.preferences.orangeThreshold == 0.1 }.preferences.orangeThreshold)
            assertEquals(0.1, repository.preferences.first().orangeThreshold)
        }

    @Test
    fun `F-20 distance and angle units are independent`() = withViewModel { viewModel, _ ->
        viewModel.onDistanceUnitChange(DistanceUnit.YD)
        val state = viewModel.stateWhen { it.preferences.distanceUnit == DistanceUnit.YD }
        assertEquals(AngleUnit.MRAD, state.preferences.angleUnit)
    }

    @Test
    fun `F-30 the objective field shows the stored diameter`() = withViewModel(Locale.FRENCH) { viewModel, repository ->
        repository.update { it.copy(objectiveMm = 44.5) }
        val state = viewModel.stateWhen { it.preferences.objectiveMm == 44.5 }
        assertEquals("44,5", state.objectiveText)
        assertFalse(state.objectiveError)
    }

    @Test
    fun `F-30 a valid objective diameter is stored as it is typed`() = withViewModel { viewModel, repository ->
        viewModel.onObjectiveChange("56")
        val state = viewModel.stateWhen { it.preferences.objectiveMm == 56.0 }
        assertEquals("56", state.objectiveText)
        assertFalse(state.objectiveError)
        assertEquals(56.0, repository.preferences.first().objectiveMm)
    }

    @Test
    fun `F-30 an objective diameter outside 20-80 mm is flagged and not stored`() = withViewModel { viewModel, repository ->
        for (text in listOf("5", "19.9", "80.1", "abc", "")) {
            viewModel.onObjectiveChange(text)
            val state = viewModel.stateWhen { it.objectiveText == text }
            assertTrue(state.objectiveError, "\"$text\"")
            assertEquals(50.0, repository.preferences.first().objectiveMm, "\"$text\"")
        }
        viewModel.onObjectiveChange("20")
        assertFalse(viewModel.stateWhen { it.objectiveText == "20" }.objectiveError)
        viewModel.onObjectiveChange("80")
        assertEquals(80.0, viewModel.stateWhen { it.preferences.objectiveMm == 80.0 }.preferences.objectiveMm)
    }
}
