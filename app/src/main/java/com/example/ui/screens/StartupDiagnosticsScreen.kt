package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.diagnostics.CrashDiagnostics
import com.example.ui.diagnostics.CrashReport
import com.example.ui.theme.JimmyNavyBorder
import com.example.ui.theme.JimmyNavyCard
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyNavyDeep
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyOrangeDeep
import com.example.ui.theme.JimmyTextMuted
import com.example.ui.theme.JimmyTextPrimary
import com.example.ui.theme.JimmyTextSecondary

@Composable
fun StartupDiagnosticsScreen(
    report: CrashReport,
    onRetry: () -> Unit,
    onResetData: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("startup_diagnostics_screen"),
        color = JimmyNavyDarkest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Warning Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B151E))
                    .border(2.dp, Color(0xFFEF5350), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Startup Error",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Startup Diagnostic Report",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = JimmyTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "An exception occurred during app launch. Review details below or restart cleanly.",
                style = MaterialTheme.typography.bodyMedium,
                color = JimmyTextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Diagnostic Info Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JimmyNavyCard)
                    .border(1.dp, JimmyNavyBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "EXCEPTION SUMMARY",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = JimmyOrangeBright
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = report.exceptionClass,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = report.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFCDD2)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Thread: ${report.threadName}", style = MaterialTheme.typography.labelSmall, color = JimmyTextMuted)
                        Text(text = "Time: ${report.formattedTime}", style = MaterialTheme.typography.labelSmall, color = JimmyTextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = report.deviceModel, style = MaterialTheme.typography.labelSmall, color = JimmyTextMuted)
                        Text(text = report.androidVersion, style = MaterialTheme.typography.labelSmall, color = JimmyTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stack Trace Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JimmyNavyDeep)
                    .border(1.dp, JimmyNavyBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "STACK TRACE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = JimmyTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B1A))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = report.stackTrace.ifBlank { "No stack trace available." },
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = JimmyOrangeBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("diagnostic_retry_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Restart App", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val summary = CrashDiagnostics.getDiagnosticSummary(report)
                    val copied = CrashDiagnostics.copyToClipboard(context, "JIMMY Diagnostic Log", summary)
                    if (copied) {
                        Toast.makeText(context, "Diagnostic log copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("diagnostic_copy_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = JimmyOrangeBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Copy Diagnostic Log", fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onResetData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("diagnostic_reset_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFEF5350))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Reset Local Storage & Cache", fontWeight = FontWeight.SemiBold, color = Color(0xFFEF5350))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
