package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.util.UUID

/**
 * Clean session helper with zero Firebase dependencies.
 * Provides instant direct guest session access without authentication walls.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object GuestSessionManager {
    data class GuestSession(val userId: String, val displayName: String)

    private var fallbackUid: String = "jimmy_guest_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    private var fallbackDisplayName: String = "Guest #${(1000..9999).random()}"

    fun getOrCreateUserId(context: Context): String {
        return runCatching {
            val prefs = context.getSharedPreferences("jimmy_guest_prefs", Context.MODE_PRIVATE)
            var uid = prefs.getString("guest_uid", null)
            if (uid.isNullOrBlank()) {
                uid = "jimmy_guest_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                prefs.edit().putString("guest_uid", uid).apply()
            }
            fallbackUid = uid
            uid
        }.getOrDefault(fallbackUid)
    }

    fun getDisplayName(context: Context): String {
        return runCatching {
            val prefs = context.getSharedPreferences("jimmy_guest_prefs", Context.MODE_PRIVATE)
            var name = prefs.getString("guest_name", null)
            if (name.isNullOrBlank()) {
                val randomNum = (1000..9999).random()
                name = "Guest #$randomNum"
                prefs.edit().putString("guest_name", name).apply()
            }
            fallbackDisplayName = name
            name
        }.getOrDefault(fallbackDisplayName)
    }

    fun setDisplayName(context: Context, name: String) {
        val safeName = name.trim().ifBlank { "Guest #${(1000..9999).random()}" }
        fallbackDisplayName = safeName
        runCatching {
            val prefs = context.getSharedPreferences("jimmy_guest_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("guest_name", safeName).apply()
        }
    }

    fun getOrCreateSession(context: Context): GuestSession {
        return GuestSession(
            userId = getOrCreateUserId(context),
            displayName = getDisplayName(context)
        )
    }

    fun resetSession(context: Context): GuestSession {
        val newUid = "jimmy_guest_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val randomNum = (1000..9999).random()
        val defaultName = "Guest #$randomNum"
        fallbackUid = newUid
        fallbackDisplayName = defaultName
        runCatching {
            val prefs = context.getSharedPreferences("jimmy_guest_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("guest_uid", newUid).putString("guest_name", defaultName).apply()
        }
        return GuestSession(newUid, defaultName)
    }
}
