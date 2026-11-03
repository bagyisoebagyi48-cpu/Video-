package com.example.ui.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReport(
    val timestampMs: Long,
    val formattedTime: String,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
    val androidVersion: String,
    val deviceModel: String,
    val appVersion: String = "1.0.0"
)

object CrashDiagnostics {
    private const val PREFS_NAME = "jimmy_crash_diagnostics"
    private const val KEY_TIMESTAMP = "last_crash_timestamp"
    private const val KEY_THREAD = "last_crash_thread"
    private const val KEY_EXCEPTION_CLASS = "last_crash_class"
    private const val KEY_MESSAGE = "last_crash_message"
    private const val KEY_STACKTRACE = "last_crash_stacktrace"
    private const val TAG = "CrashDiagnostics"

    @Volatile
    private var inMemoryLastCrash: CrashReport? = null

    fun recordCrash(context: Context?, thread: Thread?, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceStr = sw.toString()
        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now))

        val report = CrashReport(
            timestampMs = now,
            formattedTime = timeFormat,
            threadName = thread?.name ?: "main",
            exceptionClass = throwable.javaClass.name,
            message = throwable.message ?: "No error message provided",
            stackTrace = stackTraceStr,
            androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
        inMemoryLastCrash = report
        Log.e(TAG, "Recorded crash: ${report.exceptionClass}: ${report.message}\n$stackTraceStr")

        if (context != null) {
            runCatching {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong(KEY_TIMESTAMP, report.timestampMs)
                    .putString(KEY_THREAD, report.threadName)
                    .putString(KEY_EXCEPTION_CLASS, report.exceptionClass)
                    .putString(KEY_MESSAGE, report.message)
                    .putString(KEY_STACKTRACE, report.stackTrace)
                    .commit()
            }.onFailure { Log.e(TAG, "Failed to persist crash report", it) }
        }
    }

    fun readLastCrash(context: Context): CrashReport? {
        inMemoryLastCrash?.let { return it }
        return runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
            if (timestamp == 0L) return null
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
            CrashReport(
                timestampMs = timestamp,
                formattedTime = timeFormat,
                threadName = prefs.getString(KEY_THREAD, "unknown") ?: "unknown",
                exceptionClass = prefs.getString(KEY_EXCEPTION_CLASS, "Exception") ?: "Exception",
                message = prefs.getString(KEY_MESSAGE, "Unknown error") ?: "Unknown error",
                stackTrace = prefs.getString(KEY_STACKTRACE, "") ?: "",
                androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            )
        }.getOrNull()
    }

    fun clearLastCrash(context: Context) {
        inMemoryLastCrash = null
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    fun clearAllData(context: Context) {
        clearLastCrash(context)
        runCatching {
            context.getSharedPreferences("jimmy_guest_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("jimmy_session_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            true
        }.getOrDefault(false)
    }

    fun getDiagnosticSummary(report: CrashReport): String {
        return buildString {
            appendLine("=== JIMMY Crash Diagnostic Report ===")
            appendLine("Time: ${report.formattedTime}")
            appendLine("Thread: ${report.threadName}")
            appendLine("Device: ${report.deviceModel}")
            appendLine("OS: ${report.androidVersion}")
            appendLine("App Version: ${report.appVersion}")
            appendLine("Exception: ${report.exceptionClass}")
            appendLine("Message: ${report.message}")
            appendLine("=== Stack Trace ===")
            appendLine(report.stackTrace)
        }
    }
}
