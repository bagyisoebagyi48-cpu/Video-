package com.example

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ui.auth.ensureFirebaseInitialized
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class JimmyApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (base != null) {
            runCatching { ensureFirebaseInitialized(base) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        installDefensiveExceptionHandler()
        runCatching {
            ensureFirebaseInitialized(this)
        }.onFailure { err ->
            Log.e(TAG, "Failed to initialize Firebase in JimmyApplication", err)
        }
    }

    private fun installDefensiveExceptionHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val lastCrashTimeMs = AtomicLong(0L)
        val rapidCrashCount = AtomicInteger(0)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            val mainLooper = runCatching { Looper.getMainLooper() }.getOrNull()
            val isMainThread = mainLooper?.thread == thread

            // Suppress background worker thread crashes (e.g. CameraX, OkHttp/gRPC, Play Services binder)
            if (!isMainThread) {
                Log.w(TAG, "Suppressed background thread exception on ${thread.name}")
                return@setDefaultUncaughtExceptionHandler
            }

            // On the main thread, recover the main Looper unless we hit a tight unrecoverable crash loop
            val now = System.currentTimeMillis()
            val elapsed = now - lastCrashTimeMs.getAndSet(now)
            val count = if (elapsed < 1200L) {
                rapidCrashCount.incrementAndGet()
            } else {
                rapidCrashCount.set(1)
                1
            }

            if (count <= 4) {
                while (true) {
                    try {
                        Looper.loop()
                    } catch (loopErr: Throwable) {
                        Log.e(TAG, "Main looper recovered from exception: ${loopErr.message}", loopErr)
                        val loopNow = System.currentTimeMillis()
                        if (loopNow - lastCrashTimeMs.getAndSet(loopNow) < 500L &&
                            rapidCrashCount.incrementAndGet() > 4
                        ) {
                            previousHandler?.uncaughtException(thread, loopErr)
                            break
                        }
                    }
                }
            } else {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private companion object {
        const val TAG = "JimmyApplication"
    }
}
