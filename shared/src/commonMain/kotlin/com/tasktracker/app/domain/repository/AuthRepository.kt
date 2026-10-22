package com.tasktracker.app.domain.repository

import com.tasktracker.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the signed-in user, or null when signed out. */
    val currentUserStream: Flow<User?>

    suspend fun signUpWithEmail(email: String, password: String): Result<User>

    suspend fun signInWithEmail(email: String, password: String): Result<User>

    /** [idToken] is the Google ID token obtained by the native sign-in flow of the platform. */
    suspend fun signInWithGoogle(idToken: String): Result<User>

    suspend fun sendEmailVerification(): Result<Unit>

    /** Reloads the user from Firebase so [User.isEmailVerified] reflects a completed verification. */
    suspend fun refreshCurrentUser(): Result<User?>

    /** Stores the device push token on the signed-in user profile; no-op when signed out. */
    suspend fun updateFcmToken(token: String): Result<Unit>

    suspend fun signOut()

    fun getCurrentUser(): User?
}
