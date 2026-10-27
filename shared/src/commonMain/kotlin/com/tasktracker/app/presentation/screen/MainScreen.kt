package com.tasktracker.app.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tasktracker.app.domain.model.Role
import com.tasktracker.app.domain.model.TaskPriority
import com.tasktracker.app.domain.model.Workspace
import com.tasktracker.app.presentation.navigation.DeepLinkController
import com.tasktracker.app.presentation.theme.AppButton
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppIcons
import com.tasktracker.app.presentation.theme.AppTextButton
import com.tasktracker.app.presentation.theme.PlatformBackHandler
import com.tasktracker.app.presentation.theme.RoleBadge
import com.tasktracker.app.presentation.theme.SectionHeader
import com.tasktracker.app.presentation.theme.ThemeController
import com.tasktracker.app.presentation.theme.WorkspaceMark
import com.tasktracker.app.presentation.theme.tactileClickable
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import com.tasktracker.app.presentation.viewmodel.InvitesViewModel
import com.tasktracker.app.presentation.viewmodel.NotificationsViewModel
import com.tasktracker.app.presentation.viewmodel.TaskListViewModel
import com.tasktracker.app.presentation.viewmodel.WorkspaceViewModel
import org.koin.compose.viewmodel.koinViewModel

private enum class MainTab(val label: String, val icon: ImageVector) {
    Tasks("Tasks", AppIcons.Tasks),
    Boards("Boards", AppIcons.Board),
    Members("Members", AppIcons.People),
    Invites("Invites", AppIcons.Mail),
    Profile("Profile", AppIcons.Person),
}

class MainScreen : Screen {

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel = koinViewModel<AuthViewModel>()
        val taskViewModel = koinViewModel<TaskListViewModel>()
        val workspaceViewModel = koinViewModel<WorkspaceViewModel>()
        val invitesViewModel = koinViewModel<InvitesViewModel>()
        val notificationsViewModel = koinViewModel<NotificationsViewModel>()

        val authState by authViewModel.uiState.collectAsStateWithLifecycle()
        val taskState by taskViewModel.uiState.collectAsStateWithLifecycle()
        val workspaceState by workspaceViewModel.uiState.collectAsStateWithLifecycle()
        val invitesState by invitesViewModel.uiState.collectAsStateWithLifecycle()
        val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()

        var tabIndex by rememberSaveable { mutableStateOf(MainTab.Tasks.ordinal) }
        val tab = MainTab.entries[tabIndex]
        var showSwitcher by rememberSaveable { mutableStateOf(false) }
        var showCreate by rememberSaveable { mutableStateOf(false) }
        var renameTarget by remember { mutableStateOf<Workspace?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }

        val uid = authState.user?.uid
        val workspace = workspaceState.current
        val myRole = workspace?.roleOf(uid) ?: Role.GUEST
        val darkTheme = ThemeController.isDark()
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        // Back from any other tab returns to Tasks first; only from Tasks does it leave the app.
        PlatformBackHandler(enabled = tab != MainTab.Tasks) { tabIndex = MainTab.Tasks.ordinal }

        // "Open task" from a notification: show the Tasks tab and make sure its workspace is the active one.
        val pendingTask = DeepLinkController.pendingTask
        LaunchedEffect(pendingTask) {
            val target = pendingTask ?: return@LaunchedEffect
            tabIndex = MainTab.Tasks.ordinal
            if (workspace?.id != target.workspaceId) workspaceViewModel.switchWorkspace(target.workspaceId)
        }
        // Signed out (here or elsewhere) -> back to the auth flow.
        LaunchedEffect(authState.isSignedIn) {
            if (!authState.isSignedIn) navigator.replaceAll(AuthScreen())
        }
        // One snackbar host for every ViewModel's errors and confirmations.
        LaunchedEffect(taskState.error) {
            taskState.error?.let { snackbarHostState.showSnackbar(it); taskViewModel.clearError() }
        }
        LaunchedEffect(workspaceState.error) {
            workspaceState.error?.let { snackbarHostState.showSnackbar(it); workspaceViewModel.clearError() }
        }
        LaunchedEffect(notificationsState.error) {
            notificationsState.error?.let { snackbarHostState.showSnackbar(it); notificationsViewModel.clearError() }
        }
        LaunchedEffect(invitesState.error, invitesState.message) {
            (invitesState.error ?: invitesState.message)?.let {
                snackbarHostState.showSnackbar(it)
                invitesViewModel.clearFeedback()
            }
        }
        LaunchedEffect(authState.message, authState.error) {
            (authState.error ?: authState.message)?.let { snackbarHostState.showSnackbar(it); authViewModel.clearError() }
        }

