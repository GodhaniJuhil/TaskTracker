package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.domain.repository.WorkspaceRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class WorkspaceRepositoryImpl(
    firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : WorkspaceRepository {

    private val users = firestore.collection("users")
    private val workspaces = firestore.collection("workspaces")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun workspacesStream(): Flow<List<Workspace>> =
        auth.authStateChanged.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                flow {
                    ensurePersonalWorkspace(user.uid)
                    emitAll(
                        workspaces.where { "members" contains user.uid }.snapshots().map { snapshot ->
                            snapshot.documents
                                .map { it.data<Workspace>().copy(id = it.id) }
                                .sortedWith(compareByDescending<Workspace> { it.id == Workspace.personalId(user.uid) }.thenBy { it.createdAt })
                        }.onEach { list -> publishMissingEmail(user.uid, list) },
                    )
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun currentWorkspaceStream(): Flow<Workspace?> =
        auth.authStateChanged.flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                combine(selectedIdStream(user.uid), workspacesStream()) { selectedId, all ->
                    all.firstOrNull { it.id == selectedId }
                        ?: all.firstOrNull { it.id == Workspace.personalId(user.uid) }
                        ?: all.firstOrNull()
                }
            }
        }

    override suspend fun createWorkspace(name: String): Result<Workspace> = firebaseCall {
        val uid = requireUid()
        val workspace = Workspace(
            name = name.trim(),
            ownerId = uid,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            members = listOf(uid),
            emails = mapOf(uid to auth.currentUser?.email.orEmpty().lowercase()),
        )
        val created = workspace.copy(id = workspaces.add(workspace).id)
        select(uid, created.id)
        created
    }

    override suspend fun switchWorkspace(workspaceId: String): Result<Unit> = firebaseCall {
        select(requireUid(), workspaceId)
    }

    override suspend fun leaveWorkspace(workspaceId: String): Result<Unit> = firebaseCall {
        val uid = requireUid()
        check(workspaceId != Workspace.personalId(uid)) { "You can't leave your personal workspace" }
        workspaces.document(workspaceId).update(
            "members" to FieldValue.arrayRemove(uid),
            "roles.$uid" to FieldValue.delete,
            "emails.$uid" to FieldValue.delete,
        )
        select(uid, Workspace.personalId(uid))
    }

    override suspend fun renameWorkspace(workspaceId: String, name: String): Result<Unit> = firebaseCall {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Enter a workspace name" }
        require(trimmed.length <= MAX_NAME) { "Keep the name under $MAX_NAME characters" }
        workspaces.document(workspaceId).update("name" to trimmed)
    }

    override suspend fun removeMember(workspaceId: String, uid: String): Result<Unit> = firebaseCall {
        check(requireUid() != uid) { "Use Leave workspace to remove yourself" }
        workspaces.document(workspaceId).update(
            "members" to FieldValue.arrayRemove(uid),
            "roles.$uid" to FieldValue.delete,
            "emails.$uid" to FieldValue.delete,
        )
    }

    override suspend fun setMemberRole(workspaceId: String, uid: String, role: Role): Result<Unit> = firebaseCall {
        require(role != Role.OWNER) { "Ownership can't be transferred yet" }
        check(requireUid() != uid) { "You can't change your own role" }
        workspaces.document(workspaceId).update("roles.$uid" to role.name)
    }

    private fun selectedIdStream(uid: String): Flow<String?> =
        users.document(uid).snapshots().map { snapshot ->
            if (snapshot.exists) runCatching { snapshot.get<String?>("currentWorkspaceId") }.getOrNull() else null
        }

    private suspend fun select(uid: String, workspaceId: String) {
        users.document(uid).set(SelectedWorkspace(workspaceId), merge = true)
    }

    // Idempotent; the id is derived from the uid so a retry or a second device can't create a duplicate.
    private suspend fun ensurePersonalWorkspace(uid: String) {
        firebaseCall {
            val ref = workspaces.document(Workspace.personalId(uid))
            if (!ref.get().exists) {
                ref.set(
                    Workspace(
                        name = Workspace.DEFAULT_NAME,
                        ownerId = uid,
                        createdAt = Clock.System.now().toEpochMilliseconds(),
                        members = listOf(uid),
                        emails = mapOf(uid to auth.currentUser?.email.orEmpty().lowercase()),
                    ),
                )
            }
        }
    }

    // Workspaces created before emails were stored lack the caller's entry; each member fills in their own,
    // since other users' profiles aren't readable.
    private suspend fun publishMissingEmail(uid: String, list: List<Workspace>) {
        val email = auth.currentUser?.email?.lowercase().orEmpty()
        if (email.isEmpty()) return
        list.filter { uid in it.members && it.emails[uid].isNullOrBlank() }.forEach { ws ->
            firebaseCall { workspaces.document(ws.id).update("emails.$uid" to email) }
        }
    }

    private fun requireUid(): String = auth.currentUser?.uid ?: error("Not signed in")
}

private const val MAX_NAME = 40

@Serializable
private data class SelectedWorkspace(val currentWorkspaceId: String)
