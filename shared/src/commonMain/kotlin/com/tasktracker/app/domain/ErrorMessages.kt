package com.tasktracker.app.domain

/**
 * Turns an exception into something a user can act on. Our own require/check messages are already
 * written for users; anything else (Firebase, network) is matched on its text, because the exception
 * classes differ per platform.
 */
fun Throwable.toUserMessage(): String {
    if (this is IllegalArgumentException || this is IllegalStateException) message?.let { return it }
    val m = (message ?: "").lowercase()
    return when {
        "invalid_login_credentials" in m || "invalid_credential" in m || "credential is incorrect" in m ||
            "password is invalid" in m || "wrong-password" in m -> "Incorrect email or password."
        "no user record" in m || "user-not-found" in m -> "No account found for that email."
        "already in use" in m || "email-already" in m -> "An account with this email already exists. Try signing in."
        "badly formatted" in m || "invalid-email" in m -> "Enter a valid email address."
        "weak" in m || "at least 6" in m -> "Choose a stronger password with at least 6 characters."
        "too many" in m || "blocked all requests" in m -> "Too many attempts. Wait a moment and try again."
        "network" in m || "unavailable" in m || "timeout" in m || "timed out" in m || "offline" in m ->
            "No connection. Check your internet and try again."
        "permission_denied" in m || "permission denied" in m || "insufficient permissions" in m ->
            "You don't have permission to do that."
        "disabled" in m -> "This account has been disabled."
        "cancel" in m -> "Sign-in was cancelled."
        else -> "Something went wrong. Please try again."
    }
}
