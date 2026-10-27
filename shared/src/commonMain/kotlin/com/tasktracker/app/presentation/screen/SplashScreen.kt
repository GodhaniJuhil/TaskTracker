package com.tasktracker.app.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasktracker.app.presentation.theme.AppIcons

private val SplashTop = Color(0xFF0B1220)
private val SplashBottom = Color(0xFF1E293B)
private val SplashBlueLight = Color(0xFF5B8DEF)
private val SplashBlue = Color(0xFF2563EB)

/** Brand splash, always dark regardless of the in-app theme; the host decides when to dismiss it. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashTop, SplashBottom, SplashTop))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Soft ambient glow behind the logo.
                Canvas(Modifier.size(280.dp)) {
                    drawCircle(
                        Brush.radialGradient(listOf(SplashBlue.copy(alpha = 0.45f), Color.Transparent)),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = SplashBlue, spotColor = SplashBlue)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(SplashBlueLight, SplashBlue))),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AppIcons.Check, contentDescription = null, tint = SplashBlue, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Text(
                "Task Tracker",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Sync Tasks • Work Together",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SplashSpinner()
            Spacer(Modifier.height(20.dp))
            Text(
                "POWERED BY FIREBASE & KMP",
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SplashSpinner() {
    val rotation = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
    )
    Canvas(Modifier.size(24.dp).graphicsLayer { rotationZ = rotation.value }) {
        val stroke = 3.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(SplashBlueLight.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(SplashBlueLight, -90f, 90f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
