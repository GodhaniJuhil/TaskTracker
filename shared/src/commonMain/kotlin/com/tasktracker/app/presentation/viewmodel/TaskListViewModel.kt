package com.tasktracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class TaskListViewModel(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val actionError = MutableStateFlow<String?>(null)
    private val streamError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TaskListUiState> = combine(
        taskRepository.getTasksStream()
            .map { TaskListUiState(tasks = it, isLoading = false) }
            .catch { e ->
                streamError.value = e.message
                emit(TaskListUiState(isLoading = false))
            },
        actionError,
        streamError,
    ) { state, action, stream -> state.copy(error = action ?: stream) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun addTask(title: String, description: String = "", assignedTo: String? = null, createdBy: String? = null) =
        run { taskRepository.addTask(Task(title = title, description = description, assignedTo = assignedTo, createdBy = createdBy)) }

    fun toggleCompleted(task: Task) =
        run { taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted)) }

    fun deleteTask(taskId: String) = run { taskRepository.deleteTask(taskId) }

    fun clearError() {
        actionError.update { null }
    }

    private fun run(action: suspend () -> Result<*>) {
        viewModelScope.launch {
            actionError.value = action().exceptionOrNull()?.message
        }
    }
}
