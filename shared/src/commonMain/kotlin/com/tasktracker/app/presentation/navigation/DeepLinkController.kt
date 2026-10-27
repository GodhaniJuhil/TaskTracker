package com.tasktracker.app.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class TaskTarget(val workspaceId: String, val taskId: String)

/** Hand-off from the platform host (e.g. a tapped notification) to the UI: which task to open once the app is up. */
object DeepLinkController {
    var pendingTask by mutableStateOf<TaskTarget?>(null)
}
