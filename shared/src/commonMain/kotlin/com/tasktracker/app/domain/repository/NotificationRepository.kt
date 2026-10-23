package com.tasktracker.app.domain.repository

import com.tasktracker.app.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /** Activity feed across all of the user's workspaces, newest first. */
    fun notificationsStream(): Flow<List<AppNotification>>

    suspend fun markRead(id: String): Result<Unit>

    suspend fun markAllRead(): Result<Unit>

    suspend fun clearAll(): Result<Unit>
}
