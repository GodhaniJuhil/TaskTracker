package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tasktracker.app.domain.model.Task
import com.tasktracker.app.domain.model.TaskChanges
import com.tasktracker.app.domain.model.TaskPriority
import com.tasktracker.app.domain.model.TaskStatus
import com.tasktracker.app.presentation.theme.AppChip
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppTextButton

internal fun TaskStatus.label() = when (this) {
    TaskStatus.TODO -> "To do"
    TaskStatus.IN_PROGRESS -> "In progress"
    TaskStatus.DONE -> "Done"
}

/** Edits title, description, priority and status. Only the fields that differ are reported in [onSave]. */
@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onSave: (TaskChanges) -> Unit) {
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var description by rememberSaveable(task.id) { mutableStateOf(task.description) }
    var priorityIndex by rememberSaveable(task.id) { mutableStateOf(task.priority.ordinal) }
    var statusIndex by rememberSaveable(task.id) { mutableStateOf(task.currentStatus.ordinal) }
    val priority = TaskPriority.entries[priorityIndex]
    val status = TaskStatus.entries[statusIndex]

    val changes = TaskChanges(
        title = title.trim().takeIf { it != task.title },
        description = description.trim().takeIf { it != task.description },
        priority = priority.takeIf { it != task.priority },
        status = status.takeIf { it != task.currentStatus },
    )
    val dirty = changes != TaskChanges()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    isError = title.isBlank(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 5,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge, color = AppColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p -> AppChip(p.label(), selected = p == priority, onClick = { priorityIndex = p.ordinal }) }
                }
                Text("Status", style = MaterialTheme.typography.labelLarge, color = AppColors.TextMuted)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskStatus.entries.forEach { s -> AppChip(s.label(), selected = s == status, onClick = { statusIndex = s.ordinal }) }
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = { onSave(changes) }, enabled = dirty && title.isNotBlank()) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
