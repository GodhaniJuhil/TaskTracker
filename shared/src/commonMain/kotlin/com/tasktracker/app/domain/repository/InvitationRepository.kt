package com.tasktracker.app.domain.repository

import com.tasktracker.app.domain.model.Invitation
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {
    /** Invitations addressed to the signed-in user's email, newest first. */
    fun receivedStream(): Flow<List<Invitation>>

    /** Invitations the signed-in user sent, newest first. */
    fun sentStream(): Flow<List<Invitation>>

    /** Owners and admins only; [role] is what the receiver becomes on accepting. */
    suspend fun invite(workspace: Workspace, email: String, role: Role): Result<Unit>

    /** Joins the workspace. Requires a verified email. */
    suspend fun accept(invitation: Invitation): Result<Unit>

    suspend fun decline(invitation: Invitation): Result<Unit>
}
