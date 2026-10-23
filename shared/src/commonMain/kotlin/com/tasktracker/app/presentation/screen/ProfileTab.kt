package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tasktracker.app.domain.model.User
import com.tasktracker.app.presentation.theme.AppButton
import com.tasktracker.app.presentation.theme.AppCard
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppIcons
import com.tasktracker.app.presentation.theme.AppOutlinedButton
import com.tasktracker.app.presentation.theme.AppTag
import com.tasktracker.app.presentation.theme.AppTextButton
import com.tasktracker.app.presentation.theme.Avatar

@Composable
fun ProfileTab(
    user: User?,
    isSaving: Boolean,
    darkTheme: Boolean,
    onSaveName: (String) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val savedName = user?.displayName.orEmpty()
    var name by rememberSaveable(savedName) { mutableStateOf(savedName) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val dirty = name.trim().isNotEmpty() && name.trim() != savedName

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(name.ifBlank { user?.email.orEmpty() }, size = 96.dp)
            Text("Your initials are shown until photo upload is available.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display name") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); if (dirty) onSaveName(name) }),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = user?.email.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Email") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            trailingIcon = { if (user?.isEmailVerified == true) AppTag("Verified", AppColors.Success, Modifier.padding(end = 12.dp)) },
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton(onClick = { focus.clearFocus(); onSaveName(name) }, enabled = dirty && !isSaving, modifier = Modifier.fillMaxWidth()) {
            if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AppColors.OnPrimary)
            else Text("Save changes", fontWeight = FontWeight.SemiBold)
        }

        AppCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(if (darkTheme) AppIcons.DarkMode else AppIcons.LightMode, contentDescription = null, tint = AppColors.Primary)
                Column(Modifier.weight(1f)) {
                    Text("Dark mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (darkTheme) "On" else "Off", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
                Switch(
                    checked = darkTheme,
                    onCheckedChange = onToggleTheme,
                    colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Primary, checkedThumbColor = AppColors.OnPrimary),
                )
            }
        }

        AppOutlinedButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(AppIcons.Logout, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(20.dp))
            Text("  Sign out", color = AppColors.Danger, fontWeight = FontWeight.SemiBold)
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to see your workspaces.") },
            confirmButton = { AppTextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sign out", color = AppColors.Danger) } },
            dismissButton = { AppTextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}
