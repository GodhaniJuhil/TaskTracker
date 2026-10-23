package com.tasktracker.app.presentation.theme

import androidx.compose.runtime.Composable

/** Intercepts the system back gesture while [enabled]; a no-op where the platform has none. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
