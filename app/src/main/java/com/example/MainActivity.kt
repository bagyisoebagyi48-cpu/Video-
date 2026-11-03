package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.JimmyRepository
import com.example.ui.auth.GuestSessionManager
import com.example.ui.diagnostics.CrashDiagnostics
import com.example.ui.diagnostics.CrashReport
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StartupDiagnosticsScreen
import com.example.ui.screens.VideoChatScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ActiveAppScreen
import com.example.ui.viewmodel.JimmyViewModel

class MainActivity : ComponentActivity() {

    private var activeCrashReport by mutableStateOf<CrashReport?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity.onCreate starting...")
        runCatching { enableEdgeToEdge() }

        try {
            setContent {
                MyApplicationTheme {
                    val report = activeCrashReport
                    if (report != null) {
                        StartupDiagnosticsScreen(
                            report = report,
                            onRetry = {
                                CrashDiagnostics.clearLastCrash(this@MainActivity)
                                activeCrashReport = null
                                recreate()
                            },
                            onResetData = {
                                CrashDiagnostics.clearAllData(this@MainActivity)
                                activeCrashReport = null
                                recreate()
                            }
                        )
                    } else {
                        JimmyAppRoot(
                            onFatalError = { error ->
                                Log.e(TAG, "Fatal Compose error captured", error)
                                CrashDiagnostics.recordCrash(this@MainActivity, Thread.currentThread(), error)
                                activeCrashReport = CrashDiagnostics.readLastCrash(this@MainActivity)
                            }
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Uncaught exception in MainActivity.onCreate", t)
            CrashDiagnostics.recordCrash(this, Thread.currentThread(), t)
            val report = CrashDiagnostics.readLastCrash(this)
            setContent {
                MyApplicationTheme {
                    if (report != null) {
                        StartupDiagnosticsScreen(
                            report = report,
                            onRetry = {
                                CrashDiagnostics.clearLastCrash(this@MainActivity)
                                recreate()
                            },
                            onResetData = {
                                CrashDiagnostics.clearAllData(this@MainActivity)
                                recreate()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "JimmyMainActivity"
    }
}

@Composable
fun JimmyAppRoot(
    onFatalError: (Throwable) -> Unit = {}
) {
    val context = LocalContext.current
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val guestUserId = remember(context) {
        runCatching { GuestSessionManager.getOrCreateUserId(context) }
            .getOrDefault("jimmy_guest_default")
    }
    val displayName = remember(context) {
        runCatching { GuestSessionManager.getDisplayName(context) }
            .getOrDefault("Jimmy Explorer")
    }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    // Standalone ViewModel with in-memory repository (Zero Firebase)
    val appContext = context.applicationContext ?: context
    val viewModel: JimmyViewModel = viewModel(
        key = guestUserId,
        factory = viewModelFactory {
            initializer {
                val resolvedContext = this[APPLICATION_KEY] ?: appContext
                try {
                    JimmyViewModel(
                        repository = JimmyRepository(resolvedContext),
                        currentUserId = guestUserId,
                        initialDisplayName = displayName
                    )
                } catch (t: Throwable) {
                    onFatalError(t)
                    JimmyViewModel(
                        repository = JimmyRepository(defaultUserId = guestUserId),
                        currentUserId = guestUserId,
                        initialDisplayName = displayName
                    )
                }
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
