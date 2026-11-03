package com.example

import android.app.Application
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
    }

    companion object {
        const val TAG = "JimmyApplication"
    }
}
