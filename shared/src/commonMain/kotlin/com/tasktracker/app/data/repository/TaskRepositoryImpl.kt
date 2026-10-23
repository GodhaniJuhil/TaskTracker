package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.model.TaskChanges
import com.tasktracker.app.domain.model.TaskStatus
import com.tasktracker.app.domain.repository.TaskRepository
import com.tasktracker.app.domain.repository.WorkspaceRepository
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
    private val workspaceRepository: WorkspaceRepository,
) : TaskRepository {

    /** Tasks live in workspaces/{workspaceId}/tasks, so they never leak across workspaces. */
    private fun tasks(workspaceId: String): CollectionReference =
        firestore.collection("workspaces").document(workspaceId).collection(TASKS)

    private suspend fun currentWorkspaceId(): String =
        workspaceRepository.currentWorkspaceStream().first()?.id ?: error("No workspace selected")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTasksStream(): Flow<List<Task>> =
        workspaceRepository.currentWorkspaceStream()
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { workspaceId ->
                if (workspaceId == null) {
                    flowOf(emptyList())
                } else {
                    tasks(workspaceId).snapshots().map { snapshot ->
                        snapshot.documents
                            .map { it.data<Task>().copy(id = it.id, workspaceId = workspaceId) }
                            .sortedByDescending { it.updatedAt }
                    }
                }
            }

    override suspend fun addTask(task: Task): Result<Task> = firebaseCall {
        val now = Clock.System.now().toEpochMilliseconds()
        val uid = auth.currentUser?.uid
        val workspaceId = currentWorkspaceId()
        val toSave = task.copy(
            workspaceId = workspaceId,
            status = task.status ?: TaskStatus.TODO,
            isCompleted = task.status == TaskStatus.DONE,
            createdBy = task.createdBy ?: uid,
            updatedBy = uid,
            createdAt = now,
            updatedAt = now,
        )
        toSave.copy(id = tasks(workspaceId).add(toSave).id)
    }

    override suspend fun updateTask(taskId: String, changes: TaskChanges): Result<Unit> = firebaseCall {
        val fields = buildList<Pair<String, Any?>> {
            changes.title?.let { add("title" to it) }
            changes.description?.let { add("description" to it) }
            changes.priority?.let { add("priority" to it.name) }
            changes.status?.let {
                add("status" to it.name)
                add("isCompleted" to (it == TaskStatus.DONE))
            }
            add("updatedBy" to auth.currentUser?.uid)
            add("updatedAt" to Clock.System.now().toEpochMilliseconds())
        }
        tasks(currentWorkspaceId()).document(taskId).update(*fields.toTypedArray())
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = firebaseCall {
        val ref = tasks(currentWorkspaceId()).document(taskId)
        // A delete carries no author, so stamp the actor first; the notification function reads it from the old document.
        ref.update("updatedBy" to auth.currentUser?.uid)
        ref.delete()
    }

    private companion object {
        const val TASKS = "tasks"
    }
}
