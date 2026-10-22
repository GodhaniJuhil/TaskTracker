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
     * Foreground and data messages. With a high-priority payload (see [FcmPayloadExample]) this also
     * runs when the app is backgrounded or killed, so the local notification is posted via the
     * max-priority channel and wakes the screen. Notification-only messages in the background are
     * displayed by the system on the manifest's default (high-importance) channel.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Task Tracker"
        val body = message.notification?.body ?: message.data["body"] ?: return
        notifications.show((message.messageId ?: body).hashCode(), title, body)
    }
}
