package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tasktracker.app.domain.model.Invitation
import com.tasktracker.app.domain.model.InvitationStatus
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.domain.model.WorkspaceMember
import com.tasktracker.app.presentation.theme.AppButton
import com.tasktracker.app.presentation.theme.AppCard
import com.tasktracker.app.presentation.theme.AppChip
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppIcons
import com.tasktracker.app.presentation.theme.AppTag
import com.tasktracker.app.presentation.theme.AppTextButton
import com.tasktracker.app.presentation.theme.Avatar
import com.tasktracker.app.presentation.theme.RoleBadge
import com.tasktracker.app.presentation.theme.tactileClickable
import com.tasktracker.app.presentation.viewmodel.InvitesUiState

/** Owners can remove anyone; admins can remove members and guests. The owner is never removable. */
private fun canRemove(myRole: Role, target: Role): Boolean = when (myRole) {
    Role.OWNER -> target != Role.OWNER
    Role.ADMIN -> target == Role.MEMBER || target == Role.GUEST
    else -> false
}

/** Roles an owner can hand out. Ownership itself can't be transferred from the app yet. */
private val AssignableRoles = listOf(Role.ADMIN, Role.MEMBER, Role.GUEST)

private fun displayName(member: WorkspaceMember, selfUid: String?, selfName: String?): String = when {
    member.uid == selfUid && !selfName.isNullOrBlank() -> selfName
    member.email.isNotBlank() -> member.email.substringBefore('@').replace('.', ' ').replace('_', ' ')
        .split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    else -> "Member"
}

