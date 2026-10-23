package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.tasktracker.app.domain.model.AppNotification
import com.tasktracker.app.domain.model.Invitation
import com.tasktracker.app.domain.model.InvitationStatus
import com.tasktracker.app.domain.model.NotificationType
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.presentation.theme.AppButton
import com.tasktracker.app.presentation.theme.AppCard
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppIcons
import com.tasktracker.app.presentation.theme.AppOutlinedButton
import com.tasktracker.app.presentation.theme.AppTag
import com.tasktracker.app.presentation.theme.AppTextButton
import com.tasktracker.app.presentation.theme.EmptyState
import com.tasktracker.app.presentation.theme.RoleBadge
import com.tasktracker.app.presentation.theme.SectionHeader
import com.tasktracker.app.presentation.theme.SkeletonCard
import com.tasktracker.app.presentation.theme.WorkspaceMark
import com.tasktracker.app.presentation.theme.tactileClickable
import com.tasktracker.app.presentation.viewmodel.InvitesUiState
import com.tasktracker.app.presentation.viewmodel.NotificationsUiState
import kotlin.time.Clock

/** Pending invitations first, then the activity log. */
@Composable
fun InvitesTab(
    invites: InvitesUiState,
    activity: NotificationsUiState,
    onAccept: (Invitation) -> Unit,
    onDecline: (Invitation) -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    // Pending ones first so the actionable cards are never buried under answered ones.
    val received = invites.received.sortedBy { it.status != InvitationStatus.PENDING }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Inbox", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Invitations and what changed in your workspaces", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
            }
        }

        item { SectionHeader("Invitations") }
        if (received.isEmpty()) {
            item { EmptyState(AppIcons.Mail, "No invitations", "When someone invites you to a workspace, it shows up here.") }
        } else {
            items(received, key = { "r_" + it.id }) { invitation ->
                ReceivedItem(invitation, onAccept = { onAccept(invitation) }, onDecline = { onDecline(invitation) })
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(
                    if (activity.unreadCount > 0) "Activity · ${activity.unreadCount} unread" else "Activity",
                    modifier = Modifier.weight(1f),
                )
                AppTextButton(onClick = onMarkAllRead, enabled = activity.unreadCount > 0) { Text("Mark all read") }
                AppTextButton(onClick = { confirmClear = true }, enabled = activity.notifications.isNotEmpty()) {
                    Text("Clear", color = if (activity.notifications.isNotEmpty()) AppColors.Danger else AppColors.TextMuted)
                }
            }
        }
        when {
            activity.isLoading -> items(2) { SkeletonCard() }
            activity.notifications.isEmpty() -> item {
                EmptyState(AppIcons.Bell, "No activity yet", "Task updates and invitations show up here.")
            }
            else -> items(activity.notifications, key = { "n_" + it.id }) { NotificationItem(it, onClick = { onMarkRead(it.id) }) }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all activity?") },
            text = { Text("This removes every entry from your activity log. It can't be undone.") },
            confirmButton = { AppTextButton(onClick = { confirmClear = false; onClearAll() }) { Text("Clear all", color = AppColors.Danger) } },
            dismissButton = { AppTextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ReceivedItem(invitation: Invitation, onAccept: () -> Unit, onDecline: () -> Unit) {
    val role = Role.entries.firstOrNull { it.name == invitation.role } ?: Role.MEMBER
    AppCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WorkspaceMark(invitation.workspaceName, size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(invitation.workspaceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Invited by ${invitation.senderEmail}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
                if (invitation.status != InvitationStatus.PENDING) StatusChip(invitation.status)
            }
            if (invitation.status == InvitationStatus.PENDING) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You would join as", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    RoleBadge(role)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppOutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("Decline", color = AppColors.Danger) }
                    AppButton(onClick = onAccept, modifier = Modifier.weight(1.4f)) { Text("Accept", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: InvitationStatus) {
    val (label, color) = when (status) {
        InvitationStatus.PENDING -> "Pending" to AppColors.Warning
        InvitationStatus.ACCEPTED -> "Accepted" to AppColors.Success
        InvitationStatus.DECLINED -> "Declined" to AppColors.Danger
    }
    AppTag(label, color)
}

@Composable
private fun NotificationItem(notification: AppNotification, onClick: () -> Unit) {
    AppCard(Modifier.tactileClickable(onClick)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(if (notification.isRead) AppColors.Border else AppColors.Primary, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                )
                Text(notification.body, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                Text(
                    (if (notification.type == NotificationType.INVITE) "Invitation" else "Task") + " · " + timeAgo(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextMuted,
                )
            }
        }
    }
}

private fun timeAgo(epochMillis: Long): String {
    val minutes = (Clock.System.now().toEpochMilliseconds() - epochMillis) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}
