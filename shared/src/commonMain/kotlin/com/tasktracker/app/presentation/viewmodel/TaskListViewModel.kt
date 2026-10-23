package com.tasktracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.model.TaskChanges
import com.tasktracker.app.domain.model.TaskPriority
import com.tasktracker.app.domain.model.TaskStatus
import com.tasktracker.app.domain.repository.TaskRepository
import com.tasktracker.app.domain.repository.WorkspaceRepository
import com.tasktracker.app.domain.toUserMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val error: String? = null,
)

class TaskListViewModel(
    private val taskRepository: TaskRepository,
    workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val actionError = MutableStateFlow<String?>(null)

    /**
     * Restarts from a loading state on every workspace switch, so the previous workspace's tasks are
     * never shown under the new workspace's name while its first snapshot is on the way.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val stream = workspaceRepository.currentWorkspaceStream()
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { workspaceId ->
            flow {
                emit(TaskListUiState(isLoading = workspaceId != null))
                emitAll(
                    taskRepository.getTasksStream()
                        .map { TaskListUiState(tasks = it, isLoading = false) }
                        .catch { e -> emit(TaskListUiState(isLoading = false, loadFailed = true, error = e.toUserMessage())) },
                )
            }
        }

    val uiState: StateFlow<TaskListUiState> = combine(stream, actionError) { state, action ->
        state.copy(error = action ?: state.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun addTask(title: String, description: String = "", priority: TaskPriority = TaskPriority.MEDIUM) =
        run { taskRepository.addTask(Task(title = title, description = description, priority = priority)) }

    fun setStatus(task: Task, status: TaskStatus) =
        run { taskRepository.updateTask(task.id, TaskChanges(status = status)) }

    fun editTask(task: Task, changes: TaskChanges) = run { taskRepository.updateTask(task.id, changes) }

    fun toggleCompleted(task: Task) =
        setStatus(task, if (task.isDone) TaskStatus.TODO else TaskStatus.DONE)

    fun deleteTask(taskId: String) = run { taskRepository.deleteTask(taskId) }

    fun clearError() {
        actionError.value = null
    }

    private fun run(action: suspend () -> Result<*>) {
        viewModelScope.launch {
            action().onFailure { actionError.value = it.toUserMessage() }
        }
    }
}
