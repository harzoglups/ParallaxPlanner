package com.cussou.parallax.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.cussou.parallax.R
import com.cussou.parallax.ui.matches.MatchListRoute
import com.cussou.parallax.ui.matches.MatchRoute
import com.cussou.parallax.ui.quickcalc.QuickCalcRoute
import com.cussou.parallax.ui.quickcalc.StageRoute
import com.cussou.parallax.ui.settings.SettingsRoute

/** The top-level screens. Their bottom bar items are tagged "nav.CALC", "nav.MATCHES", "nav.SETTINGS". */
enum class Screen { CALC, MATCHES, SETTINGS }

const val NAVIGATION_TAG = "nav"

/**
 * Navigation between the top-level screens (docs/03-ux.md, progressive disclosure):
 * simple mode is one screen with a gear icon, advanced mode adds a bottom bar and the matches.
 * A state is enough for that; no navigation library is needed.
 */
@Composable
fun AppNavigation(advancedMode: Boolean) {
    var selected by rememberSaveable { mutableStateOf(Screen.CALC) }
    // The match opened inside the Matches tab, if any.
    var openMatchId by rememberSaveable { mutableStateOf<String?>(null) }
    // The stage opened inside that match, if any.
    var openStageId by rememberSaveable { mutableStateOf<String?>(null) }
    val openMatch: (String?) -> Unit = { openMatchId = it; openStageId = null }
    // Matches are hidden, not deleted, while advanced mode is off (F-41).
    val screen = if (!advancedMode && selected == Screen.MATCHES) Screen.CALC else selected

    if (!advancedMode) {
        BackHandler(enabled = screen == Screen.SETTINGS) { selected = Screen.CALC }
        ScreenContent(
                screen,
                advancedMode,
                openMatchId,
                openStageId,
                onOpenMatch = openMatch,
                onOpenStage = { openStageId = it },
                onNavigate = { selected = it },
            )
        return
    }
    Scaffold(
        // Each screen has its own top bar and handles the status bar itself.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == screen,
                        onClick = { selected = item },
                        icon = { ScreenIcon(item) },
                        label = { Text(stringResource(item.label)) },
                        modifier = Modifier.testTag("$NAVIGATION_TAG.$item"),
                    )
                }
            }
        },
    ) { barPadding ->
        // The bar already covers the navigation inset: the screens must not add it again.
        Box(Modifier.padding(barPadding).consumeWindowInsets(barPadding)) {
            ScreenContent(
                screen,
                advancedMode,
                openMatchId,
                openStageId,
                onOpenMatch = openMatch,
                onOpenStage = { openStageId = it },
                onNavigate = { selected = it },
            )
        }
    }
}

@Composable
private fun ScreenContent(
    screen: Screen,
    advancedMode: Boolean,
    openMatchId: String?,
    openStageId: String?,
    onOpenMatch: (String?) -> Unit,
    onOpenStage: (String?) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    when (screen) {
        Screen.CALC -> QuickCalcRoute(
            onOpenSettings = { onNavigate(Screen.SETTINGS) },
            // With the bottom bar, Settings is a tab: the gear icon would be a duplicate.
            showSettingsAction = !advancedMode,
        )
        Screen.MATCHES -> when {
            openMatchId == null -> MatchListRoute(onOpenMatch = onOpenMatch)
            openStageId == null -> {
                BackHandler { onOpenMatch(null) }
                MatchRoute(openMatchId, onBack = { onOpenMatch(null) }, onOpenStage = onOpenStage)
            }
            else -> {
                BackHandler { onOpenStage(null) }
                StageRoute(openMatchId, openStageId, onBack = { onOpenStage(null) })
            }
        }
        Screen.SETTINGS -> SettingsRoute(onBack = if (advancedMode) null else ({ onNavigate(Screen.CALC) }))
    }
}

private val Screen.label: Int
    get() = when (this) {
        Screen.CALC -> R.string.nav_calc
        Screen.MATCHES -> R.string.matches
        Screen.SETTINGS -> R.string.settings_title
    }

@Composable
private fun ScreenIcon(screen: Screen) {
    when (screen) {
        Screen.CALC -> Icon(painterResource(R.drawable.ic_reticle), contentDescription = null)
        Screen.MATCHES -> Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
        Screen.SETTINGS -> Icon(Icons.Outlined.Settings, contentDescription = null)
    }
}
