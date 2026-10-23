package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.Invitation
import com.tasktracker.app.domain.model.InvitationStatus
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.domain.repository.InvitationRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class InvitationRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : InvitationRepository {

    private val invitations = firestore.collection("invitations")

    override fun receivedStream(): Flow<List<Invitation>> = stream("receiverEmail")

    override fun sentStream(): Flow<List<Invitation>> = stream("senderEmail")

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun stream(emailField: String): Flow<List<Invitation>> =
        auth.authStateChanged.flatMapLatest { user ->
            val email = user?.email?.lowercase()
            if (email.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                invitations.where { emailField equalTo email }.snapshots().map { snapshot ->
                    snapshot.documents
                        .map { it.data<Invitation>().copy(id = it.id) }
                        .sortedByDescending { it.createdAt }
                }
            }
        }

    override suspend fun invite(workspace: Workspace, email: String, role: Role): Result<Unit> = firebaseCall {
        val sender = auth.currentUser?.email?.lowercase() ?: error("Not signed in")
        val receiver = email.trim().lowercase()
        require(receiver.matches(EMAIL)) { "Enter a valid email address" }
        require(receiver != sender) { "You can't invite yourself" }
        require(role != Role.OWNER) { "Invite people as admin, member or guest" }
        invitations.document("${workspace.id}_$receiver").set(
            Invitation(
                workspaceId = workspace.id,
                workspaceName = workspace.name,
                senderEmail = sender,
                receiverEmail = receiver,
                role = role.name,
                status = InvitationStatus.PENDING,
                createdAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun accept(invitation: Invitation): Result<Unit> = firebaseCall {
        val user = checkNotNull(auth.currentUser) { "Not signed in" }
        check(user.isEmailVerified) { "Verify your email before joining a workspace" }
        // One batch so a user is never a member without the invitation being closed (and vice versa).
        firestore.batch()
            .update(invitations.document(invitation.id), "status" to InvitationStatus.ACCEPTED.name)
            .update(
                firestore.collection("workspaces").document(invitation.workspaceId),
                "members" to FieldValue.arrayUnion(user.uid),
                "roles.${user.uid}" to invitation.role,
                "emails.${user.uid}" to user.email.orEmpty().lowercase(),
            )
            .commit()
    }

    override suspend fun decline(invitation: Invitation): Result<Unit> = firebaseCall {
        invitations.document(invitation.id).update("status" to InvitationStatus.DECLINED.name)
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s/]+@[^@\\s/]+\\.[^@\\s/]+$")
    }
}
