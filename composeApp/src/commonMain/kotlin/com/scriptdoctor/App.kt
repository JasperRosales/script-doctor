package com.scriptdoctor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import com.scriptdoctor.ui.HistoryScreen
import com.scriptdoctor.ui.HomeScreen
import com.scriptdoctor.ui.ResultScreen
import com.scriptdoctor.ui.SettingsScreen

private val Teal = Color(0xFF00695C)
private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Color(0xFF455A64),
    tertiary = Color(0xFF6A1B9A),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    secondary = Color(0xFF90A4AE),
    tertiary = Color(0xFFCE93D8),
)

@Composable
fun App(onStateCreated: (AppState) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope).also(onStateCreated) }
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        AppScaffold(state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(state: AppState) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            state.error = null
        }
    }
    val screen = state.screen
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (screen) {
                            Screen.Home -> "Script Doctor"
                            is Screen.Result -> "Decoded prescription"
                            Screen.History -> "History"
                            Screen.Settings -> "Settings"
                        },
                    )
                },
                navigationIcon = {
                    if (screen != Screen.Home) {
                        IconButton(onClick = { state.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (screen == Screen.Home) {
                        IconButton(onClick = { state.navigate(Screen.History) }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                        IconButton(onClick = { state.navigate(Screen.Settings) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (screen) {
            Screen.Home -> HomeScreen(state, modifier)
            is Screen.Result -> ResultScreen(screen.result, modifier)
            Screen.History -> HistoryScreen(state, modifier)
            Screen.Settings -> SettingsScreen(state, modifier)
        }
    }
}
