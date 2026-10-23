package com.tasktracker.app.domain.repository

import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    /** Every workspace the signed-in user belongs to; the personal workspace is created on first use. */
    fun workspacesStream(): Flow<List<Workspace>>

    /** The workspace tasks are currently scoped to; falls back to the personal workspace. */
    fun currentWorkspaceStream(): Flow<Workspace?>

    /** Creates a workspace owned by the caller and makes it the current one. */
    suspend fun createWorkspace(name: String): Result<Workspace>

    suspend fun switchWorkspace(workspaceId: String): Result<Unit>

    /** Not allowed for the owner or for the personal workspace. */
    suspend fun leaveWorkspace(workspaceId: String): Result<Unit>

    /** Owners and admins. */
    suspend fun renameWorkspace(workspaceId: String, name: String): Result<Unit>

    /** Owners can remove anyone but themselves; admins can remove members and guests. */
    suspend fun removeMember(workspaceId: String, uid: String): Result<Unit>

    /** Owner only. The owner's own role can't be changed and nobody can be promoted to OWNER. */
    suspend fun setMemberRole(workspaceId: String, uid: String, role: Role): Result<Unit>
}
