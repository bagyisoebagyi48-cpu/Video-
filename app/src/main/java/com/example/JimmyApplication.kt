package com.example

import android.app.Application
import android.util.Log
import com.example.ui.auth.ensureFirebaseInitialized

class JimmyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            runCatching {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(throwable)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        runCatching {
            ensureFirebaseInitialized(this)
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("JimmyApplication initialized successfully")
        }.onFailure { err ->
            Log.e(TAG, "Failed to initialize Firebase in JimmyApplication", err)
            runCatching {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(err)
            }
        }
    }

    companion object {
        const val TAG = "JimmyApplication"
    }
}