        Scaffold(
            containerColor = AppColors.Background,
            topBar = {
                MainTopBar(
                    workspace = workspace,
                    role = myRole,
                    darkTheme = darkTheme,
                    onOpenSwitcher = { showSwitcher = true },
                    onToggleTheme = { ThemeController.setDark(!darkTheme) },
                )
            },
            bottomBar = {
                // The keyboard replaces the bar, so inputs are never squeezed between the two.
                if (!imeVisible) {
                    FloatingBottomBar(
                        selected = tab,
                        onSelect = { tabIndex = it.ordinal },
                        badges = mapOf(MainTab.Invites to invitesState.pendingReceivedCount + notificationsState.unreadCount),
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                when (tab) {
                    MainTab.Tasks -> TasksTab(
                        state = taskState,
                        workspace = workspace,
                        canEdit = myRole.canEditTasks,
                        onAdd = { title, priority: TaskPriority -> taskViewModel.addTask(title, priority = priority) },
                        onToggle = taskViewModel::toggleCompleted,
                        onDelete = taskViewModel::deleteTask,
                        onEdit = taskViewModel::editTask,
                        openTaskId = pendingTask?.takeIf { it.workspaceId == workspace?.id }?.taskId,
                        onOpenTaskHandled = { DeepLinkController.pendingTask = null },
                    )
                    MainTab.Boards -> BoardsTab(
                        state = taskState,
                        workspace = workspace,
                        canEdit = myRole.canEditTasks,
                        onMove = taskViewModel::setStatus,
                        onEdit = taskViewModel::editTask,
                    )
                    MainTab.Members -> MembersTab(
                        workspace = workspace,
                        myRole = myRole,
                        selfUid = uid,
                        selfEmail = authState.user?.email,
                        selfName = authState.user?.displayName,
                        invitesState = invitesState,
                        onInvite = { email, role -> workspace?.let { invitesViewModel.invite(it, email, role) } },
                        onClearInviteError = invitesViewModel::clearInviteError,
                        onChangeRole = { memberUid, role -> workspace?.let { workspaceViewModel.setMemberRole(it.id, memberUid, role) } },
                        onLeave = { workspace?.let { workspaceViewModel.leaveWorkspace(it.id) } },
                        onRemoveMember = { memberUid -> workspace?.let { workspaceViewModel.removeMember(it.id, memberUid) } },
                    )
                    MainTab.Invites -> InvitesTab(
                        invites = invitesState,
                        activity = notificationsState,
                        onAccept = invitesViewModel::accept,
                        onDecline = invitesViewModel::decline,
                        onMarkRead = notificationsViewModel::markRead,
                        onMarkAllRead = notificationsViewModel::markAllRead,
                        onClearAll = notificationsViewModel::clearAll,
                    )
                    MainTab.Profile -> ProfileTab(
                        user = authState.user,
                        isSaving = authState.isSavingProfile,
                        darkTheme = darkTheme,
                        onSaveName = authViewModel::updateDisplayName,
                        onToggleTheme = ThemeController::setDark,
                        onSignOut = authViewModel::signOut,
                    )
                }
            }
        }

        if (showSwitcher) {
            WorkspaceSwitcherSheet(
                workspaces = workspaceState.workspaces,
                currentId = workspace?.id,
                uid = uid,
                onDismiss = { showSwitcher = false },
                onSwitch = { workspaceViewModel.switchWorkspace(it) },
                onCreate = { showSwitcher = false; showCreate = true },
                onRename = { showSwitcher = false; renameTarget = it },
            )
        }
        renameTarget?.let { target ->
            NameDialog(
                title = "Rename workspace",
                label = "Workspace name",
                confirm = "Save",
                initial = target.name,
                onDismiss = { renameTarget = null },
                onConfirm = { workspaceViewModel.renameWorkspace(target.id, it); renameTarget = null },
            )
        }
        if (showCreate) {
            NameDialog(
                title = "New workspace",
                label = "Workspace name",
                confirm = "Create",
                initial = "",
                onDismiss = { showCreate = false },
                onConfirm = { workspaceViewModel.createWorkspace(it); showCreate = false },
            )
        }
    }
}

@Composable
private fun MainTopBar(
    workspace: Workspace?,
    role: Role,
    darkTheme: Boolean,
    onOpenSwitcher: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Column(Modifier.background(AppColors.Surface.copy(alpha = 0.96f))) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .tactileClickable(onOpenSwitcher)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WorkspaceMark(workspace?.name ?: "…", size = 36.dp)
                Column(Modifier.weight(1f, fill = false)) {
                    Text("Workspace", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
                    Text(
                        workspace?.name ?: "Loading…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(AppIcons.ChevronDown, contentDescription = "Switch workspace", tint = AppColors.TextMuted, modifier = Modifier.size(22.dp))
            }
            if (workspace != null) RoleBadge(role)
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (darkTheme) AppIcons.LightMode else AppIcons.DarkMode,
                    contentDescription = if (darkTheme) "Switch to light mode" else "Switch to dark mode",
                    tint = AppColors.TextMuted,
                )
            }
        }
        HorizontalDivider(color = AppColors.Border)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceSwitcherSheet(
    workspaces: List<Workspace>,
    currentId: String?,
    uid: String?,
    onDismiss: () -> Unit,
    onSwitch: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (Workspace) -> Unit,
) {
    val (owned, joined) = workspaces.partition { it.ownerId == uid }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Background,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Switch workspace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            SectionHeader("Owned · ${owned.size}")
            owned.forEach { WorkspaceRow(it, uid, it.id == currentId, onRename = { onRename(it) }) { onSwitch(it.id); onDismiss() } }
            SectionHeader("Joined · ${joined.size}")
            if (joined.isEmpty()) {
                Text("Workspaces you're invited to will appear here.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted, modifier = Modifier.padding(vertical = 8.dp))
            }
            joined.forEach { WorkspaceRow(it, uid, it.id == currentId, onRename = { onRename(it) }) { onSwitch(it.id); onDismiss() } }
            Spacer(Modifier.height(12.dp))
            AppButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Create workspace", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WorkspaceRow(workspace: Workspace, uid: String?, isCurrent: Boolean, onRename: () -> Unit, onClick: () -> Unit) {
    val role = workspace.roleOf(uid)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isCurrent) AppColors.Primary.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.5.dp, if (isCurrent) AppColors.Primary else Color.Transparent, RoundedCornerShape(18.dp))
            .tactileClickable(onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WorkspaceMark(workspace.name, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(workspace.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${workspace.members.size} ${if (workspace.members.size == 1) "member" else "members"}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
        RoleBadge(role)
        if (isCurrent) Icon(AppIcons.Check, contentDescription = "Active workspace", tint = AppColors.Primary, modifier = Modifier.size(22.dp))
        if (role.canInvite) {
            IconButton(onClick = onRename, modifier = Modifier.size(48.dp)) {
                Icon(AppIcons.Edit, contentDescription = "Rename ${workspace.name}", tint = AppColors.TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    confirm: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val valid = name.isNotBlank() && name.trim() != initial.trim()
    val submit = { if (valid) onConfirm(name.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { AppTextButton(onClick = submit, enabled = valid) { Text(confirm) } },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Floating pill; the selected item gets a tinted pill and an animated curve under its icon. */
@Composable
private fun FloatingBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit, badges: Map<MainTab, Int>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(32.dp), ambientColor = AppColors.Primary, spotColor = AppColors.Primary)
                .background(AppColors.Surface, RoundedCornerShape(32.dp))
                .border(1.dp, AppColors.Border, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { item ->
                BottomBarItem(item, selected = item == selected, badge = badges[item] ?: 0, onClick = { onSelect(item) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BottomBarItem(tab: MainTab, selected: Boolean, badge: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(if (selected) AppColors.Primary else AppColors.TextMuted, label = "tabTint")
    val pill by animateColorAsState(if (selected) AppColors.Primary.copy(alpha = 0.12f) else Color.Transparent, label = "tabPill")
    val curveWidth by animateDpAsState(if (selected) 18.dp else 0.dp, label = "tabCurve")

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(pill)
            .tactileClickable(onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            // The label below already names the tab, so the icon stays silent for screen readers.
            Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            if (badge > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd).padding(start = 14.dp),
                    containerColor = AppColors.Danger,
                    contentColor = Color.White,
                ) { Text(if (badge > 9) "9+" else badge.toString()) }
            }
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(2.dp))
        Box(Modifier.width(curveWidth).height(3.dp).background(AppColors.Primary, RoundedCornerShape(2.dp)))
    }
}
