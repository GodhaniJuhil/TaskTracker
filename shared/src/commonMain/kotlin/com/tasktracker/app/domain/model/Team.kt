package com.tasktracker.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    // Mirrors the Firestore document id, which doubles as the invite code.
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
)
