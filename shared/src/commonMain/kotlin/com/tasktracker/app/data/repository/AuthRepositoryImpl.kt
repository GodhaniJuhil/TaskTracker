package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.User
import com.tasktracker.app.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
) : AuthRepository {

    override val currentUserStream: Flow<User?> =
        auth.authStateChanged.map { it?.toDomain() }

    override suspend fun signUpWithEmail(email: String, password: String): Result<User> = firebaseCall {
        val firebaseUser = requireNotNull(auth.createUserWithEmailAndPassword(email, password).user) {
            "Registration returned no user"
        }
        firebaseUser.toDomain().withFreshToken().also { saveProfile(it) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = firebaseCall {
        val firebaseUser = requireNotNull(auth.signInWithEmailAndPassword(email, password).user) {
            "Sign-in returned no user"
        }
        firebaseUser.toDomain().withFreshToken().also { saveProfile(it) }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = firebaseCall {
        val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
        val firebaseUser = requireNotNull(auth.signInWithCredential(credential).user) {
            "Google sign-in returned no user"
        }
        firebaseUser.toDomain().withFreshToken().also { saveProfile(it) }
    }

    override suspend fun sendEmailVerification(): Result<Unit> = firebaseCall {
        val user = checkNotNull(auth.currentUser) { "No signed-in user" }
        user.sendEmailVerification()
    }

    override suspend fun refreshCurrentUser(): Result<User?> = firebaseCall {
        auth.currentUser?.let {
            it.reload()
            auth.currentUser?.toDomain()
        }
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> = firebaseCall {
        val uid = auth.currentUser?.uid ?: return@firebaseCall
        firestore.collection("users").document(uid).update("fcmToken" to token)
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun getCurrentUser(): User? = auth.currentUser?.toDomain()

    // Token fetch is best effort: push may be unavailable (permissions, simulator, no Play Services).
    private suspend fun User.withFreshToken(): User =
        firebaseCall { messaging.getToken() }.getOrNull()?.let { copy(fcmToken = it) } ?: this

    private suspend fun saveProfile(user: User) {
        firestore.collection("users").document(user.uid).set(user)
    }

    private fun FirebaseUser.toDomain() = User(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName,
        isEmailVerified = isEmailVerified,
    )
}
