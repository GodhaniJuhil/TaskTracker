package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.model.TaskChanges
import com.tasktracker.app.domain.model.TaskStatus
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.presentation.theme.AppCard
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppIcons
import com.tasktracker.app.presentation.theme.AppTag
import com.tasktracker.app.presentation.theme.AppTextButton
import com.tasktracker.app.presentation.theme.SkeletonCard
import com.tasktracker.app.presentation.theme.tactileClickable
import com.tasktracker.app.presentation.viewmodel.TaskListUiState

private fun TaskStatus.title() = when (this) {
    TaskStatus.TODO -> "To Do"
    TaskStatus.IN_PROGRESS -> "In Progress"
    TaskStatus.DONE -> "Done"
}

private fun TaskStatus.next() = when (this) {
    TaskStatus.TODO -> TaskStatus.IN_PROGRESS
    TaskStatus.IN_PROGRESS -> TaskStatus.DONE
    TaskStatus.DONE -> TaskStatus.TODO
}

private fun TaskStatus.moveLabel() = when (this) {
    TaskStatus.TODO -> "Start"
    TaskStatus.IN_PROGRESS -> "Finish"
    TaskStatus.DONE -> "Reopen"
}

/** Stacked columns rather than side-scrolling ones, so nothing is hidden off-screen on a phone. */
@Composable
fun BoardsTab(
    state: TaskListUiState,
    workspace: Workspace?,
    canEdit: Boolean,
    onMove: (Task, TaskStatus) -> Unit,
    onEdit: (Task, TaskChanges) -> Unit,
) {
    var editing by remember { mutableStateOf<Task?>(null) }
    editing?.let { task ->
        EditTaskDialog(task, onDismiss = { editing = null }, onSave = { onEdit(task, it); editing = null })
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Board", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(workspace?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
            }
        }
        if (state.isLoading) {
            items(3) { SkeletonCard() }
            return@LazyColumn
        }
        TaskStatus.entries.forEach { status ->
            val tasks = state.tasks.filter { it.currentStatus == status }
            item(key = "header_$status") {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(status.color(), CircleShape))
                    Text(status.title().uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    AppTag(tasks.size.toString(), AppColors.TextMuted)
                }
            }
            if (tasks.isEmpty()) {
                item(key = "empty_$status") {
                    Text("Nothing here", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(tasks, key = { it.id }) { task -> BoardCard(task, status, canEdit, onMove = { onMove(task, status.next()) }, onEdit = { editing = task }) }
            }
        }
    }
}

@Composable
private fun TaskStatus.color() = when (this) {
    TaskStatus.TODO -> AppColors.Warning
    TaskStatus.IN_PROGRESS -> AppColors.Primary
    TaskStatus.DONE -> AppColors.Success
}

@Composable
private fun BoardCard(task: Task, status: TaskStatus, canEdit: Boolean, onMove: () -> Unit, onEdit: () -> Unit) {
    AppCard(if (canEdit) Modifier.tactileClickable(onEdit) else Modifier) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (status == TaskStatus.DONE) AppColors.TextMuted else AppColors.TextPrimary,
                    textDecoration = if (status == TaskStatus.DONE) TextDecoration.LineThrough else null,
                )
                AppTag(task.priority.label(), task.priority.color())
            }
            if (canEdit) {
                AppTextButton(onClick = onMove) {
                    Text(status.moveLabel(), fontWeight = FontWeight.Bold)
                    Icon(AppIcons.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                }
            }
        }
    }
}
