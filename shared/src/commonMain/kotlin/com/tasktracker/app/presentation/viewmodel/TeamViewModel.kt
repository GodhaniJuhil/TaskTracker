package com.tasktracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasktracker.app.domain.model.Team
import com.tasktracker.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeamUiState(
    val team: Team? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class TeamViewModel(
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val actionError = MutableStateFlow<String?>(null)
    private val streamError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TeamUiState> = combine(
        teamRepository.currentTeamStream()
            .map { TeamUiState(team = it, isLoading = false) }
            .catch { e ->
                streamError.value = e.message
                emit(TeamUiState(isLoading = false))
            },
        actionError,
        streamError,
    ) { state, action, stream -> state.copy(error = action ?: stream) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamUiState())

    fun createTeam(name: String) = run { teamRepository.createTeam(name.trim()) }

    fun joinTeam(inviteCode: String) = run { teamRepository.joinTeam(inviteCode.trim()) }

    fun leaveTeam(teamId: String) = run { teamRepository.leaveTeam(teamId) }

    private fun run(action: suspend () -> Result<*>) {
        viewModelScope.launch {
            actionError.value = action().exceptionOrNull()?.message
        }
    }
}
