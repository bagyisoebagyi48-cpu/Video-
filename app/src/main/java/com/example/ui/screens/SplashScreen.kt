package com.example.ui.screens

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlowingGlobeVisual
import com.example.ui.components.JimmyLogoHeader
import com.example.ui.components.WorldSkylineSilhouette
import com.example.ui.theme.JimmyGoldCrown
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyNavyDeep
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyOrangeDeep
import com.example.ui.theme.JimmyTextPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2100L)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_spinner")
    val spinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_degrees"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        JimmyNavyDarkest,
                        JimmyNavyDeep,
                        Color(0xFF0A163D),
                        JimmyNavyDarkest
                    )
                )
            )
            .testTag("splash_screen")
    ) {
        // Subtle starry cosmos background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stars = listOf(
                Offset(0.12f, 0.08f), Offset(0.84f, 0.11f), Offset(0.28f, 0.16f),
                Offset(0.68f, 0.19f), Offset(0.16f, 0.34f), Offset(0.88f, 0.31f),
                Offset(0.08f, 0.56f), Offset(0.92f, 0.52f), Offset(0.45f, 0.06f)
            )
            stars.forEachIndexed { index, pos ->
                drawCircle(
                    color = Color.White.copy(alpha = if (index % 2 == 0) 0.45f else 0.25f),
                    radius = if (index % 3 == 0) 3.2f else 2.0f,
                    center = Offset(size.width * pos.x, size.height * pos.y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Top: Jimmy Crowned Logo + Video Chat Bubble + "Meet The World" + "Random Video Chat"
            JimmyLogoHeader(
                showVideoBubbleIcon = true,
                subtitle = stringResource(R.string.tagline_meet_the_world),
                secondarySubtitle = stringResource(R.string.subtitle_random_video_chat)
            )

            // Center: Glowing Earth Globe + Glowing Orange Ring Loader
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                GlowingGlobeVisual(
                    globeSize = 290.dp,
                    showOrbitingCallers = false
                )

                // Overlay Loading Indicator matching the left panel of the mockup
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 80.dp)
                ) {
                    Canvas(modifier = Modifier.size(46.dp)) {
                        if (size.width <= 1f || size.height <= 1f) return@Canvas
                        rotate(degrees = spinDegrees) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        JimmyOrangeDeep,
                                        JimmyOrangeBright,
                                        JimmyGoldCrown
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 290f,
                                useCenter = false,
                                style = Stroke(width = 4.5.dp.toPx().coerceAtLeast(1f), cap = StrokeCap.Round)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.loading_status),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = JimmyTextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // Bottom World Landmarks Skyline Silhouette
        WorldSkylineSilhouette(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
