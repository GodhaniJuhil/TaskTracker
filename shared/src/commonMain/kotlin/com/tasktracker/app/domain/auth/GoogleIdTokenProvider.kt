package com.tasktracker.app.domain.auth

/**
 * Platform hook that runs the native Google sign-in UI (Credential Manager on Android,
 * GoogleSignIn on iOS) and returns the Google ID token. Register an implementation in Koin
 * per platform; the shared code only consumes the token.
 */
fun interface GoogleIdTokenProvider {
    suspend fun getIdToken(): Result<String>
}
