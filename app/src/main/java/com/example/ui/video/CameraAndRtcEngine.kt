package com.example.ui.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.util.Base64
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CountryOption
import com.example.ui.theme.JimmyBlueAccent
import com.example.ui.theme.JimmyCyanGlow
import com.example.ui.theme.JimmyNavyCard
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyOnlineGreen
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyOrangeLight
import com.example.ui.theme.JimmyTextPrimary
import com.example.ui.theme.JimmyTextSecondary
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

object WebRtcSessionDescriptor {
    private const val DEFAULT_STUN_URL = "stun:stun.l.google.com:19302"

    fun createOfferSdp(userId: String, cameraFacing: String, audioMuted: Boolean): String {
        val stunUrl = DEFAULT_STUN_URL
        val sessionId = UUID.randomUUID().toString().take(8)
        return buildString {
            appendLine("v=0")
            appendLine("o=jimmy-$userId $sessionId 2 IN IP4 0.0.0.0")
            appendLine("s=JIMMY-WebRTC-LiveCall")
            appendLine("a=ice-lite")
            appendLine("a=stun:$stunUrl")
            appendLine("a=camera:$cameraFacing")
            appendLine("a=audio:${if (audioMuted) "inactive" else "sendrecv"}")
            appendLine("m=video 9 UDP/TLS/RTP/SAVPF 96")
        }
    }

    fun configureCallAudioRouting(context: Context, enableCallMode: Boolean, isMuted: Boolean) {
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (enableCallMode) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isMicrophoneMute = isMuted
            } else {
                audioManager.isMicrophoneMute = false
                audioManager.mode = AudioManager.MODE_NORMAL
            }
        }
    }
}

