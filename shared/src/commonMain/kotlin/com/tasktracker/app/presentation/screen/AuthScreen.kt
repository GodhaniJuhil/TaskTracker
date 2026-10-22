package com.tasktracker.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tasktracker.app.domain.model.User
import com.tasktracker.app.presentation.viewmodel.AuthUiState
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

class AuthScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<AuthViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        // Signed in and verified -> leave the auth flow. Driven purely by the StateFlow.
        LaunchedEffect(state.isSignedIn, state.needsEmailVerification) {
            if (state.isSignedIn && !state.needsEmailVerification) {
                navigator.replaceAll(TaskListScreen())
            }
        }

        AuthContent(
            state = state,
            onSignIn = viewModel::signIn,
            onRegister = viewModel::register,
            onGoogleSignIn = viewModel::signInWithGoogle,
            onResendVerification = viewModel::resendVerificationEmail,
            onRefreshVerification = viewModel::refreshVerificationStatus,
            onSignOut = viewModel::signOut,
        )
    }
}

@Composable
private fun AuthContent(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Task Tracker", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        val user = state.user
        if (user != null && state.needsEmailVerification) {
            VerificationPanel(user, state, onResendVerification, onRefreshVerification, onSignOut)
        } else {
            CredentialsForm(state, onSignIn, onRegister, onGoogleSignIn)
        }

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CredentialsForm(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val canSubmit = email.isNotBlank() && password.length >= 6 && !state.isLoading

    Text(
        if (isRegistering) "Create an account" else "Sign in",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it.trim() },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (min. 6 characters)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { if (isRegistering) onRegister(email, password) else onSignIn(email, password) },
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
        } else {
            Text(if (isRegistering) "Register" else "Sign in")
        }
    }
    TextButton(onClick = { isRegistering = !isRegistering }) {
        Text(if (isRegistering) "Already have an account? Sign in" else "No account? Register")
    }

    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    OutlinedButton(
        onClick = onGoogleSignIn,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Continue with Google")
    }
}

@Composable
private fun VerificationPanel(
    user: User,
    state: AuthUiState,
    onResend: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    Text("Verify your email", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        if (state.verificationEmailSent) {
            "We sent a verification link to ${user.email}. Open it, then come back and refresh."
        } else {
            "${user.email} is not verified yet."
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text("I've verified - refresh")
    }
    OutlinedButton(onClick = onResend, modifier = Modifier.fillMaxWidth()) {
        Text("Resend verification email")
    }
    TextButton(onClick = onSignOut) { Text("Use a different account") }
}
