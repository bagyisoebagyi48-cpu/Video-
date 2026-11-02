package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.CountryCatalog
import com.example.ui.theme.JimmyBlueAccent
import com.example.ui.theme.JimmyBlueLight
import com.example.ui.theme.JimmyDangerRed
import com.example.ui.theme.JimmyGoldCrown
import com.example.ui.theme.JimmyNavyBorder
import com.example.ui.theme.JimmyNavyCard
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyNavyDeep
import com.example.ui.theme.JimmyNavySurface
import com.example.ui.theme.JimmyOnlineGreen
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyOrangeDeep
import com.example.ui.theme.JimmyOrangeLight
import com.example.ui.theme.JimmyTextMuted
import com.example.ui.theme.JimmyTextPrimary
import com.example.ui.theme.JimmyTextSecondary
import com.example.ui.video.LocalCameraPreviewSurface
import com.example.ui.video.RemoteStrangerVideoStage
import com.example.ui.viewmodel.JimmyUiState
import com.example.ui.viewmodel.MatchmakingState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoChatScreen(
    uiState: JimmyUiState,
    onToggleChatInput: () -> Unit,
    onDraftMessageChanged: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onNextClicked: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleMute: () -> Unit,
    onLocalFrameCaptured: (String) -> Unit,
    onReportOrBlock: (actionType: String, reason: String) -> Unit,
    onLeaveCall: () -> Unit,
    onClearToast: () -> Unit
) {
    BackHandler {
        onLeaveCall()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val chatListState = rememberLazyListState()

    var hasCameraAndAudioPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val cameraGranted = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        hasCameraAndAudioPermissions = cameraGranted && audioGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraAndAudioPermissions) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    val latestIsMicMuted by rememberUpdatedState(uiState.isMicMuted)
    val latestOnToggleMute by rememberUpdatedState(onToggleMute)

    // Safe lifecycle handling when app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Mute audio when backgrounded for privacy
                if (!latestIsMicMuted) {
                    latestOnToggleMute()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        if (!uiState.toastMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(uiState.toastMessage)
            onClearToast()
        }
    }

    // Automatically scroll to newest chat message
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            runCatching {
                chatListState.animateScrollToItem(uiState.chatMessages.size - 1)
            }
        }
    }

    var showModerationSheet by remember { mutableStateOf(false) }
    var showEmojiQuickRow by remember { mutableStateOf(false) }

    val activeRoom = uiState.activeRoom
    val isSearching = uiState.matchmakingState is MatchmakingState.Searching ||
        activeRoom == null ||
        activeRoom.status == "waiting"

    val partnerCountry = remember(activeRoom, uiState.connectWithCountry, uiState.currentUserId) {
        if (activeRoom != null && activeRoom.status == "connected") {
            val code = activeRoom.partnerCountryCode(uiState.currentUserId)
            CountryCatalog.findByCode(code)
        } else {
            uiState.connectWithCountry
        }
    }

    val partnerName = remember(activeRoom, uiState.currentUserId) {
        activeRoom?.partnerName(uiState.currentUserId) ?: "Stranger"
    }

    val searchingStatusText = when (val ms = uiState.matchmakingState) {
        is MatchmakingState.Searching -> ms.statusText
        is MatchmakingState.Error -> ms.message
        else -> stringResource(R.string.searching_for_stranger)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = JimmyNavyDarkest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(JimmyNavyDarkest)
                .testTag("video_chat_screen")
        ) {
            // Split Full-Screen Video Layout: Upper 50% Stranger, Lower 50% User
            Column(modifier = Modifier.fillMaxSize()) {
                // UPPER HALF: Stranger's Live Camera Video Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("stranger_video_stage")
                ) {
                    RemoteStrangerVideoStage(
                        isSearching = isSearching,
                        partnerName = partnerName,
                        partnerCountry = partnerCountry,
                        partnerFrameBase64 = activeRoom?.partnerFrameData(uiState.currentUserId).orEmpty(),
                        partnerAudioMuted = activeRoom?.partnerAudioMuted(uiState.currentUserId) ?: false,
                        searchingStatusText = searchingStatusText
                    )

                    // Top Overlay Bar: Stranger Country & Flag Badge (left) + Report & Camera Switch (right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stranger's Country + Flag + Online Dot Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.52f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.18f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("stranger_country_badge"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = partnerCountry.flagEmoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSearching) {
                                    "Searching • ${partnerCountry.name}"
                                } else {
                                    partnerCountry.name
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSearching) JimmyOrangeBright else JimmyOnlineGreen
                                    )
                            )
                        }

                        // Top-right Controls: Safety Report/Block (!) + Switch Camera (⇄) + Leave (✕)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Report / Block Shield Button matching mockup
                            IconButton(
                                onClick = { showModerationSheet = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.52f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .testTag("report_block_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = "Report or Block Stranger",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Switch Camera Button matching mockup
                            IconButton(
                                onClick = onSwitchCamera,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.52f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .testTag("switch_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Leave to Welcome Screen Button
                            IconButton(
                                onClick = onLeaveCall,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.52f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .testTag("leave_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "End Call and Return",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Subtle 1dp glowing divider between Stranger and User halves
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    JimmyBlueAccent.copy(alpha = 0.6f),
                                    JimmyOrangeBright.copy(alpha = 0.8f),
                                    JimmyBlueAccent.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // LOWER HALF: User's Own Live Camera Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("local_camera_stage")
                ) {
                    if (hasCameraAndAudioPermissions) {
                        LocalCameraPreviewSurface(
                            useFrontCamera = uiState.useFrontCamera,
                            isMicMuted = uiState.isMicMuted,
                            onFrameCaptured = onLocalFrameCaptured,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Graceful Camera & Microphone Permission Request Card
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(JimmyNavyDeep)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = JimmyOrangeBright,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.camera_mic_permission_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = JimmyTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.camera_mic_permission_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = JimmyTextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JimmyOrangeBright
                                ),
                                modifier = Modifier.testTag("grant_permissions_button")
                            ) {
                                Text(text = stringResource(R.string.grant_permissions))
                            }
                        }
                    }

                    // Bottom gradient scrim for legible chat bubbles and controls over camera feed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.68f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Top-right of User's own camera half: Microphone Mute Toggle pill
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.50f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("mute_mic_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (uiState.isMicMuted) "Unmute Microphone" else "Mute Microphone",
                            tint = if (uiState.isMicMuted) JimmyDangerRed else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // IN-VIDEO TEXT CHAT OVERLAY & BOTTOM CONTROLS (Adjusts smoothly with keyboard via imePadding)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Real-time Floating Chat Bubbles over the lower video half
                if (uiState.chatMessages.isNotEmpty()) {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 170.dp)
                            .padding(bottom = 8.dp)
                            .testTag("in_video_chat_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = uiState.chatMessages,
                            key = { it.id.ifBlank { "${it.senderId}_${it.text}" } }
                        ) { message ->
                            val isMine = message.senderId == uiState.currentUserId
                            InVideoChatBubbleItem(
                                message = message,
                                isMine = isMine,
                                partnerFlag = partnerCountry.flagEmoji,
                                myFlag = uiState.yourCountry.flagEmoji
                            )
                        }
                    }
                }

                // Quick Emoji Row when toggled inside Chat Input
                AnimatedVisibility(
                    visible = uiState.isChatInputVisible && showEmojiQuickRow,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(JimmyNavyDeep.copy(alpha = 0.92f))
                            .border(1.dp, JimmyNavyBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("👋", "😊", "❤️", "🔥", "😂", "🎉", "🌏").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onDraftMessageChanged(uiState.draftMessage + emoji)
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // In-Video Message Input Bar (visible when Chat button is tapped)
                AnimatedVisibility(
                    visible = uiState.isChatInputVisible,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF101836).copy(alpha = 0.90f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.22f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showEmojiQuickRow = !showEmojiQuickRow },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SentimentSatisfiedAlt,
                                    contentDescription = "Toggle Emojis",
                                    tint = JimmyTextSecondary
                                )
                            }
                            TextField(
                                value = uiState.draftMessage,
                                onValueChange = onDraftMessageChanged,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.type_message_hint),
                                        color = JimmyTextMuted,
                                        fontSize = 14.sp
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = { onSendMessage(null) }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input")
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Vibrant Blue Circular Send Button matching the mockup
                        IconButton(
                            onClick = { onSendMessage(null) },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(JimmyBlueLight, JimmyBlueAccent)
                                    )
                                )
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Bottom Control Bar: Chat Button (left) + Large Orange Next Button (center) + Gift/Wave Button (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chat Toggle Button
                    Column(
                        modifier = Modifier
                            .width(84.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (uiState.isChatInputVisible) {
                                    JimmyBlueAccent.copy(alpha = 0.35f)
                                } else {
                                    Color(0xFF121B3A).copy(alpha = 0.88f)
                                }
                            )
                            .border(
                                1.dp,
                                if (uiState.isChatInputVisible) JimmyBlueLight
                                else Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(30.dp)
                            )
                            .clickable(onClick = onToggleChatInput)
                            .testTag("toggle_chat_button"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = stringResource(R.string.chat_button_label),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.chat_button_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    // Center: Large Orange Gradient NEXT Button
                    Button(
                        onClick = onNextClicked,
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(JimmyOrangeLight, JimmyOrangeBright, JimmyOrangeDeep)
                                )
                            )
                            .testTag("next_stranger_button")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = stringResource(R.string.next_button_label),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = stringResource(R.string.next_button_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }

                    // Right: Quick Icebreaker / Gift Greeting Button
                    Column(
                        modifier = Modifier
                            .width(84.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF121B3A).copy(alpha = 0.88f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(30.dp)
                            )
                            .clickable {
                                onSendMessage("Hello 👋 Nice to meet you! 🎁")
                            }
                            .testTag("send_icebreaker_button"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Send Greeting Gift",
                            tint = JimmyGoldCrown,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Gift",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Safety & Moderation Bottom Sheet (Report / Block User -> Immediately ends call)
    if (showModerationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModerationSheet = false },
            containerColor = JimmyNavySurface,
            contentColor = JimmyTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_or_block_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = JimmyTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reporting or blocking immediately disconnects this call and prevents matching with this user again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JimmyTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                listOf(
                    "Inappropriate or explicit content",
                    "Harassment or abusive language",
                    "Spam, bot, or underage user"
                ).forEachIndexed { idx, reason ->
                    Button(
                        onClick = {
                            showModerationSheet = false
                            onReportOrBlock("report", reason)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JimmyDangerRed.copy(alpha = 0.18f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("report_reason_$idx")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = JimmyDangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Report: $reason")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showModerationSheet = false
                        onReportOrBlock("block", "Blocked by user")
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_block_user_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = JimmyOrangeLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.block_user_action),
                        color = JimmyOrangeLight
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun InVideoChatBubbleItem(
    message: ChatMessage,
    isMine: Boolean,
    partnerFlag: String,
    myFlag: String
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.createdAt) {
        val date = message.createdAt?.toDate()
        if (date != null) timeFormatter.format(date) else "now"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(JimmyNavyCard)
                    .border(1.dp, JimmyOrangeLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = partnerFlag, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Row(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isMine) {
                        Brush.horizontalGradient(
                            colors = listOf(JimmyBlueAccent, JimmyBlueLight)
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1B243B).copy(alpha = 0.88f),
                                Color(0xFF263252).copy(alpha = 0.88f)
                            )
                        )
                    }
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isMine) "$timeString ✓✓" else timeString,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp
            )
        }

        if (isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(JimmyNavyCard)
                    .border(1.dp, JimmyBlueLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = myFlag, fontSize = 15.sp)
            }
        }
    }
}
