package com.askyoutube.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.askyoutube.app.data.settings.ThemeMode
import com.askyoutube.app.ui.home.HomeScreen
import com.askyoutube.app.ui.home.HomeViewModel
import com.askyoutube.app.ui.settings.SettingsScreen
import com.askyoutube.app.ui.settings.SettingsViewModel
import com.askyoutube.app.ui.theme.AskYoutubeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { ThemedApp() }
    }
}

/**
 * Resolves the effective theme and hosts the app inside it.
 *
 * The override lives in Settings; following the OS is the default. The choice is
 * held here rather than read deep in the tree so switching it repaints the whole
 * app immediately.
 */
@Composable
private fun ThemedApp() {
    val context = LocalContext.current
    val settings = remember(context) {
        (context.applicationContext as AskYoutubeApp).container.settings()
    }
    var mode by remember { mutableStateOf(settings.themeMode) }

    AskYoutubeTheme(
        darkTheme = when (mode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    ) {
        AppRoot(onThemeModeChange = { mode = it })
    }
}

private enum class Screen { HOME, SETTINGS }

@Composable
private fun AppRoot(onThemeModeChange: (ThemeMode) -> Unit) {
    // ponytail: two destinations, no arguments, no deep links, and no back stack
    // worth persisting. A screen enum plus BackHandler is a fraction of a
    // Navigation Compose graph and one less dependency. Introduce Navigation
    // Compose on the third destination, or the moment deep links or real back
    // stack restoration start to matter.
    var screen by remember { mutableStateOf(Screen.HOME) }

    val homeViewModel: HomeViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val homeState by homeViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()

    fun leaveSettings() {
        screen = Screen.HOME
        // Home shows a "set your key first" prompt, so it needs to know whether
        // one is now stored.
        homeViewModel.onSettingsChanged()
    }

    BackHandler(enabled = screen == Screen.SETTINGS) { leaveSettings() }

    when (screen) {
        Screen.HOME -> HomeScreen(
            state = homeState,
            onUrlChange = homeViewModel::onUrlChange,
            onDraftChange = homeViewModel::onDraftChange,
            onSend = homeViewModel::send,
            onApplySuggestion = homeViewModel::applySuggestion,
            onRetry = homeViewModel::retry,
            onDismissError = homeViewModel::dismissError,
            onOpenSettings = { screen = Screen.SETTINGS },
        )

        Screen.SETTINGS -> SettingsScreen(
            state = settingsState,
            onApiKeyChange = settingsViewModel::onApiKeyChange,
            onSaveKey = settingsViewModel::saveKey,
            onValidateKey = settingsViewModel::validateKey,
            onClearKey = settingsViewModel::clearKey,
            onTopKChange = settingsViewModel::onTopKChange,
            onEmbeddingModelChange = settingsViewModel::onEmbeddingModelChange,
            onChatModelChange = settingsViewModel::onChatModelChange,
            onProviderChange = settingsViewModel::onProviderChange,
            onBaseUrlChange = settingsViewModel::onBaseUrlChange,
            onEmbeddingSourceChange = settingsViewModel::onEmbeddingSourceChange,
            onEmbeddingBaseUrlChange = settingsViewModel::onEmbeddingBaseUrlChange,
            onEmbeddingApiKeyChange = settingsViewModel::onEmbeddingApiKeyChange,
            onSaveEmbeddingKey = settingsViewModel::onSaveEmbeddingKey,
            onThemeModeChange = { mode ->
                settingsViewModel.onThemeModeChange(mode)
                onThemeModeChange(mode)
            },
            onBack = ::leaveSettings,
        )
    }
}
