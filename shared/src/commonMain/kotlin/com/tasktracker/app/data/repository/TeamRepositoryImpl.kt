package com.tasktracker.app.data.repository

import com.tasktracker.app.data.firebase.firebaseCall
import com.tasktracker.app.domain.model.Team
import com.tasktracker.app.domain.repository.TeamRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class TeamRepositoryImpl(
    firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TeamRepository {

    private val teams = firestore.collection("teams")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun currentTeamStream(): Flow<Team?> =
        auth.authStateChanged.flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                // A user belongs to at most one team; membership lives on the team document.
                teams.where { "memberIds" contains user.uid }.snapshots().map { snapshot ->
                    snapshot.documents.firstOrNull()?.let { it.data<Team>().copy(id = it.id) }
                }
            }
        }

    override suspend fun createTeam(name: String): Result<Team> = firebaseCall {
        val uid = requireUid()
        val team = Team(name = name, ownerId = uid, memberIds = listOf(uid))
        team.copy(id = teams.add(team).id)
    }

    override suspend fun joinTeam(teamId: String): Result<Team> = firebaseCall {
        val uid = requireUid()
        val ref = teams.document(teamId)
        val snapshot = ref.get()
        check(snapshot.exists) { "No team found for that invite code" }
        ref.updateFields { "memberIds" to FieldValue.arrayUnion(uid) }
        val team = snapshot.data<Team>()
        team.copy(id = snapshot.id, memberIds = team.memberIds + uid)
    }

    override suspend fun leaveTeam(teamId: String): Result<Unit> = firebaseCall {
        val uid = requireUid()
        teams.document(teamId).updateFields { "memberIds" to FieldValue.arrayRemove(uid) }
    }

    private fun requireUid(): String = auth.currentUser?.uid ?: error("Not signed in")
}
