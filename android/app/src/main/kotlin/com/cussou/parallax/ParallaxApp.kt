package com.cussou.parallax

import android.app.Application
import android.content.Context
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.data.PreferencesRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "preferences")

/** Holds the app-wide objects. Manual wiring: the app is too small to need a DI library. */
class ParallaxApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(preferencesDataStore) }

    val matchRepository: MatchRepository by lazy { MatchRepository(File(filesDir, "matches")) }

    override fun onCreate() {
        super.onCreate()
        // The system locales are read here, outside the domain (docs/04-architecture.md).
        val localeTags = LocaleList.getDefault().toLanguageTags().split(',')
        applicationScope.launch { preferencesRepository.initializeOnFirstLaunch(localeTags) }
    }
}
