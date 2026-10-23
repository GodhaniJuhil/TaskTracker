package com.tasktracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.domain.repository.WorkspaceRepository
import com.tasktracker.app.domain.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkspaceUiState(
    val workspaces: List<Workspace> = emptyList(),
    val current: Workspace? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class WorkspaceViewModel(
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val actionError = MutableStateFlow<String?>(null)
    private val streamError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WorkspaceUiState> = combine(
        workspaceRepository.workspacesStream(),
        workspaceRepository.currentWorkspaceStream(),
        actionError,
        streamError,
    ) { all, current, action, stream ->
        WorkspaceUiState(workspaces = all, current = current, isLoading = false, error = action ?: stream)
    }
        .catch { e ->
            val message = e.toUserMessage()
            streamError.value = message
            emit(WorkspaceUiState(isLoading = false, error = message))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceUiState())

    fun createWorkspace(name: String) = run { workspaceRepository.createWorkspace(name) }

    fun switchWorkspace(workspaceId: String) = run { workspaceRepository.switchWorkspace(workspaceId) }

    fun leaveWorkspace(workspaceId: String) = run { workspaceRepository.leaveWorkspace(workspaceId) }

    fun renameWorkspace(workspaceId: String, name: String) = run { workspaceRepository.renameWorkspace(workspaceId, name) }

    fun removeMember(workspaceId: String, uid: String) = run { workspaceRepository.removeMember(workspaceId, uid) }

    fun setMemberRole(workspaceId: String, uid: String, role: Role) =
        run { workspaceRepository.setMemberRole(workspaceId, uid, role) }

    fun clearError() {
        actionError.value = null
        streamError.value = null
    }

    private fun run(action: suspend () -> Result<*>) {
        viewModelScope.launch {
            action().onFailure { actionError.value = it.toUserMessage() }
        }
    }
}
