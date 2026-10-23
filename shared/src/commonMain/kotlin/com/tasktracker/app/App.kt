package com.tasktracker.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tasktracker.app.presentation.screen.AuthScreen
import com.tasktracker.app.presentation.screen.MainScreen
import com.tasktracker.app.presentation.theme.AppTheme
import com.tasktracker.app.presentation.theme.ThemeController
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

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
        Navigator(startScreen) { SlideTransition(it) }
    }
}
