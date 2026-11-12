package com.cussou.parallax

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cussou.parallax.data.PreferencesRepository
import com.cussou.parallax.domain.AccentColor
import com.cussou.parallax.domain.ThemeMode
import com.cussou.parallax.domain.languageOf
import com.cussou.parallax.ui.AppNavigation
import com.cussou.parallax.ui.onboarding.OnboardingRoute
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
            val onboardingDone by preferencesRepository.onboardingDone.collectAsStateWithLifecycle(initialValue = null)
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

            ParallaxPlannerTheme(darkTheme, preferences?.accent ?: AccentColor.AZURE) {
                when (onboardingDone) {
                    null -> Unit // still loading: draw nothing rather than the wrong screen
                    false -> OnboardingRoute()
                    true -> AppNavigation()
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
