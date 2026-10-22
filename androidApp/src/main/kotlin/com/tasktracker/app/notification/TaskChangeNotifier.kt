package com.tasktracker.app.notification

import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.repository.AuthRepository
import com.tasktracker.app.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Watches the real-time task stream while a user is signed in and posts a local notification for
 * changes made by someone else. Changes the current user made themselves are ignored.
 */
class TaskChangeNotifier(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val notifications: NotificationHelper,
) {
    private var previous: Map<String, Task>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        authRepository.currentUserStream
            .flatMapLatest { user ->
                // A new session starts with a fresh baseline, so its first snapshot is silent.
                previous = null
                if (user == null) emptyFlow() else taskRepository.getTasksStream()
            }
            .onEach(::onTasks)
            .catch { /* stream errors surface in the UI; nothing to notify */ }
            .launchIn(scope)
    }

    private fun onTasks(tasks: List<Task>) {
        val old = previous
        previous = tasks.associateBy { it.id }
        if (old == null) return

        val me = authRepository.getCurrentUser()?.uid
        for (task in tasks) {
            if (task.updatedBy == me) continue
            val before = old[task.id]
            when {
                before == null -> notifications.show(task.id.hashCode(), "New task", task.title)
                !before.isCompleted && task.isCompleted ->
                    notifications.show(task.id.hashCode(), "Task completed", task.title)
                before.title != task.title || before.description != task.description ->
                    notifications.show(task.id.hashCode(), "Task updated", task.title)
            }
        }
    }
}
