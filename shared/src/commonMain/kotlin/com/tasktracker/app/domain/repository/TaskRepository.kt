package com.tasktracker.app.domain.repository

import com.tasktracker.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /** Real-time stream; emits a new list whenever the remote collection changes. */
    fun getTasksStream(): Flow<List<Task>>

    suspend fun addTask(task: Task): Result<Task>

    suspend fun updateTask(task: Task): Result<Unit>

    suspend fun deleteTask(taskId: String): Result<Unit>
}
