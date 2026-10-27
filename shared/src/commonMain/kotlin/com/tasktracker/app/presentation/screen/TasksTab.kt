package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.model.TaskChanges
import com.tasktracker.app.domain.model.TaskPriority
import com.tasktracker.app.domain.model.TaskStatus
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.presentation.theme.AppButton
import com.tasktracker.app.presentation.theme.AppCard
import com.tasktracker.app.presentation.theme.AppChip
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppIcons
import com.tasktracker.app.presentation.theme.AppTag
import com.tasktracker.app.presentation.theme.AppTextButton
import com.tasktracker.app.presentation.theme.EmptyState
import com.tasktracker.app.presentation.theme.SkeletonCard
import com.tasktracker.app.presentation.theme.tactileClickable
import com.tasktracker.app.presentation.viewmodel.TaskListUiState

private enum class TaskFilter(val label: String) { All("All"), Open("Open"), Done("Done") }

internal fun TaskPriority.label() = when (this) {
    TaskPriority.LOW -> "Low"
    TaskPriority.MEDIUM -> "Medium"
    TaskPriority.HIGH -> "High"
}

@Composable
internal fun TaskPriority.color(): Color = when (this) {
    TaskPriority.LOW -> AppColors.Primary
    TaskPriority.MEDIUM -> AppColors.Warning
    TaskPriority.HIGH -> AppColors.Danger
}

@Composable
fun TasksTab(
    state: TaskListUiState,
    workspace: Workspace?,
    canEdit: Boolean,
    onAdd: (title: String, priority: TaskPriority) -> Unit,
    onToggle: (Task) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Task, TaskChanges) -> Unit,
    openTaskId: String? = null,
    onOpenTaskHandled: () -> Unit = {},
) {
    var editing by remember { mutableStateOf<Task?>(null) }

    // A notification's "Open task" lands here once the right workspace's tasks have loaded.
    LaunchedEffect(openTaskId, state.tasks) {
        val target = openTaskId ?: return@LaunchedEffect
        state.tasks.firstOrNull { it.id == target }?.let {
            if (canEdit) editing = it
            onOpenTaskHandled()
        }
    }
    var filterIndex by rememberSaveable { mutableStateOf(TaskFilter.Open.ordinal) }
    val filter = TaskFilter.entries[filterIndex]
    var taskPendingDelete by remember { mutableStateOf<Task?>(null) }

    val open = state.tasks.count { !it.isDone }
    val visible = when (filter) {
        TaskFilter.All -> state.tasks
        TaskFilter.Open -> state.tasks.filter { !it.isDone }
        TaskFilter.Done -> state.tasks.filter { it.isDone }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (state.isLoading) "Loading tasks…" else "$open open ${if (open == 1) "task" else "tasks"}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(workspace?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
            }
        }
        item { if (canEdit) QuickAddCard(onAdd) else GuestNote(workspace) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskFilter.entries.forEach { f ->
                    val count = when (f) {
                        TaskFilter.All -> state.tasks.size
                        TaskFilter.Open -> open
                        TaskFilter.Done -> state.tasks.size - open
                    }
                    AppChip(f.label, selected = f == filter, onClick = { filterIndex = f.ordinal }, count = if (state.isLoading) null else count)
                }
            }
        }
        when {
            state.isLoading -> items(3) { SkeletonCard() }
            state.loadFailed -> item {
                EmptyState(AppIcons.Close, "Couldn't load tasks", state.error ?: "Check your connection and try again.")
            }
            visible.isEmpty() -> item {
                when (filter) {
                    TaskFilter.Done -> EmptyState(AppIcons.Check, "Nothing finished yet", "Completed tasks collect here.")
                    else -> EmptyState(
                        AppIcons.Tasks,
                        if (state.tasks.isEmpty()) "No tasks yet" else "All caught up",
                        if (canEdit) "Add one above to get started." else "Tasks added by your team will show up here.",
                    )
                }
            }
            else -> items(visible, key = { it.id }) { task ->
                TaskItem(task, canEdit, onToggle = { onToggle(task) }, onDelete = { taskPendingDelete = task }, onEdit = { editing = task })
            }
        }
    }

    editing?.let { task ->
        EditTaskDialog(task, onDismiss = { editing = null }, onSave = { onEdit(task, it); editing = null })
    }

    taskPendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text("Delete task?") },
            text = { Text("\"${task.title}\" will be permanently deleted.") },
            confirmButton = {
                AppTextButton(onClick = {
                    onDelete(task.id)
                    taskPendingDelete = null
                }) { Text("Delete", color = AppColors.Danger) }
            },
            dismissButton = { AppTextButton(onClick = { taskPendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun QuickAddCard(onAdd: (String, TaskPriority) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var priorityIndex by rememberSaveable { mutableStateOf(TaskPriority.MEDIUM.ordinal) }
    val priority = TaskPriority.entries[priorityIndex]
    val submit = {
        if (title.isNotBlank()) {
            onAdd(title.trim(), priority)
            title = ""
        }
    }

    AppCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What needs doing?") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { p ->
                    AppChip(p.label(), selected = p == priority, onClick = { priorityIndex = p.ordinal })
                }
            }
            AppButton(onClick = submit, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Add task", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GuestNote(workspace: Workspace?) {
    AppCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.Lock, contentDescription = null, tint = AppColors.TextMuted)
            Text(
                "You have guest access to ${workspace?.name ?: "this workspace"}. You can follow along but not change tasks.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

@Composable
private fun TaskItem(task: Task, canEdit: Boolean, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    AppCard(if (canEdit) Modifier.tactileClickable(onEdit) else Modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle() },
                enabled = canEdit,
                colors = CheckboxDefaults.colors(checkedColor = AppColors.Success),
            )
            Column(Modifier.weight(1f).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isDone) AppColors.TextMuted else AppColors.TextPrimary,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                )
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted, maxLines = 3)
                }
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppTag(task.priority.label(), task.priority.color())
                    if (task.currentStatus == TaskStatus.IN_PROGRESS) AppTag("In progress", AppColors.Primary)
                }
            }
            if (canEdit) {
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcons.Delete, contentDescription = "Delete ${task.title}", tint = AppColors.TextMuted, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
