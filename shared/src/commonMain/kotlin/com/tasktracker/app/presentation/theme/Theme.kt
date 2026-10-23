package com.tasktracker.app.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
class AppPalette(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val isDark: Boolean,
)

private val LightPalette = AppPalette(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    border = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textMuted = Color(0xFF64748B),
    success = Color(0xFF059669),
    warning = Color(0xFFB45309),
    danger = Color(0xFFE11D48),
    isDark = false,
)

private val DarkPalette = AppPalette(
    primary = Color(0xFF5B8DEF),
    onPrimary = Color(0xFF0B1222),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1A2438),
    surfaceVariant = Color(0xFF243049),
    border = Color(0xFF2B3A56),
    textPrimary = Color(0xFFF1F5F9),
    textMuted = Color(0xFF94A3B8),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
    isDark = true,
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Theme-aware colours; every property resolves against the palette of the enclosing [AppTheme]. */
object AppColors {
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primary
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onPrimary
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primary
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
    val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceVariant
    val Border: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.border
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textPrimary
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textMuted
    val Success: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.success
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warning
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.danger
}

enum class ThemeMode { System, Light, Dark }

/** In-memory choice of the user; follows the system until the toggle is used. */
object ThemeController {
    var mode by mutableStateOf(ThemeMode.System)

    @Composable
    fun isDark(): Boolean = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    fun setDark(dark: Boolean) {
        mode = if (dark) ThemeMode.Dark else ThemeMode.Light
    }
}

private fun AppPalette.toColorScheme() =
    if (isDark) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary, secondary = primary, onSecondary = onPrimary,
            background = background, onBackground = textPrimary,
            surface = surface, onSurface = textPrimary,
            surfaceVariant = surfaceVariant, onSurfaceVariant = textMuted,
            surfaceContainerLowest = surface, surfaceContainerLow = background, surfaceContainer = surface,
            surfaceContainerHigh = surface, surfaceContainerHighest = surfaceVariant,
            outline = border, outlineVariant = border, error = danger,
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary, secondary = primary, onSecondary = onPrimary,
            background = background, onBackground = textPrimary,
            surface = surface, onSurface = textPrimary,
            surfaceVariant = surfaceVariant, onSurfaceVariant = textMuted,
            surfaceContainerLowest = surface, surfaceContainerLow = background, surfaceContainer = surface,
            surfaceContainerHigh = surface, surfaceContainerHighest = surfaceVariant,
            outline = border, outlineVariant = border, error = danger,
        )
    }

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun AppTheme(darkTheme: Boolean = ThemeController.isDark(), content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = palette.toColorScheme(), shapes = AppShapes, content = content)
    }
}

/** Scales the element to 0.96 while it is pressed, for tactile feedback on top of the ripple. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.scale(scale)
}

/** Clickable with ripple and the same press-scale feedback as the buttons. */
@Composable
fun Modifier.tactileClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource)
        .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
}
