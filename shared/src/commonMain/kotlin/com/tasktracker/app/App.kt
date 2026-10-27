package com.tasktracker.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import com.tasktracker.app.presentation.screen.SplashScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tasktracker.app.presentation.screen.AuthScreen
import com.tasktracker.app.presentation.screen.MainScreen
import com.tasktracker.app.presentation.theme.AppTheme
import com.tasktracker.app.presentation.theme.ThemeController
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

private const val SPLASH_DURATION_MS = 1500L

/** [onDarkThemeChanged] lets the host sync system bar icon colours with the in-app theme. */
@Composable
fun App(onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val dark = ThemeController.isDark()
    LaunchedEffect(dark) { onDarkThemeChanged(dark) }

    AppTheme(darkTheme = dark) {
        val authViewModel = koinViewModel<AuthViewModel>()
        // Decided once at launch; afterwards the screens navigate off the auth StateFlow.
        val startScreen = remember {
            val user = authViewModel.uiState.value.user
            if (user != null && user.isEmailVerified) MainScreen() else AuthScreen()
        }
        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(SPLASH_DURATION_MS)
            showSplash = false
        }

        Box(Modifier.fillMaxSize()) {
            Navigator(startScreen) { SlideTransition(it) }
            AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(400))) { SplashScreen() }
        }
    }
}
