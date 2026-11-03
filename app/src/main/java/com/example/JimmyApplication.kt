package com.example

import android.app.Application
import android.util.Log
import com.example.ui.diagnostics.CrashDiagnostics

class JimmyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e(TAG, "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
                CrashDiagnostics.recordCrash(this, thread, throwable)
                defaultHandler?.uncaughtException(thread, throwable)
            }
            Log.i(TAG, "JIMMY Application initialized cleanly without Firebase dependencies.")
        } catch (t: Throwable) {
            Log.e(TAG, "Critical failure in Application.onCreate", t)
            CrashDiagnostics.recordCrash(this, Thread.currentThread(), t)
        }
    }

    companion object {
        const val TAG = "JimmyApplication"
    }
}
