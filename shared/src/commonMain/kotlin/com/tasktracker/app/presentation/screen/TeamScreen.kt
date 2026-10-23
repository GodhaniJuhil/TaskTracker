package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tasktracker.app.domain.model.Team
import com.tasktracker.app.presentation.viewmodel.TeamViewModel
import org.koin.compose.viewmodel.koinViewModel

class TeamScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<TeamViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Team") },
                    navigationIcon = { TextButton(onClick = { navigator.pop() }) { Text("Back") } },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val team = state.team
                when {
                    state.isLoading -> Text("Loading...")
                    team != null -> TeamDetails(team, onLeave = { viewModel.leaveTeam(team.id) })
                    else -> NoTeam(onCreate = viewModel::createTeam, onJoin = viewModel::joinTeam)
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TeamDetails(team: Team, onLeave: () -> Unit) {
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    Text(team.name, style = MaterialTheme.typography.headlineSmall)
    Text("${team.memberIds.size} member(s). Tasks are shared with everyone in the team.")
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text("Invite code", style = MaterialTheme.typography.titleSmall)
    // Long-press to select and copy, then send it to teammates.
    SelectionContainer { Text(team.id, style = MaterialTheme.typography.bodyLarge) }
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = { confirmLeave = true }, modifier = Modifier.fillMaxWidth()) { Text("Leave team") }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave team?") },
            text = { Text("You will go back to your personal task list and stop receiving this team's updates.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onLeave()
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NoTeam(onCreate: (String) -> Unit, onJoin: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Text("You are working solo. Create a team or join one to share tasks.")
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Team name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { onCreate(name) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text("Create team")
    }
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text("Invite code") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = { onJoin(code) }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text("Join team")
    }
}
