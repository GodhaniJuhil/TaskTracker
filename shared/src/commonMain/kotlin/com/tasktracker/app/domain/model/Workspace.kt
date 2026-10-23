package com.tasktracker.app.domain.model

import kotlinx.serialization.Serializable

/** What a member may do in a workspace. The owner is [Workspace.ownerId]; everyone else is listed in [Workspace.roles]. */
@Serializable
enum class Role {
    OWNER, ADMIN, MEMBER, GUEST;

    val canEditTasks: Boolean get() = this != GUEST
    val canInvite: Boolean get() = this == OWNER || this == ADMIN
    val canManageRoles: Boolean get() = this == OWNER
}

@Serializable
data class Workspace(
    // Mirrors the Firestore document id; filled in by the repository when reading.
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L,
    val members: List<String> = emptyList(),
    // uid -> Role name for every non-owner member; a member without an entry is a MEMBER.
    val roles: Map<String, String> = emptyMap(),
    // uid -> email, so the roster can be shown without reading other users' private profiles.
    val emails: Map<String, String> = emptyMap(),
) {
    /** The workspace every user gets on sign-up; it can be shared like any other. */
    val isPersonal: Boolean get() = id.startsWith(PERSONAL_PREFIX)

    fun roleOf(uid: String?): Role = when {
        uid == null -> Role.GUEST
        uid == ownerId -> Role.OWNER
        else -> roles[uid]?.let { name -> Role.entries.firstOrNull { it.name == name } } ?: Role.MEMBER
    }

    fun memberList(): List<WorkspaceMember> =
        members.map { WorkspaceMember(it, emails[it].orEmpty(), roleOf(it)) }
            .sortedWith(compareBy({ it.role.ordinal }, { it.email }))

    companion object {
        const val PERSONAL_PREFIX = "personal_"
        const val DEFAULT_NAME = "Team Board"
        fun personalId(uid: String) = PERSONAL_PREFIX + uid
    }
}

data class WorkspaceMember(val uid: String, val email: String, val role: Role)
