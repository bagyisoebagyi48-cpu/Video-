package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.CountryCatalog
import com.example.data.repository.JimmyRepository
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.ensureFirebaseInitialized
import com.example.ui.auth.getSafeFirebaseAuth
import com.example.ui.auth.getSafeFirestore
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideoChatScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ActiveAppScreen
import com.example.ui.viewmodel.JimmyViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureFirebaseInitialized(this)
        runCatching { enableEdgeToEdge() }
        setContent {
            MyApplicationTheme {
                JimmyAppRoot()
            }
        }
    }
}

@Composable
fun JimmyAppRoot(authOverride: FirebaseAuth? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember(authOverride, context) {
        authOverride ?: getSafeFirebaseAuth(context)
    }
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth?.currentUser)

    // Pre-auth local country selection state so the Welcome screen is interactive immediately
    var guestYourCountry by remember { mutableStateOf(CountryCatalog.detectDeviceCountry()) }
    var guestConnectWith by remember { mutableStateOf(CountryCatalog.ALL_COUNTRIES) }
    var guestAgeVerified by rememberSaveable { mutableStateOf(true) }
    var guestToast by remember { mutableStateOf<String?>(null) }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val authenticatedUser = currentUser
    if (authenticatedUser == null || auth == null) {
        // Navigation-Level Auth Gate: Do not instantiate JimmyViewModel or query Firestore while unauthenticated
        WelcomeScreen(
            isAuthenticated = false,
            userDisplayName = "",
            yourCountry = guestYourCountry,
            connectWithCountry = guestConnectWith,
            ageVerified18Plus = guestAgeVerified,
            toastMessage = guestToast,
            onSelectYourCountry = { guestYourCountry = it },
            onSelectConnectWith = { guestConnectWith = it },
            onToggleAgeVerified = { guestAgeVerified = it },
            onStartVideoChat = {},
            onAuthSuccess = {},
            onSignOut = {},
            onClearToast = { guestToast = null }
        )
    } else {
        // Authenticated session: Instantiate JimmyViewModel keyed by authenticated UID
        val viewModel: JimmyViewModel = viewModel(
            key = authenticatedUser.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val db = getSafeFirestore(app)
                    JimmyViewModel(
                        repository = JimmyRepository(db, auth),
                        currentUserId = authenticatedUser.uid,
                        initialDisplayName = authenticatedUser.displayName ?: "Jimmy Explorer"
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
}
