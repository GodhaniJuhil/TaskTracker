package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.repository.TaskRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class TaskRepositoryImpl(
    firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TaskRepository {

    private val tasks = firestore.collection(COLLECTION)

    override fun getTasksStream(): Flow<List<Task>> =
        tasks.snapshots().map { snapshot ->
            snapshot.documents
                .map { it.data<Task>().copy(id = it.id) }
                .sortedByDescending { it.updatedAt }
        }

    override suspend fun addTask(task: Task): Result<Task> = firebaseCall {
        val now = Clock.System.now().toEpochMilliseconds()
        val uid = auth.currentUser?.uid
        val toSave = task.copy(createdBy = task.createdBy ?: uid, updatedBy = uid, createdAt = now, updatedAt = now)
        val ref = tasks.add(toSave)
        toSave.copy(id = ref.id)
    }

    override suspend fun updateTask(task: Task): Result<Unit> = firebaseCall {
        val updated = task.copy(
            updatedBy = auth.currentUser?.uid,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        tasks.document(task.id).set(updated)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = firebaseCall {
        tasks.document(taskId).delete()
    }

    private companion object {
        const val COLLECTION = "tasks"
    }
}
