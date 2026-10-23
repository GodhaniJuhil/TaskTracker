package com.tasktracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasktracker.app.domain.model.AppNotification
import com.tasktracker.app.domain.repository.NotificationRepository
import com.tasktracker.app.domain.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val actionError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.notificationsStream(),
        actionError,
    ) { list, error -> NotificationsUiState(list, isLoading = false, error = error) }
        .catch { e -> emit(NotificationsUiState(isLoading = false, error = e.toUserMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    fun markRead(id: String) = run { notificationRepository.markRead(id) }

    fun markAllRead() = run { notificationRepository.markAllRead() }

    fun clearAll() = run { notificationRepository.clearAll() }

    fun clearError() {
        actionError.value = null
    }

    private fun run(action: suspend () -> Result<*>) {
        viewModelScope.launch {
            action().onFailure { actionError.value = it.toUserMessage() }
        }
    }
}
