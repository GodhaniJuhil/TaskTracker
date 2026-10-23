package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.repository.TaskRepository
import com.tasktracker.app.domain.repository.TeamRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class TaskRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val teamRepository: TeamRepository,
) : TaskRepository {

    /**
     * Tasks live in the user's team (teams/{teamId}/tasks) when they belong to one, so all members
     * read and write the same list; otherwise in their personal list (users/{uid}/tasks).
     */
    private fun workspace(uid: String, teamId: String?): CollectionReference =
        if (teamId != null) {
            firestore.collection("teams").document(teamId).collection(TASKS)
        } else {
            firestore.collection("users").document(uid).collection(TASKS)
        }

    private suspend fun currentWorkspace(): CollectionReference {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        return workspace(uid, teamRepository.currentTeamStream().first()?.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTasksStream(): Flow<List<Task>> =
        auth.authStateChanged.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                teamRepository.currentTeamStream()
                    .map { it?.id }
                    .distinctUntilChanged()
                    .flatMapLatest { teamId ->
                        workspace(user.uid, teamId).snapshots().map { snapshot ->
                            snapshot.documents
                                .map { it.data<Task>().copy(id = it.id) }
                                .sortedByDescending { it.updatedAt }
                        }
                    }
            }
        }

    override suspend fun addTask(task: Task): Result<Task> = firebaseCall {
        val now = Clock.System.now().toEpochMilliseconds()
        val uid = auth.currentUser?.uid
        val toSave = task.copy(createdBy = task.createdBy ?: uid, updatedBy = uid, createdAt = now, updatedAt = now)
        val ref = currentWorkspace().add(toSave)
        toSave.copy(id = ref.id)
    }

    override suspend fun updateTask(task: Task): Result<Unit> = firebaseCall {
        val updated = task.copy(
            updatedBy = auth.currentUser?.uid,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        currentWorkspace().document(task.id).set(updated)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = firebaseCall {
        currentWorkspace().document(taskId).delete()
    }

    private companion object {
        const val TASKS = "tasks"
    }
}
