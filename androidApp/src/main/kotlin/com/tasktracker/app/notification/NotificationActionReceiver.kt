package com.tasktracker.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tasktracker.app.domain.model.InvitationStatus
import com.tasktracker.app.domain.repository.InvitationRepository
import com.tasktracker.app.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Runs the notification buttons that act in the background (accept/decline an invite, mark a task done).
 * Works with the app killed: the manifest-registered receiver starts the process, [TaskTrackerApplication]
 * brings Koin up, and Firebase Auth restores the signed-in user from disk.
 */
class NotificationActionReceiver : BroadcastReceiver(), KoinComponent {

    private val invitations: InvitationRepository by inject()
    private val tasks: TaskRepository by inject()
    private val notifications: NotificationHelper by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        val workspaceId = intent.getStringExtra(NotificationHelper.EXTRA_WORKSPACE_ID).orEmpty()
        val entityId = intent.getStringExtra(NotificationHelper.EXTRA_ENTITY_ID).orEmpty()

        // goAsync keeps the process alive past onReceive; the system allows roughly 10 seconds.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val result = runCatching { withTimeout(ACTION_TIMEOUT_MS) { perform(action, workspaceId, entityId) } }
                    .mapCatching { it.getOrThrow() }
                result
                    .onSuccess { notifications.cancel(notificationId) }
                    .onFailure { notifications.showFailure(notificationId, failureTitle(action), it.message ?: "Open the app to try again.") }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun perform(action: String, workspaceId: String, entityId: String): Result<Unit> = when (action) {
        ACTION_ACCEPT_INVITE, ACTION_DECLINE_INVITE -> {
            val invitation = invitations.receivedStream().first().firstOrNull { it.id == entityId }
            when {
                invitation == null -> Result.failure(IllegalStateException("This invitation is no longer available."))
                invitation.status != InvitationStatus.PENDING -> Result.failure(IllegalStateException("This invitation was already answered."))
                action == ACTION_ACCEPT_INVITE -> invitations.accept(invitation)
                else -> invitations.decline(invitation)
            }
        }
        ACTION_MARK_DONE -> tasks.markDone(workspaceId, entityId)
        else -> Result.failure(IllegalArgumentException("Unknown action $action"))
    }

    private fun failureTitle(action: String) = when (action) {
        ACTION_ACCEPT_INVITE -> "Couldn't accept invitation"
        ACTION_DECLINE_INVITE -> "Couldn't decline invitation"
        else -> "Couldn't mark task done"
    }

    companion object {
        const val ACTION_ACCEPT_INVITE = "ACTION_ACCEPT_INVITE"
        const val ACTION_DECLINE_INVITE = "ACTION_DECLINE_INVITE"
        const val ACTION_MARK_DONE = "ACTION_MARK_DONE"
        private const val ACTION_TIMEOUT_MS = 8_000L
    }
}
