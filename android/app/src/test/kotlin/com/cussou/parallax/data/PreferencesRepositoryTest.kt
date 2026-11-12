package com.cussou.parallax.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cussou.parallax.domain.AngleUnit
import com.cussou.parallax.domain.DistanceUnit
import com.cussou.parallax.domain.EyePosition
import com.cussou.parallax.domain.Language
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.UserPreferences
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/** Preferences persisted in a real DataStore file (NF-04). */
class PreferencesRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "test.preferences_pb") }

    /** Runs [block] on a DataStore over [file], then closes it so the file can be reopened. */
    private suspend fun TestScope.withStore(block: suspend (DataStore<Preferences>) -> Unit) {
        val job = Job()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(StandardTestDispatcher(testScheduler) + job),
            produceFile = { file },
        )
        block(store)
        job.cancelAndJoin()
    }

    private val custom = UserPreferences(
        language = Language.FR,
        distanceUnit = DistanceUnit.YD,
        angleUnit = AngleUnit.MOA,
        targetSizeUnit = SizeUnit.IN,
        objectiveMm = 56.0,
        eyePosition = EyePosition.WORST_CASE,
        orangeThreshold = 0.3,
        theme = ThemeMode.DARK,
    )

    @Test
    fun `an empty store gives the static defaults`() = runTest {
        withStore { store ->
            assertEquals(UserPreferences(), PreferencesRepository(store).preferences.first())
        }
    }

    @Test
    fun `F-11 F-21 first launch stores the defaults detected from the locales`() = runTest {
        withStore { store ->
            val repository = PreferencesRepository(store)
            repository.initializeOnFirstLaunch(listOf("fr-US"))
            val preferences = repository.preferences.first()
            assertEquals(Language.FR, preferences.language)
            assertEquals(DistanceUnit.YD, preferences.distanceUnit)
            assertEquals(AngleUnit.MOA, preferences.angleUnit)
            assertEquals(SizeUnit.MRAD, preferences.targetSizeUnit)
            assertEquals(50.0, preferences.objectiveMm)
        }
    }

    @Test
    fun `F-11 detection happens on first launch only`() = runTest {
        withStore { store ->
            PreferencesRepository(store).initializeOnFirstLaunch(listOf("en-US"))
        }
        withStore { store ->
            val repository = PreferencesRepository(store)
            repository.initializeOnFirstLaunch(listOf("fr-FR"))
            val preferences = repository.preferences.first()
            assertEquals(Language.EN, preferences.language)
            assertEquals(DistanceUnit.YD, preferences.distanceUnit)
        }
    }

    @Test
    fun `F-11 a later launch never overwrites what the user chose`() = runTest {
        withStore { store ->
            val repository = PreferencesRepository(store)
            repository.initializeOnFirstLaunch(listOf("en-US"))
            repository.update { it.copy(distanceUnit = DistanceUnit.M, objectiveMm = 44.0) }
            repository.initializeOnFirstLaunch(listOf("en-US"))
            val preferences = repository.preferences.first()
            assertEquals(DistanceUnit.M, preferences.distanceUnit)
            assertEquals(44.0, preferences.objectiveMm)
            assertEquals(AngleUnit.MOA, preferences.angleUnit)
        }
    }

    @Test
    fun `NF-04 every preference survives closing and reopening the store`() = runTest {
        withStore { store -> PreferencesRepository(store).update { custom } }
        withStore { store -> assertEquals(custom, PreferencesRepository(store).preferences.first()) }
    }

    @Test
    fun `update changes only what the transform changes`() = runTest {
        withStore { store ->
            val repository = PreferencesRepository(store)
            repository.update { custom }
            repository.update { it.copy(eyePosition = EyePosition.EXCELLENT) }
            assertEquals(custom.copy(eyePosition = EyePosition.EXCELLENT), repository.preferences.first())
        }
    }

    @Test
    fun `onboarding is pending until it is completed, first-launch detection does not complete it`() = runTest {
        withStore { store ->
            val repository = PreferencesRepository(store)
            assertEquals(false, repository.onboardingDone.first())
            repository.initializeOnFirstLaunch(listOf("fr-FR"))
            assertEquals(false, repository.onboardingDone.first())
            repository.completeOnboarding()
            assertEquals(true, repository.onboardingDone.first())
        }
    }

    @Test
    fun `completed onboarding is remembered and leaves the preferences alone`() = runTest {
        withStore { store ->
            val repository = PreferencesRepository(store)
            repository.update { custom }
            repository.completeOnboarding()
        }
        withStore { store ->
            val repository = PreferencesRepository(store)
            assertEquals(true, repository.onboardingDone.first())
            assertEquals(custom, repository.preferences.first())
        }
    }

    @Test
    fun `unreadable stored values fall back to the defaults`() = runTest {
        withStore { store ->
            store.edit {
                it[stringPreferencesKey("language")] = "de"
                it[stringPreferencesKey("distanceUnit")] = "furlong"
                it[stringPreferencesKey("eyePosition")] = "??"
            }
            assertEquals(UserPreferences(), PreferencesRepository(store).preferences.first())
        }
    }

    @Test
    fun `F-30 F-32 out of range stored values are brought back into range`() = runTest {
        withStore { store ->
            store.edit {
                it[doublePreferencesKey("objectiveMm")] = 500.0
                it[doublePreferencesKey("orangeThreshold")] = 0.0
            }
            val preferences = PreferencesRepository(store).preferences.first()
            assertEquals(80.0, preferences.objectiveMm)
            assertEquals(0.1, preferences.orangeThreshold)
        }
    }
}
