package com.tasktracker.app

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tasktracker.app.notification.NotificationHelper
import com.tasktracker.app.presentation.navigation.DeepLinkController
import com.tasktracker.app.presentation.navigation.TaskTarget

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* notifications are optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash up until Compose has drawn its first frame, so the hand-off to SplashScreen() has no gap.
        var ready by mutableStateOf(false)
        installSplashScreen().setKeepOnScreenCondition { !ready }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // A recreated activity (rotation) must not replay the notification that launched it.
        if (savedInstanceState == null) routeNotificationIntent(intent)

        setContent {
            LaunchedEffect(Unit) { ready = true }
            // The in-app theme can differ from the system one, so the bar icons follow the app, not the OS.
            App(onDarkThemeChanged = { dark ->
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeNotificationIntent(intent)
    }

    /** "Open task" from a notification: hand the target to the shared UI, which switches workspace and opens the task. */
    private fun routeNotificationIntent(intent: Intent?) {
        val workspaceId = intent?.getStringExtra(NotificationHelper.EXTRA_WORKSPACE_ID)
        val taskId = intent?.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
        if (!workspaceId.isNullOrEmpty() && !taskId.isNullOrEmpty()) {
            DeepLinkController.pendingTask = TaskTarget(workspaceId, taskId)
        }
    }
}
