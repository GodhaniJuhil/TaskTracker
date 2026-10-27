package com.tasktracker.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType { INVITE, TASK_UPDATE, TASK_ASSIGNED }

/** An entry of the in-app activity feed. Written by Cloud Functions; clients only read, mark read and delete. */
@Serializable
data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val workspaceId: String = "",
    val type: NotificationType = NotificationType.TASK_UPDATE,
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
)
