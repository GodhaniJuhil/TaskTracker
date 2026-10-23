package com.tasktracker.app.domain.repository

import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.model.TaskChanges
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /** Real-time stream; emits a new list whenever the remote collection changes. */
    fun getTasksStream(): Flow<List<Task>>

    suspend fun addTask(task: Task): Result<Task>

    /** Writes only the fields set in [changes], so concurrent edits to other fields survive. */
    suspend fun updateTask(taskId: String, changes: TaskChanges): Result<Unit>

    suspend fun deleteTask(taskId: String): Result<Unit>
}
