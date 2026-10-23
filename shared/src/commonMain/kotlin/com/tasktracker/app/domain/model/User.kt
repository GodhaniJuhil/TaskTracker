package com.tasktracker.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String? = null,
    val isEmailVerified: Boolean = false,
    val fcmToken: String? = null,
    val currentWorkspaceId: String? = null,
)