@Composable
fun MembersTab(
    workspace: Workspace?,
    myRole: Role,
    selfUid: String?,
    selfEmail: String?,
    selfName: String?,
    invitesState: InvitesUiState,
    onInvite: (email: String, role: Role) -> Unit,
    onClearInviteError: () -> Unit,
    onChangeRole: (uid: String, role: Role) -> Unit,
    onLeave: () -> Unit,
    onRemoveMember: (uid: String) -> Unit,
) {
    if (workspace == null) return
    var showInvite by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val members = workspace.memberList()
    val pending = invitesState.sent.filter { it.workspaceId == workspace.id && it.status == InvitationStatus.PENDING }
    val canInvite = myRole.canInvite
    var memberToRemove by remember { mutableStateOf<WorkspaceMember?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Team", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${members.size} ${if (members.size == 1) "person" else "people"} in ${workspace.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (canInvite) {
                    AppButton(onClick = { showInvite = true }) {
                        Icon(AppIcons.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("  Invite", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (!myRole.canInvite) {
            item { Hint("Only owners and admins can invite people.") }
        } else if (!myRole.canManageRoles) {
            item { Hint("Only the owner can change roles.") }
        }

        items(members, key = { it.uid }) { member ->
            val isSelf = member.uid == selfUid
            val email = member.email.ifBlank { if (isSelf) selfEmail.orEmpty() else "" }
            MemberRow(
                name = displayName(member, selfUid, selfName),
                email = email,
                isSelf = isSelf,
                role = member.role,
                canEditRole = myRole.canManageRoles && member.role != Role.OWNER && !isSelf,
                onRoleSelected = { onChangeRole(member.uid, it) },
                canRemove = !isSelf && canRemove(myRole, member.role),
                onRemove = { memberToRemove = member },
            )
        }
        items(pending, key = { "p_" + it.id }) { invitation -> PendingRow(invitation) }

        if (myRole != Role.OWNER) {
            item {
                AppTextButton(onClick = { confirmLeave = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Leave workspace", color = AppColors.Danger, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showInvite) {
        InviteDialog(
            workspaceName = workspace.name,
            state = invitesState,
            onDismiss = { showInvite = false; onClearInviteError() },
            onInvite = onInvite,
            onClearError = onClearInviteError,
        )
    }
    memberToRemove?.let { member ->
        val name = displayName(member, selfUid, selfName)
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove $name?") },
            text = { Text("They will lose access to ${workspace.name} and its tasks. You can invite them again later.") },
            confirmButton = { AppTextButton(onClick = { memberToRemove = null; onRemoveMember(member.uid) }) { Text("Remove", color = AppColors.Danger) } },
            dismissButton = { AppTextButton(onClick = { memberToRemove = null }) { Text("Cancel") } },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave ${workspace.name}?") },
            text = { Text("You will lose access to its tasks. You can rejoin only if someone invites you again.") },
            confirmButton = { AppTextButton(onClick = { confirmLeave = false; onLeave() }) { Text("Leave", color = AppColors.Danger) } },
            dismissButton = { AppTextButton(onClick = { confirmLeave = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
}

@Composable
private fun MemberRow(
    name: String,
    email: String,
    isSelf: Boolean,
    role: Role,
    canEditRole: Boolean,
    onRoleSelected: (Role) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    AppCard {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(name, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (isSelf) "$name (you)" else name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Not truncated to one line: a long address wraps instead of hiding its domain.
                Text(
                    email.ifBlank { "Email shows once they next open the app" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    fontStyle = if (email.isBlank()) FontStyle.Italic else FontStyle.Normal,
                )
            }
            if (canEditRole) RoleDropdown(role, onRoleSelected) else RoleBadge(role)
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcons.Close, contentDescription = "Remove $name", tint = AppColors.Danger, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun RoleDropdown(role: Role, onSelected: (Role) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .tactileClickable { open = true }
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoleBadge(role)
            Icon(AppIcons.ChevronDown, contentDescription = "Change role", tint = AppColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AssignableRoles.forEach { r ->
                DropdownMenuItem(
                    text = { Text(r.name, fontWeight = if (r == role) FontWeight.Bold else FontWeight.Normal) },
                    trailingIcon = { if (r == role) Icon(AppIcons.Check, contentDescription = "Current role", tint = AppColors.Primary, modifier = Modifier.size(18.dp)) },
                    onClick = { open = false; if (r != role) onSelected(r) },
                )
            }
        }
    }
}

@Composable
private fun PendingRow(invitation: Invitation) {
    AppCard {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(invitation.receiverEmail, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(invitation.receiverEmail, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Invited as ${invitation.role.lowercase()}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            }
            AppTag("Pending", AppColors.Warning)
        }
    }
}

@Composable
private fun InviteDialog(
    workspaceName: String,
    state: InvitesUiState,
    onDismiss: () -> Unit,
    onInvite: (String, Role) -> Unit,
    onClearError: () -> Unit,
) {
    // The email stays in the field after an error so it can be corrected instead of retyped.
    var email by rememberSaveable { mutableStateOf("") }
    var roleIndex by rememberSaveable { mutableStateOf(Role.MEMBER.ordinal) }
    val role = Role.entries[roleIndex]
    val startCount = remember { state.invitesSent }
    LaunchedEffect(state.invitesSent) { if (state.invitesSent != startCount) onDismiss() }
    val submit = { if (email.isNotBlank() && !state.isSending) onInvite(email, role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite to $workspaceName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; if (state.inviteError != null) onClearError() },
                    label = { Text("Teammate's email") },
                    singleLine = true,
                    isError = state.inviteError != null,
                    supportingText = state.inviteError?.let { { Text(it) } },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Role", style = MaterialTheme.typography.labelLarge, color = AppColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssignableRoles.forEach { r -> AppChip(r.name.lowercase().replaceFirstChar { it.uppercase() }, selected = r == role, onClick = { roleIndex = r.ordinal }) }
                }
                Text(
                    when (role) {
                        Role.ADMIN -> "Admins can invite people and manage tasks."
                        Role.GUEST -> "Guests can view tasks but not change them."
                        else -> "Members can add and complete tasks."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
        },
        confirmButton = {
            AppTextButton(onClick = submit, enabled = email.isNotBlank() && !state.isSending) {
                if (state.isSending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Send invitation", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
