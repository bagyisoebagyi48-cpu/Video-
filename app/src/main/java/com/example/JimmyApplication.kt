package com.example

import android.app.Application
import android.util.Log

class JimmyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "JIMMY Application initialized cleanly without Firebase dependencies.")
    }

    companion object {
        const val TAG = "JimmyApplication"
    }
}
