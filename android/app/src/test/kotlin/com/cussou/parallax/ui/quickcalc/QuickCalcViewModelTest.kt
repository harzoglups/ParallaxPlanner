package com.cussou.parallax.ui.quickcalc

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCalcViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @BeforeTest
    fun setMain() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    /** Runs [block] with a view model backed by a real preferences file. */
    private fun withViewModel(
        locale: Locale = Locale.ENGLISH,
        block: suspend (QuickCalcViewModel, PreferencesRepository) -> Unit,
    ) = scope.runTest {
        val job = Job()
        val repository = PreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(dispatcher + job),
                produceFile = { File(folder.root, "test.preferences_pb") },
            ),
        )
        block(QuickCalcViewModel(repository) { locale }, repository)
        job.cancelAndJoin()
    }

    private suspend fun QuickCalcViewModel.stateWhen(condition: (QuickCalcUiState) -> Boolean) =
        uiState.filterNotNull().first(condition)

    @Test
    fun `F-04 the result follows the typed range, without any button`() = withViewModel { viewModel, _ ->
        assertNull(viewModel.stateWhen { true }.outcome.result)

        viewModel.onMinChange("100")
        viewModel.onMaxChange("200")
        val state = viewModel.stateWhen { it.outcome.result != null }
        assertEquals("100", state.minText)
        assertEquals("200", state.maxText)
        assertEquals(133.3333333333, state.outcome.result!!.settingM, 1e-9)

        viewModel.onMaxChange("")
        assertNull(viewModel.stateWhen { it.maxText.isEmpty() }.outcome.result)
    }

    @Test
    fun `F-31 the eye position chosen on the screen is the stored preference`() = withViewModel { viewModel, repository ->
        viewModel.onMinChange("100")
        viewModel.onMaxChange("200")
        val good = viewModel.stateWhen { it.outcome.result != null }
        assertEquals(EyePosition.GOOD, good.preferences.eyePosition)

        viewModel.onEyePositionChange(EyePosition.WORST_CASE)
        val worst = viewModel.stateWhen { it.preferences.eyePosition == EyePosition.WORST_CASE }
        assertEquals(0.0625, worst.outcome.result!!.maxErrorMrad, 1e-12)
        assertEquals(EyePosition.WORST_CASE, repository.preferences.first().eyePosition)
    }

    @Test
    fun `F-25 changing the distance unit converts the typed range instead of reinterpreting it`() =
        withViewModel { viewModel, repository ->
            viewModel.onMinChange("100")
            viewModel.onMaxChange("200")
            val meters = assertNotNull(viewModel.stateWhen { it.outcome.result != null }.outcome.result)

            repository.update { it.copy(distanceUnit = DistanceUnit.YD) }
            val state = viewModel.stateWhen { it.preferences.distanceUnit == DistanceUnit.YD && it.minText != "100" }
            assertEquals("109.4", state.minText)
            assertEquals("218.7", state.maxText)
            // Same range within the rounding of the fields (0.1 yd).
            assertEquals(meters.settingM, state.outcome.result!!.settingM, 0.1)
        }

    @Test
    fun `F-06 F-25 the current setting follows typing and unit changes`() = withViewModel { viewModel, repository ->
        viewModel.onMinChange("100")
        viewModel.onMaxChange("200")
        viewModel.onCurrentSettingChange("150")
        val meters = viewModel.stateWhen { it.outcome.comparison != null }
        assertEquals("150", meters.currentSettingText)
        assertEquals(150.0, meters.outcome.comparison!!.settingM)

        repository.update { it.copy(distanceUnit = DistanceUnit.YD) }
        val yards = viewModel.stateWhen { it.currentSettingText != "150" }
        assertEquals("164", yards.currentSettingText)
        assertEquals(150.0, yards.outcome.comparison!!.settingM, 0.05)
    }

    @Test
    fun `converted fields use the decimal separator of the locale`() = withViewModel(Locale.FRENCH) { viewModel, repository ->
        viewModel.onMinChange("100")
        viewModel.stateWhen { it.minText == "100" }
        repository.update { it.copy(distanceUnit = DistanceUnit.YD) }
        assertEquals("109,4", viewModel.stateWhen { it.minText != "100" }.minText)
    }
}
