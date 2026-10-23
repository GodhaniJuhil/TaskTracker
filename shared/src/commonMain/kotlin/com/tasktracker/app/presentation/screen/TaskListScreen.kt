package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import com.tasktracker.app.presentation.viewmodel.TaskListViewModel
import com.tasktracker.app.presentation.viewmodel.TeamViewModel
import org.koin.compose.viewmodel.koinViewModel

class TaskListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val taskViewModel = koinViewModel<TaskListViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()
        val taskState by taskViewModel.uiState.collectAsStateWithLifecycle()
        val authState by authViewModel.uiState.collectAsStateWithLifecycle()
        val teamState by koinViewModel<TeamViewModel>().uiState.collectAsStateWithLifecycle()

        var taskPendingDelete by remember { mutableStateOf<Task?>(null) }
        var showAddDialog by rememberSaveable { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

        // Signed out (here or elsewhere) -> back to the auth flow.
        LaunchedEffect(authState.isSignedIn) {
            if (!authState.isSignedIn) navigator.replaceAll(AuthScreen())
        }
        LaunchedEffect(taskState.error) {
            taskState.error?.let {
                snackbarHostState.showSnackbar(it)
                taskViewModel.clearError()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(teamState.team?.name ?: "Tasks") },
                    actions = {
                        TextButton(onClick = { navigator.push(TeamScreen()) }) { Text("Team") }
                        TextButton(onClick = authViewModel::signOut) { Text("Sign out") }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    taskState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    taskState.tasks.isEmpty() -> Text(
                        "No tasks yet. Tap + to add one.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    else -> TaskList(
                        tasks = taskState.tasks,
                        onToggle = taskViewModel::toggleCompleted,
                        onDelete = { taskPendingDelete = it },
                    )
                }
            }
        }

        taskPendingDelete?.let { task ->
            AlertDialog(
                onDismissRequest = { taskPendingDelete = null },
                title = { Text("Delete task?") },
                text = { Text("\"${task.title}\" will be permanently deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        taskViewModel.deleteTask(task.id)
                        taskPendingDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { taskPendingDelete = null }) { Text("Cancel") } },
            )
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description ->
                    taskViewModel.addTask(title, description)
                    showAddDialog = false
                },
            )
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    LazyColumn(
        // Bottom padding clears the FAB (56dp + 16dp margin) so the last card is never covered.
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskItem(task, onToggle = { onToggle(task) }, onDelete = { onDelete(task) })
        }
    }
}

@Composable
private fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                )
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
