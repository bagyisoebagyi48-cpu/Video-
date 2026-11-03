package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.CountryCatalog
import com.example.data.repository.JimmyRepository
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.ensureFirebaseInitialized
import com.example.ui.auth.getSafeFirebaseAuth
import com.example.ui.auth.getSafeFirestore
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideoChatScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyTextPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ActiveAppScreen
import com.example.ui.viewmodel.JimmyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Global Exception Handler to capture startup crashes and log to Firebase Crashlytics
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("JimmyMainActivity", "Global uncaught exception on thread ${thread.name}", throwable)
            runCatching {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(throwable)
            }
            previousHandler?.uncaughtException(thread, throwable)
        }

        try {
            android.util.Log.i("JimmyMainActivity", "MainActivity.onCreate starting initialization...")
            runCatching {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("JIMMY MainActivity onCreate initiated")
            }
            runCatching { ensureFirebaseInitialized(this) }
            runCatching { enableEdgeToEdge() }
        } catch (e: Throwable) {
            android.util.Log.e("JimmyMainActivity", "Error during MainActivity initialization", e)
            runCatching {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            }
        }

        setContent {
            MyApplicationTheme {
                var startupError by remember { mutableStateOf<Throwable?>(null) }
                if (startupError != null) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = JimmyNavyDarkest
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.Text(
                                text = "JIMMY Startup Error",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                color = JimmyOrangeBright
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.Text(
                                text = startupError?.localizedMessage ?: "An unexpected error occurred during startup.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                color = JimmyTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            androidx.compose.material3.Button(
                                onClick = { startupError = null },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = JimmyOrangeBright
                                )
                            ) {
                                androidx.compose.material3.Text("Retry", color = Color.White)
                            }
                        }
                    }
                } else {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        runCatching {
                            // Verify safe entry point state
                        }.onFailure { err ->
                            startupError = err
                            runCatching {
                                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(err)
                            }
                        }
                    }
                    JimmyAppRoot()
                }
            }
        }
    }
}

@Composable
fun JimmyAppRoot(authOverride: FirebaseAuth? = null) {
    val context = LocalContext.current
    val auth = remember(authOverride, context) {
        authOverride ?: getSafeFirebaseAuth(context)
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    
    // Stable guest user ID for instant entry without Google Sign-In requirement
    val guestUserId = rememberSaveable { "jimmy_guest_${java.util.UUID.randomUUID().toString().take(12)}" }
    val currentUserId = auth?.currentUser?.uid ?: guestUserId
    val displayName = auth?.currentUser?.displayName ?: "Jimmy Explorer"

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    // Instantiate JimmyViewModel immediately for instant access without sign-in gate
    val appContext = context.applicationContext ?: context
    val viewModel: JimmyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val resolvedContext = this[APPLICATION_KEY] ?: appContext
                val db = getSafeFirestore(resolvedContext)
                JimmyViewModel(
                    repository = JimmyRepository(db, auth),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.activeScreen) {
        ActiveAppScreen.WELCOME -> {
            WelcomeScreen(
                isAuthenticated = true,
                userDisplayName = uiState.displayName,
                yourCountry = uiState.yourCountry,
                connectWithCountry = uiState.connectWithCountry,
                ageVerified18Plus = uiState.ageVerified18Plus,
                toastMessage = uiState.toastMessage,
                onSelectYourCountry = viewModel::selectYourCountry,
                onSelectConnectWith = viewModel::selectConnectWithCountry,
                onToggleAgeVerified = viewModel::setAgeVerified,
                onStartVideoChat = viewModel::startVideoChatSession,
                onAuthSuccess = {},
                onSignOut = {},
                onClearToast = viewModel::clearToastMessage
            )
        }
        ActiveAppScreen.VIDEO_CHAT -> {
            VideoChatScreen(
                uiState = uiState,
                onToggleChatInput = viewModel::toggleChatInput,
                onDraftMessageChanged = viewModel::updateDraftMessage,
                onSendMessage = viewModel::sendCurrentMessage,
                onNextClicked = viewModel::onNextStrangerClicked,
                onSwitchCamera = viewModel::switchCameraLens,
                onToggleMute = viewModel::toggleMicrophoneMute,
                onLocalFrameCaptured = viewModel::publishLocalVideoFrame,
                onReportOrBlock = viewModel::reportOrBlockCurrentPartner,
                onLeaveCall = viewModel::leaveVideoChatToWelcome,
                onClearToast = viewModel::clearToastMessage
            )
        }
    }
}
