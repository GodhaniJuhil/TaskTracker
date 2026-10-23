package com.tasktracker.app.domain.repository

import com.tasktracker.app.domain.model.Team
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    /** The team the signed-in user belongs to, or null when working solo or signed out. */
    fun currentTeamStream(): Flow<Team?>

    suspend fun createTeam(name: String): Result<Team>

    /** [teamId] is the invite code shown to existing members. */
    suspend fun joinTeam(teamId: String): Result<Team>

    suspend fun leaveTeam(teamId: String): Result<Unit>
}
