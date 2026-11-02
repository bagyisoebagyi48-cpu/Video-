package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.JimmyBlueAccent
import com.example.ui.theme.JimmyBlueLight
import com.example.ui.theme.JimmyCyanGlow
import com.example.ui.theme.JimmyGoldCrown
import com.example.ui.theme.JimmyNavyCard
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyNavyDeep
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyOrangeDeep
import com.example.ui.theme.JimmyOrangeLight
import com.example.ui.theme.JimmyTextMuted
import com.example.ui.theme.JimmyTextPrimary
import com.example.ui.theme.JimmyTextSecondary
import com.example.ui.theme.OutfitFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JimmyLogoHeader(
    showVideoBubbleIcon: Boolean,
    subtitle: String,
    secondarySubtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showVideoBubbleIcon) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Outer neon orange glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                JimmyOrangeBright.copy(alpha = 0.42f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = w * 0.56f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    // Speech bubble outline in glowing orange gradient
                    val bubblePath = Path().apply {
                        moveTo(w * 0.25f, h * 0.20f)
                        lineTo(w * 0.76f, h * 0.20f)
                        quadraticTo(w * 0.90f, h * 0.20f, w * 0.90f, h * 0.36f)
                        lineTo(w * 0.90f, h * 0.62f)
                        quadraticTo(w * 0.90f, h * 0.78f, w * 0.74f, h * 0.78f)
                        lineTo(w * 0.48f, h * 0.78f)
                        lineTo(w * 0.30f, h * 0.92f)
                        lineTo(w * 0.34f, h * 0.78f)
                        lineTo(w * 0.25f, h * 0.78f)
                        quadraticTo(w * 0.10f, h * 0.78f, w * 0.10f, h * 0.62f)
                        lineTo(w * 0.10f, h * 0.36f)
                        quadraticTo(w * 0.10f, h * 0.20f, w * 0.25f, h * 0.20f)
                        close()
                    }
                    drawPath(
                        path = bubblePath,
                        brush = Brush.linearGradient(
                            colors = listOf(JimmyGoldCrown, JimmyOrangeBright, JimmyOrangeDeep)
                        ),
                        style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Jimmy Video Chat Logo",
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-2).dp)
                )
            }
        }

        // Crown + "Jimmy" text
        Box(contentAlignment = Alignment.TopStart) {
            // Golden crown perched over the 'J'
            Canvas(
                modifier = Modifier
                    .size(width = 36.dp, height = 22.dp)
                    .offset(x = 4.dp, y = (-12).dp)
            ) {
                val cw = size.width
                val ch = size.height
                val crown = Path().apply {
                    moveTo(cw * 0.12f, ch * 0.88f)
                    lineTo(cw * 0.04f, ch * 0.25f)
                    lineTo(cw * 0.32f, ch * 0.56f)
                    lineTo(cw * 0.50f, ch * 0.12f)
                    lineTo(cw * 0.68f, ch * 0.56f)
                    lineTo(cw * 0.96f, ch * 0.25f)
                    lineTo(cw * 0.88f, ch * 0.88f)
                    close()
                }
                drawPath(
                    path = crown,
                    brush = Brush.verticalGradient(
                        colors = listOf(JimmyGoldCrown, JimmyOrangeBright)
                    )
                )
                drawCircle(color = JimmyGoldCrown, radius = 2.6f, center = Offset(cw * 0.04f, ch * 0.20f))
                drawCircle(color = JimmyGoldCrown, radius = 3.0f, center = Offset(cw * 0.50f, ch * 0.08f))
                drawCircle(color = JimmyGoldCrown, radius = 2.6f, center = Offset(cw * 0.96f, ch * 0.20f))
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            brush = Brush.verticalGradient(
                                colors = listOf(JimmyGoldCrown, JimmyOrangeBright, JimmyOrangeDeep)
                            ),
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontSize = if (showVideoBubbleIcon) 54.sp else 46.sp
                        )
                    ) {
                        append("J")
                    }
                    withStyle(
                        SpanStyle(
                            color = JimmyTextPrimary,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontSize = if (showVideoBubbleIcon) 52.sp else 44.sp
                        )
                    ) {
                        append("immy")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = if (showVideoBubbleIcon) {
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = JimmyTextPrimary
                )
            } else {
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = JimmyTextSecondary
                )
            }
        )

        if (secondarySubtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(1.dp)
                        .background(JimmyTextMuted.copy(alpha = 0.5f))
                )
                Text(
                    text = secondarySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JimmyTextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(1.dp)
                        .background(JimmyTextMuted.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun GlowingGlobeVisual(
    globeSize: Dp,
    showOrbitingCallers: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "globe_anim")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = modifier.size(globeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * (if (showOrbitingCallers) 0.34f else 0.44f)

            // Outer atmospheric blue/cyan halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JimmyCyanGlow.copy(alpha = 0.45f * glowPulse),
                        JimmyBlueAccent.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 1.55f
                ),
                radius = r * 1.55f,
                center = Offset(cx, cy)
            )

            // Core Earth sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A4B9C),
                        Color(0xFF0A2158),
                        Color(0xFF050D2A)
                    ),
                    center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                    radius = r * 1.2f
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            // Latitude & longitude grid arcs
            for (factor in listOf(-0.55f, -0.2f, 0.2f, 0.55f)) {
                drawOval(
                    color = JimmyCyanGlow.copy(alpha = 0.16f),
                    topLeft = Offset(cx - r * 0.92f, cy + r * factor - r * 0.15f),
                    size = Size(r * 1.84f, r * 0.30f),
                    style = Stroke(width = 1.2f)
                )
            }

            // Stylized illuminated continents & city light clusters
            val cityPoints = listOf(
                Offset(-0.35f, -0.25f), Offset(-0.28f, -0.18f), Offset(-0.42f, -0.10f),
                Offset(-0.18f, 0.22f), Offset(-0.12f, 0.38f),
                Offset(0.08f, -0.32f), Offset(0.18f, -0.24f), Offset(0.25f, -0.15f),
                Offset(0.38f, -0.05f), Offset(0.45f, 0.08f), Offset(0.52f, -0.18f),
                Offset(0.30f, 0.18f), Offset(0.12f, 0.05f), Offset(-0.05f, -0.08f)
            )
            cityPoints.forEachIndexed { idx, pt ->
                val rad = (orbitAngle * 0.4f + idx * 14f) * (PI.toFloat() / 180f)
                val shiftX = (pt.x + 0.08f * sin(rad)).coerceIn(-0.78f, 0.78f)
                val px = cx + shiftX * r
                val py = cy + pt.y * r
                drawCircle(
                    color = Color(0xFF1F6E58).copy(alpha = 0.45f),
                    radius = r * 0.16f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = JimmyGoldCrown.copy(alpha = 0.85f),
                    radius = r * 0.032f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = JimmyOrangeBright.copy(alpha = 0.40f),
                    radius = r * 0.075f,
                    center = Offset(px, py)
                )
            }

            // Rim light on top-left of Earth
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        JimmyCyanGlow.copy(alpha = 0.75f),
                         JimmyBlueLight.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    start = Offset(cx - r, cy - r),
                    end = Offset(cx + r, cy + r)
                ),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 3.5f)
            )

            // Glowing orange orbital rings matching the reference mockup
            if (showOrbitingCallers) {
                rotate(degrees = -18f, pivot = Offset(cx, cy)) {
                    drawOval(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                JimmyOrangeBright.copy(alpha = 0.15f),
                                JimmyOrangeLight.copy(alpha = 0.90f),
                                JimmyGoldCrown,
                                JimmyOrangeBright.copy(alpha = 0.20f)
                            )
                        ),
                        topLeft = Offset(cx - r * 1.38f, cy - r * 0.42f),
                        size = Size(r * 2.76f, r * 0.84f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                rotate(degrees = 22f, pivot = Offset(cx, cy)) {
                    drawOval(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                JimmyOrangeBright.copy(alpha = 0.80f),
                                JimmyCyanGlow.copy(alpha = 0.45f),
                                JimmyOrangeLight.copy(alpha = 0.85f)
                            )
                        ),
                        topLeft = Offset(cx - r * 1.32f, cy - r * 0.36f),
                        size = Size(r * 2.64f, r * 0.72f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        if (showOrbitingCallers) {
            // 5 Global Caller Badges positioned around the globe orbit
            val badges = listOf(
                Triple("🇹🇭", "Nicha", Offset(-82f, -44f)),
                Triple("🇺🇸", "Liam", Offset(78f, -56f)),
                Triple("🇲🇲", "Aung", Offset(-86f, 38f)),
                Triple("🇯🇵", "Yuki", Offset(86f, 24f)),
                Triple("🇰🇷", "Min", Offset(18f, 76f))
            )
            badges.forEach { (flag, name, offsetDp) ->
                Column(
                    modifier = Modifier.offset(x = offsetDp.x.dp, y = offsetDp.y.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(JimmyNavyCard, JimmyNavyDeep)
                                )
                            )
                            .border(2.dp, JimmyOrangeLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = flag, fontSize = 18.sp)
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = JimmyTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WorldSkylineSilhouette(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        val w = size.width
        val h = size.height
        val skylineColor = Color(0xFF162458).copy(alpha = 0.72f)

        // Base ground bar
        drawRect(
            color = skylineColor,
            topLeft = Offset(0f, h * 0.82f),
            size = Size(w, h * 0.18f)
        )

        // Eiffel Tower silhouette (left)
        val eiffel = Path().apply {
            moveTo(w * 0.08f, h * 0.82f)
            lineTo(w * 0.12f, h * 0.10f)
            lineTo(w * 0.13f, h * 0.10f)
            lineTo(w * 0.17f, h * 0.82f)
            close()
        }
        drawPath(eiffel, skylineColor)

        // Big Ben / Clock Tower
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.25f, h * 0.28f),
            size = Size(w * 0.05f, h * 0.54f)
        )
        val spire = Path().apply {
            moveTo(w * 0.25f, h * 0.28f)
            lineTo(w * 0.275f, h * 0.12f)
            lineTo(w * 0.30f, h * 0.28f)
            close()
        }
        drawPath(spire, skylineColor)

        // Myanmar Pagoda stupa silhouette (center-left)
        val pagoda = Path().apply {
            moveTo(w * 0.38f, h * 0.82f)
            lineTo(w * 0.41f, h * 0.45f)
            lineTo(w * 0.435f, h * 0.18f)
            lineTo(w * 0.46f, h * 0.45f)
            lineTo(w * 0.49f, h * 0.82f)
            close()
        }
        drawPath(pagoda, skylineColor)

        // Japanese Torii Gate silhouette (center-right)
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.56f, h * 0.42f),
            size = Size(w * 0.14f, h * 0.07f)
        )
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.58f, h * 0.49f),
            size = Size(w * 0.025f, h * 0.33f)
        )
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.655f, h * 0.49f),
            size = Size(w * 0.025f, h * 0.33f)
        )

        // Modern skyscrapers (right)
        drawRoundRect(
            color = skylineColor,
            topLeft = Offset(w * 0.76f, h * 0.32f),
            size = Size(w * 0.06f, h * 0.50f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = skylineColor,
            topLeft = Offset(w * 0.85f, h * 0.22f),
            size = Size(w * 0.07f, h * 0.60f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

@Composable
fun AdMobBannerSlot(
    modifier: Modifier = Modifier
) {
    val bannerUnitId = "ca-app-pub-3940256099942544/6300978111"
    val isTestAd = bannerUnitId.startsWith("ca-app-pub-3940256099942544")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JimmyNavyDeep.copy(alpha = 0.88f))
            .border(1.dp, JimmyNavyCard, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(JimmyOrangeBright.copy(alpha = 0.22f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isTestAd) "Ad • Test Mode" else "Sponsored",
                    style = MaterialTheme.typography.labelMedium,
                    color = JimmyOrangeLight,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "JIMMY Global Boost • Connect Faster",
                    style = MaterialTheme.typography.labelMedium,
                    color = JimmyTextPrimary
                )
                Text(
                    text = "AdMob Unit: ${bannerUnitId.takeLast(14)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = JimmyTextMuted,
                    fontSize = 10.sp
                )
            }
        }
        Text(
            text = "Non-Call Screen",
            style = MaterialTheme.typography.labelMedium,
            color = JimmyTextSecondary,
            fontSize = 10.sp
        )
    }
}
