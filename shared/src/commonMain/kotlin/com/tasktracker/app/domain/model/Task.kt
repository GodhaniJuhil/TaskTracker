package com.tasktracker.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    // Mirrors the Firestore document id; filled in by the repository when reading.
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val assignedTo: String? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
