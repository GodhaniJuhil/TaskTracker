package com.tasktracker.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class InvitationStatus { PENDING, ACCEPTED, DECLINED }

@Serializable
data class Invitation(
    // "{workspaceId}_{receiverEmail}", so the Firestore rules can look an invitation up by workspace and email.
    val id: String = "",
    val workspaceId: String = "",
    val workspaceName: String = "",
    val senderEmail: String = "",
    val receiverEmail: String = "",
    // The role the receiver gets on accepting: a Role name, never OWNER.
    val role: String = Role.MEMBER.name,
    val status: InvitationStatus = InvitationStatus.PENDING,
    val createdAt: Long = 0L,
)
