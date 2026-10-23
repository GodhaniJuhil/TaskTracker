package com.tasktracker.app.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tasktracker.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TaskTrackerMessagingService : FirebaseMessagingService(), KoinComponent {

    private val authRepository: AuthRepository by inject()
    private val notifications: NotificationHelper by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Called when FCM issues or rotates the device token; persist it on the user profile. */
    override fun onNewToken(token: String) {
        scope.launch { authRepository.updateFcmToken(token) }
    }

    /**
     * Handles FCM messages (e.g. sent from the Firebase console). While the
     * app is in the foreground this renders them; in the background the system shows the notification
     * payload itself using the manifest's default channel and icon.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Task Tracker"
        val body = message.notification?.body ?: message.data["body"] ?: return
        notifications.show((message.messageId ?: body).hashCode(), title, body)
    }
}
