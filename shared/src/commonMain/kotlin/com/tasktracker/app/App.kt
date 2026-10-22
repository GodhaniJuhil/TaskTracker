package com.tasktracker.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tasktracker.app.presentation.screen.AuthScreen
import com.tasktracker.app.presentation.screen.TaskListScreen
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        val authViewModel = koinViewModel<AuthViewModel>()
        // Decided once at launch; afterwards the screens navigate off the auth StateFlow.
        val startScreen = remember {
            val user = authViewModel.uiState.value.user
            if (user != null && user.isEmailVerified) TaskListScreen() else AuthScreen()
        }
        Navigator(startScreen) { SlideTransition(it) }
    }
}
