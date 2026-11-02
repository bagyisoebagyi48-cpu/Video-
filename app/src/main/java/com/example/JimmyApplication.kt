package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ui.auth.ensureFirebaseInitialized

class JimmyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            ensureFirebaseInitialized(this)
        }.onFailure { err ->
            Log.e(TAG, "Failed to initialize Firebase in JimmyApplication", err)
        }

        // Global crash guard for physical device stability:
        // Intercepts any uncaught exception, persists the trace, and re-launches MainActivity
        // into Diagnostic & Safe Mode instead of allowing the OS to crash and exit abruptly.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Global Uncaught Exception on thread '${thread.name}'", throwable)
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                val prefs = getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_LAST_CRASH, stackTrace).commit()

                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(EXTRA_CRASH_INFO, stackTrace)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to launch crash diagnostic screen", t)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        const val TAG = "JimmyApplication"
        const val PREFS_CRASH = "jimmy_crash_store"
        const val KEY_LAST_CRASH = "key_last_crash_trace"
        const val EXTRA_CRASH_INFO = "EXTRA_CRASH_INFO"
    }
}
