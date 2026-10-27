package com.tasktracker.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tasktracker.app.MainActivity
import com.tasktracker.app.R

/** What an FCM data message asks for; the keys match the payload built in functions/index.js. */
data class PushMessage(
    val title: String,
    val body: String,
    val type: String = "",
    val category: String = "",
    val workspaceId: String = "",
    val entityId: String = "",
) {
    /** Stable per item, so a newer push about the same invite or task replaces the older one. */
    val notificationId: Int get() = "$category:$type:$entityId".hashCode()
}

class NotificationHelper(private val context: Context) {

    fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        // High importance gives heads-up display; it is the standard level for time-sensitive messages.
        val channel = NotificationChannel(CHANNEL_ID, "Task updates", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }

    /** Posts a notification, with action buttons for the actionable categories. Skipped without POST_NOTIFICATIONS (API 33+). */
    fun show(message: PushMessage) {
        if (!canPost()) return

        val builder = baseBuilder(message.title, message.body)
        when (message.category) {
            CATEGORY_INVITE_REQUEST -> builder
                .setContentIntent(openApp())
                .addAction(0, "Accept", broadcast(message, NotificationActionReceiver.ACTION_ACCEPT_INVITE))
                .addAction(0, "Decline", broadcast(message, NotificationActionReceiver.ACTION_DECLINE_INVITE))

            CATEGORY_TASK_ASSIGNED -> builder
                .setContentIntent(openTask(message))
                .addAction(0, "Mark done", broadcast(message, NotificationActionReceiver.ACTION_MARK_DONE))
                .addAction(0, "Open task", openTask(message))

            CATEGORY_TASK_VIEW -> builder
                .setContentIntent(openTask(message))
                .addAction(0, "Open task", openTask(message))

            else -> builder.setContentIntent(openApp())
        }
        NotificationManagerCompat.from(context).notify(message.notificationId, builder.build())
    }

    /** Replaces a notification whose action failed with a plain one; tapping it opens the app so the user can retry there. */
    fun showFailure(id: Int, title: String, body: String) {
        if (!canPost()) return
        NotificationManagerCompat.from(context).notify(id, baseBuilder(title, body).setContentIntent(openApp()).build())
    }

    fun cancel(id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun canPost() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun baseBuilder(title: String, body: String) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_task)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setAutoCancel(true)
        // Stops the system from adding its own chips (e.g. "Email contact") next to ours.
        .setAllowSystemGeneratedContextualActions(false)

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Foreground action: launches the app and carries the target in extras for [MainActivity] to route. */
    private fun openTask(message: PushMessage): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode(message, ACTION_OPEN_TASK),
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_WORKSPACE_ID, message.workspaceId)
            .putExtra(EXTRA_TASK_ID, message.entityId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Background action: handled by [NotificationActionReceiver] without opening any UI. */
    private fun broadcast(message: PushMessage, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(message, action),
        Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_NOTIFICATION_ID, message.notificationId)
            .putExtra(EXTRA_WORKSPACE_ID, message.workspaceId)
            .putExtra(EXTRA_ENTITY_ID, message.entityId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // Each (notification, button) pair needs its own request code, or the PendingIntents would overwrite each other.
    private fun requestCode(message: PushMessage, action: String) = "${message.notificationId}:$action".hashCode()

    companion object {
        const val CHANNEL_ID = "task_updates_high"

        const val CATEGORY_INVITE_REQUEST = "INVITE_REQUEST"
        const val CATEGORY_TASK_ASSIGNED = "TASK_ASSIGNED"
        const val CATEGORY_TASK_VIEW = "TASK_VIEW"

        const val ACTION_OPEN_TASK = "ACTION_OPEN_TASK"

        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_WORKSPACE_ID = "workspaceId"
        const val EXTRA_ENTITY_ID = "entityId"
        const val EXTRA_TASK_ID = "taskId"
    }
}