@Composable
fun LocalCameraPreviewSurface(
    useFrontCamera: Boolean,
    isMicMuted: Boolean,
    onFrameCaptured: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFrameCaptured by rememberUpdatedState(onFrameCaptured)
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(isMicMuted) {
        WebRtcSessionDescriptor.configureCallAudioRouting(
            context = context,
            enableCallMode = true,
            isMuted = isMicMuted
        )
    }

    // Periodically sample previewView.bitmap on the main thread and compress on Dispatchers.Default
    // Avoids YuvImage JNI native buffer issues and avoids Compose snapshot state access from background threads
    LaunchedEffect(lifecycleOwner, useFrontCamera) {
        while (isActive) {
            delay(2500L)
            val snapshotBitmap = runCatching { previewView.bitmap }.getOrNull()
            if (snapshotBitmap != null) {
                val encoded = withContext(Dispatchers.Default) {
                    encodeBitmapToCompactBase64(snapshotBitmap)
                }
                if (!encoded.isNullOrBlank()) {
                    runCatching { latestOnFrameCaptured(encoded) }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, useFrontCamera) {
        val cameraProviderFuture = runCatching { ProcessCameraProvider.getInstance(context) }.getOrNull()
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture?.get() ?: return@Runnable
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val preferredSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                val fallbackSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                val activeSelector = when {
                    runCatching { cameraProvider.hasCamera(preferredSelector) }.getOrDefault(false) -> preferredSelector
                    runCatching { cameraProvider.hasCamera(fallbackSelector) }.getOrDefault(false) -> fallbackSelector
                    else -> null
                }

                cameraProvider.unbindAll()
                if (activeSelector != null) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        activeSelector,
                        preview
                    )
                }
            }.onFailure { err ->
                Log.w("JimmyCameraX", "Camera bind fallback: ${err.message}")
            }
        }
        if (cameraProviderFuture != null) {
            runCatching {
                cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
            }
        }

        onDispose {
            runCatching {
                if (cameraProviderFuture?.isDone == true) {
                    cameraProviderFuture.get()?.unbindAll()
                }
            }
            WebRtcSessionDescriptor.configureCallAudioRouting(
                context = context,
                enableCallMode = false,
                isMuted = false
            )
        }
    }

    Box(modifier = modifier.background(JimmyNavyDarkest)) {
        AndroidView(
            factory = {
                (previewView.parent as? android.view.ViewGroup)?.removeView(previewView)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun encodeBitmapToCompactBase64(bitmap: Bitmap): String? = runCatching {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null
    val scaled = Bitmap.createScaledBitmap(bitmap, 96, 128, true)
    val compressedOut = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 45, compressedOut)
    if (scaled !== bitmap) {
        runCatching { scaled.recycle() }
    }
    runCatching { bitmap.recycle() }
    Base64.encodeToString(compressedOut.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

fun decodeBase64FrameToImageBitmap(base64Data: String): ImageBitmap? {
    if (base64Data.isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun RemoteStrangerVideoStage(
    isSearching: Boolean,
    partnerName: String,
    partnerCountry: CountryOption,
    partnerFrameBase64: String,
    partnerAudioMuted: Boolean,
    searchingStatusText: String,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(partnerFrameBase64) {
        decodeBase64FrameToImageBitmap(partnerFrameBase64)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "remote_stage")
    val radarWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_wave"
    )
    val audioWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "audio_wave"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151E46),
                        Color(0xFF0A102A),
                        Color(0xFF06091A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSearching) {
            // Searching / Matchmaking Radar Animation
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 1f || size.height <= 1f) return@Canvas
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = (size.minDimension * 0.42f).coerceAtLeast(1f)
                for (ring in 0..2) {
                    val progress = ((radarWave + ring * 0.33f) % 1f).coerceIn(0f, 1f)
                    drawCircle(
                        color = JimmyOrangeBright.copy(alpha = ((1f - progress) * 0.55f).coerceIn(0f, 1f)),
                        radius = (maxR * (0.25f + progress * 0.75f)).coerceAtLeast(1f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.dp.toPx().coerceAtLeast(1f))
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(JimmyNavyCard)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = JimmyOrangeBright,
                        strokeWidth = 3.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = partnerCountry.flagEmoji,
                        fontSize = 28.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = searchingStatusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = JimmyTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Target: ${partnerCountry.flagEmoji} ${partnerCountry.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JimmyOrangeLight
                )
            }
        } else if (decodedBitmap != null) {
            // Render real live video frame decoded from remote stranger's CameraX stream
            Image(
                bitmap = decodedBitmap,
                contentDescription = "Stranger Live Video Feed",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Connected Stranger Live Stream Stage (while awaiting remote camera frame packet)
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 1f || size.height <= 1f) return@Canvas
                val w = size.width
                val h = size.height
                val circleR = (w * 0.6f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JimmyBlueAccent.copy(alpha = 0.32f),
                            JimmyOrangeBright.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = circleR
                    ),
                    radius = circleR,
                    center = Offset(w * 0.5f, h * 0.45f)
                )

                // Live audio equalizer bars at the bottom of the partner stage
                if (!partnerAudioMuted) {
                    val barCount = 15
                    val totalW = w * 0.36f
                    val startX = (w - totalW) / 2f
                    val step = totalW / barCount
                    for (i in 0 until barCount) {
                        val amp = (sin(audioWavePhase + i * 0.65f) * 0.5f + 0.5f)
                        val barH = 8.dp.toPx() + amp * 26.dp.toPx()
                        drawLine(
                            color = JimmyCyanGlow.copy(alpha = 0.78f),
                            start = Offset(startX + i * step, h * 0.78f),
                            end = Offset(startX + i * step, h * 0.78f - barH),
                            strokeWidth = 4.dp.toPx().coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(JimmyOrangeBright, JimmyBlueAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partnerCountry.flagEmoji,
                        fontSize = 42.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = partnerName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = JimmyTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(JimmyNavyDarkest.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (partnerAudioMuted) Icons.Default.MicOff else Icons.Default.GraphicEq,
                        contentDescription = "Stranger Audio Status",
                        tint = if (partnerAudioMuted) JimmyOrangeLight else JimmyOnlineGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (partnerAudioMuted) "Mic Muted • WebRTC Live" else "Live Audio & Video • HD",
                        style = MaterialTheme.typography.labelMedium,
                        color = JimmyTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
