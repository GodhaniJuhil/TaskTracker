package com.tasktracker.app.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tasktracker.app.domain.model.User
import com.tasktracker.app.presentation.theme.AppButton
import com.tasktracker.app.presentation.theme.AppColors
import com.tasktracker.app.presentation.theme.AppOutlinedButton
import com.tasktracker.app.presentation.theme.AppTextButton
import com.tasktracker.app.presentation.theme.tactileClickable
import com.tasktracker.app.presentation.viewmodel.AuthUiState
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.tasktracker.app.presentation.theme.AppIcons

class AuthScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<AuthViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        // Signed in and verified -> leave the auth flow. Driven purely by the StateFlow.
        LaunchedEffect(state.isSignedIn, state.needsEmailVerification) {
            if (state.isSignedIn && !state.needsEmailVerification) {
                navigator.replaceAll(MainScreen())
            }
        }
        // Coming back from the mail app: pick up a completed verification without a manual refresh.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            if (state.needsEmailVerification) viewModel.refreshVerificationStatus()
        }

        AuthContent(
            state = state,
            onSignIn = viewModel::signIn,
            onRegister = viewModel::register,
            onGoogleSignIn = viewModel::signInWithGoogle,
            onForgotPassword = viewModel::forgotPassword,
            onClearError = viewModel::clearError,
            onResendVerification = viewModel::resendVerificationEmail,
            onRefreshVerification = viewModel::refreshVerificationStatus,
            onSignOut = viewModel::signOut,
        )
    }
}

/** A rectangle whose bottom edge sags into a smooth curve. */
private object CurvedHeaderShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val sag = size.height * 0.22f
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - sag)
            quadraticTo(size.width / 2f, size.height + sag * 0.6f, 0f, size.height - sag)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun AuthContent(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onClearError: () -> Unit,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandHeader()

        // Pulled up over the header's curve so the glass card appears to float on it.
        Column(
            modifier = Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = AppColors.Primary, spotColor = AppColors.Primary)
                .clip(RoundedCornerShape(28.dp))
                .background(AppColors.Surface.copy(alpha = 0.88f))
                .border(1.dp, AppColors.Border, RoundedCornerShape(28.dp))
                .padding(20.dp),
        ) {
            val user = state.user
            if (user != null && state.needsEmailVerification) {
                VerificationPanel(user, state, onResendVerification, onRefreshVerification, onSignOut)
            } else {
                CredentialsForm(state, onSignIn, onRegister, onGoogleSignIn, onForgotPassword, onClearError)
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            state.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = AppColors.Success, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BrandHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(CurvedHeaderShape)
            .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF4F46E5), Color(0xFF0EA5E9)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.statusBarsPadding().padding(bottom = 24.dp)) {
            // Glowing logo: a soft radial halo behind a white disc.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
                                center = center,
                                radius = size.minDimension * 0.95f,
                            ),
                            radius = size.minDimension * 0.95f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Check, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Task Tracker", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Plan together, ship together", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CredentialsForm(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val canSubmit = email.isNotBlank() && password.length >= 6 && !state.isLoading
    val submit = {
        focus.clearFocus()
        val trimmedEmail = email.trim()
        if (isRegistering) onRegister(trimmedEmail, password) else onSignIn(trimmedEmail, password)
    }

    AuthTabs(isRegistering = isRegistering, onSelect = { isRegistering = it; onClearError() })
    Spacer(Modifier.height(20.dp))
    AuthField(
        value = email,
        onValueChange = { email = it; if (state.error != null) onClearError() },
        label = "Email",
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
        onImeAction = { focus.moveFocus(FocusDirection.Down) },
    )
    Spacer(Modifier.height(12.dp))
    AuthField(
        value = password,
        onValueChange = { password = it; if (state.error != null) onClearError() },
        label = "Password",
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        onImeAction = { if (canSubmit) submit() },
        isPassword = !showPassword,
        supportingText = if (isRegistering) "At least 6 characters" else null,
        trailing = {
            IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (showPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password",
                    tint = AppColors.TextMuted,
                )
            }
        },
    )
    if (!isRegistering) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            AppTextButton(onClick = { onForgotPassword(email) }) { Text("Forgot password?", fontWeight = FontWeight.SemiBold) }
        }
    } else {
        Spacer(Modifier.height(8.dp))
    }
    AppButton(onClick = submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AppColors.OnPrimary)
        } else {
            Text(if (isRegistering) "Create account" else "Sign in", fontWeight = FontWeight.SemiBold)
        }
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = AppColors.Border)
        Text("  or  ", color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(Modifier.weight(1f), color = AppColors.Border)
    }
    AppOutlinedButton(onClick = onGoogleSignIn, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
        Text("Continue with Google", color = AppColors.TextPrimary)
    }
}

/** Segmented Login / Register switch with a sliding indicator. */
@Composable
private fun AuthTabs(isRegistering: Boolean, onSelect: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.SurfaceVariant)
            .padding(4.dp),
    ) {
        BoxWithConstraints {
            val tabWidth: Dp = maxWidth / 2
            val indicatorOffset by animateDpAsState(if (isRegistering) tabWidth else 0.dp, label = "tabIndicator")
            Box(
                Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(44.dp)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(AppColors.Surface, RoundedCornerShape(12.dp)),
            )
            Row {
                AuthTab("Login", selected = !isRegistering, modifier = Modifier.width(tabWidth)) { onSelect(false) }
                AuthTab("Register", selected = isRegistering, modifier = Modifier.width(tabWidth)) { onSelect(true) }
            }
        }
    }
}

@Composable
private fun AuthTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) AppColors.Primary else AppColors.TextMuted, label = "tabColor")
    Box(
        modifier = modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).tactileClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    isPassword: Boolean = false,
    supportingText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
        trailingIcon = trailing,
        supportingText = supportingText?.let { { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = AppColors.Border,
            focusedContainerColor = AppColors.Surface,
            unfocusedContainerColor = AppColors.Surface.copy(alpha = 0.7f),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun VerificationPanel(
    user: User,
    state: AuthUiState,
    onResend: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    Text("Verify your email", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        if (state.verificationEmailSent) {
            "We sent a verification link to ${user.email}. Open it, then come back to continue."
        } else {
            "${user.email} is not verified yet."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.TextMuted,
    )
    Spacer(Modifier.height(16.dp))
    AppButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("I've verified, continue") }
    Spacer(Modifier.height(8.dp))
    AppOutlinedButton(onClick = onResend, modifier = Modifier.fillMaxWidth()) {
        Text("Resend verification email", color = AppColors.TextPrimary)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AppTextButton(onClick = onSignOut) { Text("Use a different account") }
    }
}
