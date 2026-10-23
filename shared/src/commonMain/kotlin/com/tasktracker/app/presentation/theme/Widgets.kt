package com.tasktracker.app.presentation.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasktracker.app.domain.model.Role

/** Small tinted pill, e.g. a priority or status. */
@Composable
fun AppTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun RoleBadge(role: Role, modifier: Modifier = Modifier) {
    val (container, content) = when (role) {
        Role.OWNER -> AppColors.Primary to AppColors.OnPrimary
        Role.ADMIN -> AppColors.Primary.copy(alpha = 0.14f) to AppColors.Primary
        Role.MEMBER -> AppColors.SurfaceVariant to AppColors.TextPrimary
        Role.GUEST -> Color.Transparent to AppColors.TextMuted
    }
    Text(
        role.name,
        color = content,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        modifier = modifier
            .background(container, CircleShape)
            .then(if (role == Role.GUEST) Modifier.border(1.dp, AppColors.TextMuted, CircleShape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun hueOf(text: String): Float = text.fold(7) { acc, c -> (acc * 31 + c.code) % 360 }.toFloat()

fun initialsOf(text: String): String =
    text.split(' ', '@', '.', '_', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Circle with the initials of [name] on a colour derived from it, until a photo exists. */
@Composable
fun Avatar(name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).background(Color.hsv(hueOf(name), 0.55f, 0.58f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initialsOf(name), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.38f).sp)
    }
}

/** Rounded gradient square with the workspace initials. */
@Composable
fun WorkspaceMark(name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val hue = hueOf(name)
    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(Color.hsv(hue, 0.62f, 0.85f), Color.hsv((hue + 28f) % 360f, 0.7f, 0.6f))),
                RoundedCornerShape(size / 3),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(initialsOf(name), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.34f).sp)
    }
}

/** Filter / choice chip with a 44dp touch target. */
@Composable
fun AppChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, count: Int? = null) {
    val container = if (selected) AppColors.Primary else AppColors.Surface
    val content = if (selected) AppColors.OnPrimary else AppColors.TextPrimary
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(container)
            .border(1.5.dp, if (selected) AppColors.Primary else AppColors.Border, CircleShape)
            .semantics { this.role = SemanticsRole.Tab; this.selected = selected }
            .tactileClickable(onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (count != null) {
            Text(
                count.toString(),
                color = content,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(content.copy(alpha = 0.18f), CircleShape).padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.TextMuted,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(64.dp).background(AppColors.Primary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted, textAlign = TextAlign.Center)
    }
}

/** Pulsing placeholder card shown while a list loads. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface, MaterialTheme.shapes.medium)
            .border(1.dp, AppColors.Border, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(24.dp).alpha(alpha).background(AppColors.SurfaceVariant, RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).alpha(alpha).background(AppColors.SurfaceVariant, CircleShape))
            Box(Modifier.fillMaxWidth(0.45f).height(10.dp).alpha(alpha).background(AppColors.SurfaceVariant, CircleShape))
        }
    }
}
