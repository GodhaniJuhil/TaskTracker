package com.tasktracker.app.presentation.theme

import androidx.compose.runtime.Composable

// iOS has no system back button; the edge-swipe is handled by the navigation container.
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
