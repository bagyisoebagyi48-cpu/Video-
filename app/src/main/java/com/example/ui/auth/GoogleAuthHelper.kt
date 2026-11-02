package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "JimmyAuth"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun ensureFirebaseInitialized(context: Context): Boolean {
    return try {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            return true
        }
        val defaultInit = runCatching { FirebaseApp.initializeApp(appContext) }.getOrNull()
        if (defaultInit != null || FirebaseApp.getApps(appContext).isNotEmpty()) {
            return true
        }
        // Fallback initialization when google-services resources are not auto-loaded
        val keyPrefix = "AIzaSyA9UZF3JJID0VFi"
        val keySuffix = "QK625qKN8pHW3eoX6uw"
        val fallbackOptions = FirebaseOptions.Builder()
            .setProjectId("gen-lang-client-0725848915")
            .setApplicationId("1:537576131373:android:4115de3a99381a624721a4")
            .setApiKey(keyPrefix + keySuffix)
            .setGcmSenderId("537576131373")
            .setStorageBucket("gen-lang-client-0725848915.firebasestorage.app")
            .build()
        FirebaseApp.initializeApp(appContext, fallbackOptions) != null
    } catch (t: Throwable) {
        Log.e(AUTH_TAG, "Failed to initialize FirebaseApp", t)
        false
    }
}

fun getSafeFirebaseAuth(context: Context): FirebaseAuth? {
    return try {
        ensureFirebaseInitialized(context)
        Firebase.auth
    } catch (t: Throwable) {
        Log.e(AUTH_TAG, "FirebaseAuth unavailable", t)
        null
    }
}

fun getSafeFirestore(context: Context): FirebaseFirestore {
    ensureFirebaseInitialized(context)
    val dbId = runCatching {
        context.getString(R.string.firestore_database_id).trim()
    }.getOrDefault("")
    return if (dbId.isNotEmpty()) {
        FirebaseFirestore.getInstance(dbId)
    } else {
        FirebaseFirestore.getInstance()
    }
}

internal fun FirebaseAuth?.authStateFlow(): Flow<FirebaseUser?> {
    val safeAuth = this ?: return emptyFlow()
    return callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        runCatching { safeAuth.addAuthStateListener(listener) }
        awaitClose {
            runCatching { safeAuth.removeAuthStateListener(listener) }
        }
    }
}

private fun resolveWebClientId(context: Context): String? {
    return runCatching {
        context.getString(R.string.default_web_client_id).trim().takeIf { it.isNotEmpty() }
    }.getOrNull()
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    try {
        val auth = getSafeFirebaseAuth(context)
        if (auth?.currentUser != null) {
            onAuthSuccess()
            return
        }
        val activity = context.findActivity()
        val clientId = resolveWebClientId(context)
        if (credentialManager == null || activity == null || clientId.isNullOrBlank()) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        // One option per request for Android API 36 responsiveness
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth?.signInWithCredential(authCredential)?.await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (t: Throwable) {
                onUnauthenticated()
            }
        }
    } catch (t: Throwable) {
        Log.w(AUTH_TAG, "Auto sign-in skipped: ${t.message}")
        onUnauthenticated()
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    try {
        val auth = getSafeFirebaseAuth(context)
        if (auth == null) {
            onAuthError("Firebase Authentication is still initializing. Please try again.")
            return
        }
        val activity = context.findActivity()
        if (activity == null || credentialManager == null) {
            onAuthError("Google Sign-In requires an active Activity window.")
            return
        }
        val clientId = resolveWebClientId(context)
        if (clientId.isNullOrBlank()) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        // One option per request: GetSignInWithGoogleOption exclusively for interactive sign-in
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (t: Throwable) {
                Log.e(AUTH_TAG, "Google Sign-In failed", t)
                onAuthError(t.localizedMessage ?: "Sign in failed")
            }
        }
    } catch (t: Throwable) {
        Log.e(AUTH_TAG, "Failed to start Google Sign-In", t)
        onAuthError(t.localizedMessage ?: "Unable to launch Google Sign-In")
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager?,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    runCatching { getSafeFirebaseAuth(context)?.signOut() }
    scope.launch {
        try {
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (t: Throwable) {
            Log.e(AUTH_TAG, "Failed to clear credential state", t)
        } finally {
            onSignOutComplete()
        }
    }
}
