package com.tasktracker.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus { TODO, IN_PROGRESS, DONE }

@Serializable
enum class TaskPriority { LOW, MEDIUM, HIGH }

@Serializable
data class Task(
    // Mirrors the Firestore document id; filled in by the repository when reading.
    val id: String = "",
    val workspaceId: String = "",
    val title: String = "",
    val description: String = "",
    // Kept in sync with [status] (DONE <=> true) because the notification function keys off it.
    val isCompleted: Boolean = false,
    // Null on tasks created before boards existed; see [currentStatus].
    val status: TaskStatus? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assignedTo: String? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val currentStatus: TaskStatus get() = status ?: if (isCompleted) TaskStatus.DONE else TaskStatus.TODO
    val isDone: Boolean get() = currentStatus == TaskStatus.DONE
}

/** The fields to change on an existing task; null leaves a field untouched. */
data class TaskChanges(
    val title: String? = null,
    val description: String? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
)
