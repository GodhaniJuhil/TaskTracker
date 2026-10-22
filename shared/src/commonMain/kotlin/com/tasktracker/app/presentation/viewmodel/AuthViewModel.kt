package com.tasktracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasktracker.app.domain.auth.GoogleIdTokenProvider
import com.tasktracker.app.domain.model.User
import com.tasktracker.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val verificationEmailSent: Boolean = false,
    val error: String? = null,
) {
    val isSignedIn: Boolean get() = user != null
    val needsEmailVerification: Boolean get() = user != null && !user.isEmailVerified
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleIdTokenProvider: GoogleIdTokenProvider? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(user = authRepository.getCurrentUser()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        authRepository.currentUserStream
            .onEach { user -> _uiState.update { it.copy(user = user) } }
            .catch { e -> _uiState.update { it.copy(error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun signIn(email: String, password: String) =
        submit { authRepository.signInWithEmail(email, password) }

    /** Creates the account, then sends the verification email. */
    fun register(email: String, password: String) = submit {
        authRepository.signUpWithEmail(email, password).onSuccess {
            authRepository.sendEmailVerification()
                .onSuccess { _uiState.update { s -> s.copy(verificationEmailSent = true) } }
        }
    }

    /** Runs the platform Google sign-in UI, then exchanges the token with Firebase. */
    fun signInWithGoogle() = submit {
        val provider = googleIdTokenProvider
            ?: return@submit Result.failure(IllegalStateException("Google sign-in is not configured on this platform"))
        provider.getIdToken().fold(
            onSuccess = { authRepository.signInWithGoogle(it) },
            onFailure = { Result.failure(it) },
        )
    }

    /** For platforms that obtain the Google ID token themselves (e.g. from Swift). */
    fun signInWithGoogleToken(idToken: String) = submit { authRepository.signInWithGoogle(idToken) }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            authRepository.sendEmailVerification().fold(
                onSuccess = { _uiState.update { it.copy(verificationEmailSent = true, error = null) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } },
            )
        }
    }

    /** Call after the user taps the link in the email, e.g. when the app returns to foreground. */
    fun refreshVerificationStatus() {
        viewModelScope.launch {
            authRepository.refreshCurrentUser().fold(
                onSuccess = { user -> _uiState.update { it.copy(user = user) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } },
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(verificationEmailSent = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun submit(action: suspend () -> Result<User>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = action()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    user = result.getOrNull() ?: it.user,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}
