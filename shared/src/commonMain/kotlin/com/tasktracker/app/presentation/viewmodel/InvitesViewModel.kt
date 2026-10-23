package com.tasktracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasktracker.app.domain.model.Invitation
import com.tasktracker.app.domain.model.InvitationStatus
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.domain.repository.InvitationRepository
import com.tasktracker.app.domain.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvitesUiState(
    val received: List<Invitation> = emptyList(),
    val sent: List<Invitation> = emptyList(),
    val isSending: Boolean = false,
    /** Shown inside the invite dialog, so the typed email is kept and the user can fix it. */
    val inviteError: String? = null,
    /** Bumped on every successful invitation; the dialog closes when it changes. */
    val invitesSent: Int = 0,
    val message: String? = null,
    val error: String? = null,
) {
    val pendingReceivedCount: Int get() = received.count { it.status == InvitationStatus.PENDING }
}

class InvitesViewModel(
    private val invitationRepository: InvitationRepository,
) : ViewModel() {

    private data class Form(val isSending: Boolean = false, val error: String? = null, val sent: Int = 0)
    private data class Feedback(val message: String? = null, val error: String? = null)

    private val form = MutableStateFlow(Form())
    private val feedback = MutableStateFlow(Feedback())

    val uiState: StateFlow<InvitesUiState> = combine(
        invitationRepository.receivedStream(),
        invitationRepository.sentStream(),
        form,
        feedback,
    ) { received, sent, f, fb ->
        InvitesUiState(received, sent, f.isSending, f.error, f.sent, fb.message, fb.error)
    }
        .catch { e -> emit(InvitesUiState(error = e.toUserMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvitesUiState())

    fun invite(workspace: Workspace, email: String, role: Role) {
        viewModelScope.launch {
            form.update { it.copy(isSending = true, error = null) }
            invitationRepository.invite(workspace, email, role).fold(
                onSuccess = {
                    form.update { it.copy(isSending = false, sent = it.sent + 1) }
                    feedback.value = Feedback(message = "Invitation sent to ${email.trim().lowercase()}")
                },
                onFailure = { e -> form.update { it.copy(isSending = false, error = e.toUserMessage()) } },
            )
        }
    }

    fun clearInviteError() = form.update { it.copy(error = null) }

    fun accept(invitation: Invitation) = run("Joined ${invitation.workspaceName}") { invitationRepository.accept(invitation) }

    fun decline(invitation: Invitation) = run("Invitation declined") { invitationRepository.decline(invitation) }

    fun clearFeedback() {
        feedback.value = Feedback()
    }

    private fun run(successMessage: String, action: suspend () -> Result<*>) {
        viewModelScope.launch {
            feedback.value = action().fold(
                onSuccess = { Feedback(message = successMessage) },
                onFailure = { Feedback(error = it.toUserMessage()) },
            )
        }
    }
}
