package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.AppNotification
import com.tasktracker.app.domain.repository.NotificationRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : NotificationRepository {

    private val notifications = firestore.collection("notifications")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun notificationsStream(): Flow<List<AppNotification>> =
        auth.authStateChanged.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                // Sorted here rather than with orderBy, which would need a composite index.
                notifications.where { "userId" equalTo user.uid }.snapshots().map { snapshot ->
                    snapshot.documents
                        .map { it.data<AppNotification>().copy(id = it.id) }
                        .sortedByDescending { it.createdAt }
                }
            }
        }

    override suspend fun markRead(id: String): Result<Unit> = firebaseCall {
        notifications.document(id).update("isRead" to true)
    }

    override suspend fun markAllRead(): Result<Unit> = firebaseCall {
        val batch = firestore.batch()
        notificationsStream().first().filter { !it.isRead }
            .forEach { batch.update(notifications.document(it.id), "isRead" to true) }
        batch.commit()
    }

    override suspend fun clearAll(): Result<Unit> = firebaseCall {
        val batch = firestore.batch()
        notificationsStream().first().forEach { batch.delete(notifications.document(it.id)) }
        batch.commit()
    }
}
