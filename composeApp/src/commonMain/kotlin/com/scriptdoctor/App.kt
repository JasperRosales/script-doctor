package com.scriptdoctor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scriptdoctor.ui.HistoryScreen
import com.scriptdoctor.ui.HomeScreen
import com.scriptdoctor.ui.ResultScreen
import com.scriptdoctor.ui.SettingsScreen

private val LightColors = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBE2),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF05201C),
    tertiary = Color(0xFF5B5B9E),
    tertiaryContainer = Color(0xFFE2DFFF),
    background = Color(0xFFF4F8F7),
    surface = Color(0xFFF4F8F7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEAF1EF),
    surfaceContainerHigh = Color(0xFFE3ECEA),
    surfaceVariant = Color(0xFFDCE5E2),
    onSurfaceVariant = Color(0xFF4F5B58),
    outlineVariant = Color(0xFFCBD6D3),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EDBC8),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFB2EBE2),
    secondaryContainer = Color(0xFF334B47),
    tertiary = Color(0xFFC3C1FF),
    background = Color(0xFF0E1513),
    surface = Color(0xFF0E1513),
    surfaceContainerLow = Color(0xFF171E1C),
    surfaceContainer = Color(0xFF1B2220),
    surfaceContainerHigh = Color(0xFF252C2A),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
private fun appTypography(): Typography {
    val base = Typography()
    return base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun App(onStateCreated: (AppState) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope).also(onStateCreated) }
    AppHost(state)
}

@Composable
fun AppHost(state: AppState) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        typography = appTypography(),
    ) {
        AppScaffold(state)
    }
}

private data class Tab(val screen: Screen, val label: String, val selected: ImageVector, val unselected: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, "Scan", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner),
    Tab(Screen.History, "History", Icons.Filled.History, Icons.Outlined.History),
    Tab(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

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
    val isResult = screen is Screen.Result
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            // Home draws its own hero header.
            if (screen != Screen.Home) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            when (screen) {
                                is Screen.Result -> "Your prescription"
                                Screen.History -> "History"
                                Screen.Settings -> "Settings"
                                Screen.Home -> ""
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        if (isResult) {
                            IconButton(onClick = { state.back() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = {
            if (!isResult) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = screen == tab.screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { state.navigate(tab.screen) },
                            icon = {
                                val count = state.history.size
                                if (tab.screen == Screen.History && count > 0) {
                                    BadgedBox(badge = { Badge { Text("$count") } }) {
                                        Icon(if (selected) tab.selected else tab.unselected, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(if (selected) tab.selected else tab.unselected, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            label = "screen",
        ) { s ->
            val modifier = Modifier.padding(padding)
            when (s) {
                // Home's hero header runs under the status bar, so only pad the bottom.
                Screen.Home -> HomeScreen(state, Modifier.padding(bottom = padding.calculateBottomPadding()))
                is Screen.Result -> ResultScreen(s.result, modifier)
                Screen.History -> HistoryScreen(state, modifier)
                Screen.Settings -> SettingsScreen(state, modifier)
            }
        }
    }
}
