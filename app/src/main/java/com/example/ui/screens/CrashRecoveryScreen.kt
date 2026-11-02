package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.JimmyLogoHeader
import com.example.ui.theme.JimmyDangerRed
import com.example.ui.theme.JimmyGoldCrown
import com.example.ui.theme.JimmyNavyBorder
import com.example.ui.theme.JimmyNavyCard
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyNavyDeep
import com.example.ui.theme.JimmyNavySurface
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyOrangeDeep
import com.example.ui.theme.JimmyOrangeLight
import com.example.ui.theme.JimmyTextMuted
import com.example.ui.theme.JimmyTextPrimary
import com.example.ui.theme.JimmyTextSecondary

@Composable
fun JimmyCrashRecoveryScreen(
    crashDetails: String,
    onRestart: () -> Unit,
    onContinueGuest: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        JimmyNavyDarkest,
                        JimmyNavyDeep,
                        Color(0xFF1B0F2A),
                        JimmyNavyDarkest
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .testTag("crash_recovery_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JimmyLogoHeader(
                showVideoBubbleIcon = false,
                subtitle = "App Diagnostic & Safe Mode"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Warning Banner Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(JimmyNavySurface.copy(alpha = 0.95f))
                    .border(1.5.dp, JimmyDangerRed.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(JimmyDangerRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Diagnostic Alert",
                            tint = JimmyDangerRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Diagnostic Report",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = JimmyTextPrimary
                        )
                        Text(
                            text = "Crash was safely intercepted to protect your device.",
                            style = MaterialTheme.typography.labelMedium,
                            color = JimmyTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ဖုန်းတွင်း startup သို့မဟုတ် runtime အခြေအနေကြောင့် app ရပ်တန့်မှုကို ကာကွယ်ထားပါသည်။ အောက်ပါ error trace ကို copy ကူးယူနိုင်ပြီး app ကို ပြန်လည်စတင်နိုင်ပါသည်။",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JimmyTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Crash Trace Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(JimmyNavyCard)
                        .border(1.dp, JimmyNavyBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = crashDetails.ifBlank { "Unknown runtime exception intercepted." },
                        color = JimmyOrangeLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons inside Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("JIMMY Error Log", crashDetails)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Diagnostic trace copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = JimmyTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy Trace", color = JimmyTextPrimary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onRestart,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JimmyOrangeBright
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Restart App", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Safe Mode Action
            Button(
                onClick = onContinueGuest,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(JimmyOrangeLight, JimmyOrangeBright, JimmyOrangeDeep)
                        )
                    )
                    .testTag("continue_guest_mode_button")
            ) {
                Text(
                    text = "Continue in Safe Guest Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
