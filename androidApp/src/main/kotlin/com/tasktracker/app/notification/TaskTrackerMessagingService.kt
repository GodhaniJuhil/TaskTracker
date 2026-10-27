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
     * The backend sends data-only, high-priority messages, so this runs in every app state (foreground,
     * background, killed) and builds the notification, including its action buttons. A console-sent
     * message with a `notification` payload still works while the app is in the foreground.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val body = data["body"] ?: message.notification?.body ?: return
        notifications.show(
            PushMessage(
                title = data["title"] ?: message.notification?.title ?: "Task Tracker",
                body = body,
                type = data["type"].orEmpty(),
                category = data["category"].orEmpty(),
                workspaceId = data["workspaceId"].orEmpty(),
                entityId = data["entityId"].orEmpty().ifEmpty { message.messageId ?: body },
            ),
        )
    }
}
