package com.cussou.parallax.ui.quickcalc

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
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
        block(QuickCalcViewModel(repository, locale = { locale }), repository)
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

    private fun sized(distanceM: Double, sizeMrad: Double, label: String = "") =
        Target(distanceM, TargetSize(sizeMrad, SizeUnit.MRAD), label)

    @Test
    fun `F-50 targets are added, kept sorted by distance, edited and deleted`() = withViewModel { viewModel, repository ->
        repository.update { it.copy(advancedMode = true, eyePosition = EyePosition.WORST_CASE) }
        viewModel.onTargetAdded(sized(200.0, 0.5, "T3"))
        viewModel.onTargetAdded(sized(25.0, 1.0, "T1"))
        viewModel.onTargetAdded(sized(100.0, 1.0, "T2"))
        val three = viewModel.stateWhen { it.targets.size == 3 && it.outcome.targetsResult != null }
        assertEquals(listOf("T1", "T2", "T3"), three.targets.map { it.label })
        assertEquals(60.0, three.outcome.targetsResult!!.settingM, 1e-9)

        // Moving T1 beyond T2 reorders the list.
        viewModel.onTargetEdited(0, sized(150.0, 1.0, "T1"))
        val edited = viewModel.stateWhen { it.targets.firstOrNull()?.label == "T2" }
        assertEquals(listOf("T2", "T1", "T3"), edited.targets.map { it.label })

        viewModel.onTargetDeleted(1)
        assertEquals(listOf("T2", "T3"), viewModel.stateWhen { it.targets.size == 2 }.targets.map { it.label })
    }

    @Test
    fun `F-66 a deleted target can be restored`() = withViewModel { viewModel, repository ->
        repository.update { it.copy(advancedMode = true) }
        val first = sized(25.0, 1.0, "T1")
        viewModel.onTargetAdded(first)
        viewModel.onTargetAdded(sized(100.0, 1.0, "T2"))
        viewModel.stateWhen { it.targets.size == 2 }

        viewModel.onTargetDeleted(0)
        viewModel.stateWhen { it.targets.size == 1 }
        viewModel.onTargetRestored(first)
        assertEquals(listOf("T1", "T2"), viewModel.stateWhen { it.targets.size == 2 }.targets.map { it.label })
    }

    @Test
    fun `F-25 changing units never changes the stored targets`() = withViewModel { viewModel, repository ->
        repository.update { it.copy(advancedMode = true) }
        val target = Target(100.0, TargetSize(5.0, SizeUnit.CM), "T1")
        viewModel.onTargetAdded(target)
        viewModel.stateWhen { it.targets.size == 1 }

        repository.update { it.copy(distanceUnit = DistanceUnit.YD, targetSizeUnit = SizeUnit.IN) }
        val state = viewModel.stateWhen { it.preferences.distanceUnit == DistanceUnit.YD }
        assertEquals(listOf(target), state.targets)
    }

    @Test
    fun `a stage holds at most 50 targets`() = withViewModel { viewModel, repository ->
        repository.update { it.copy(advancedMode = true) }
        repeat(MAX_TARGETS + 5) { viewModel.onTargetAdded(sized(10.0 + it, 1.0)) }
        assertEquals(MAX_TARGETS, viewModel.stateWhen { it.targets.size >= MAX_TARGETS }.targets.size)
    }

    @Test
    fun `converted fields use the decimal separator of the locale`() = withViewModel(Locale.FRENCH) { viewModel, repository ->
        viewModel.onMinChange("100")
        viewModel.stateWhen { it.minText == "100" }
        repository.update { it.copy(distanceUnit = DistanceUnit.YD) }
        assertEquals("109,4", viewModel.stateWhen { it.minText != "100" }.minText)
    }
}
