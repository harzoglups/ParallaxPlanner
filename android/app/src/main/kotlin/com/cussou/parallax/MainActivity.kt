package com.cussou.parallax

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.languageOf
import com.cussou.parallax.ui.quickcalc.QuickCalcRoute
import com.cussou.parallax.ui.settings.SettingsRoute
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme
import kotlinx.coroutines.launch

/** AppCompatActivity, not ComponentActivity: it applies the language chosen in the app (F-10). */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferencesRepository = (application as ParallaxApp).preferencesRepository
        syncLanguagePreference(preferencesRepository)

        setContent {
            val preferences by preferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = when (preferences?.theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            // Status and navigation bar icons follow the app theme, not the system one.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            ParallaxPlannerTheme(darkTheme) {
                // Simple mode has two screens only (docs/03-ux.md): no navigation library needed yet.
                var showSettings by rememberSaveable { mutableStateOf(false) }
                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsRoute(onBack = { showSettings = false })
                } else {
                    QuickCalcRoute(onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    /**
     * The language can also be changed outside the app, in the system per-app language settings
     * (Android 13+). The stored preference follows, so the settings screen shows the language in use.
     */
    private fun syncLanguagePreference(preferencesRepository: PreferencesRepository) {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val language = appLocales[0]?.let { languageOf(it.toLanguageTag()) } ?: return
        lifecycleScope.launch {
            preferencesRepository.update { if (it.language == language) it else it.copy(language = language) }
        }
    }
}
