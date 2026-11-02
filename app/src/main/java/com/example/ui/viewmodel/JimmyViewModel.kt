package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallRoom
import com.example.data.model.ChatMessage
import com.example.data.model.CountryCatalog
import com.example.data.model.CountryOption
import com.example.data.repository.JimmyRepository
import com.example.ui.video.WebRtcSessionDescriptor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActiveAppScreen {
    WELCOME,
    VIDEO_CHAT
}

sealed interface MatchmakingState {
    data object Idle : MatchmakingState
    data class Searching(
        val roomId: String = "",
        val statusText: String = "Searching for a stranger..."
    ) : MatchmakingState
    data class Connected(val room: CallRoom) : MatchmakingState
    data class Error(val message: String) : MatchmakingState
}

data class JimmyUiState(
    val currentUserId: String = "",
    val displayName: String = "Jimmy Explorer",
    val yourCountry: CountryOption = CountryCatalog.detectDeviceCountry(),
    val connectWithCountry: CountryOption = CountryCatalog.ALL_COUNTRIES,
    val ageVerified18Plus: Boolean = true,
    val activeScreen: ActiveAppScreen = ActiveAppScreen.WELCOME,
    val matchmakingState: MatchmakingState = MatchmakingState.Idle,
    val activeRoom: CallRoom? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatInputVisible: Boolean = false,
    val draftMessage: String = "",
    val useFrontCamera: Boolean = true,
    val isMicMuted: Boolean = false,
    val blockedUserIds: Set<String> = emptySet(),
    val previousPartnerId: String? = null,
    val toastMessage: String? = null
)

class JimmyViewModel(
    private val repository: JimmyRepository,
    private val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        JimmyUiState(
            currentUserId = currentUserId,
            displayName = initialDisplayName.ifBlank { "Jimmy Explorer" },
            yourCountry = CountryCatalog.detectDeviceCountry(),
            connectWithCountry = CountryCatalog.ALL_COUNTRIES
        )
    )
    val uiState: StateFlow<JimmyUiState> = _uiState.asStateFlow()

    private var roomObserverJob: Job? = null
    private var waitingPoolObserverJob: Job? = null
    private var messagesObserverJob: Job? = null
    private var matchmakingJob: Job? = null
    private var lastNextTapTimeMs: Long = 0L
    private var lastMessageSentTimeMs: Long = 0L

    init {
        initializeAuthenticatedSession()
    }

    private fun initializeAuthenticatedSession() {
        // 1. Load existing UserProfile if saved
        viewModelScope.launch {
            val existing = repository.getUserProfile(currentUserId).getOrNull()
            if (existing != null) {
                val myCountry = CountryCatalog.findByCode(existing.countryCode)
                val targetCountry = CountryCatalog.findByCode(existing.targetCountryCode)
                _uiState.update { state ->
                    state.copy(
                        displayName = existing.displayName.ifBlank { state.displayName },
                        yourCountry = myCountry,
                        connectWithCountry = targetCountry,
                        ageVerified18Plus = existing.ageVerified
                    )
                }
            } else {
                val state = _uiState.value
                repository.upsertUserProfile(
                    displayName = state.displayName,
                    countryCode = state.yourCountry.code,
                    countryName = state.yourCountry.name,
                    targetCountryCode = state.connectWithCountry.code,
                    onlineStatus = "online",
                    ageVerified = state.ageVerified18Plus
                )
            }
        }

        // 2. Observe blocked users list from moderation_actions
        viewModelScope.launch {
            repository.observeModerationActions(currentUserId)
                .catch { e -> Log.w(TAG, "Error observing moderation actions", e) }
                .collect { actions ->
                    val blocked = actions.map { it.targetUserId }.toSet()
                    _uiState.update { it.copy(blockedUserIds = blocked) }
                }
        }
    }

    fun selectYourCountry(country: CountryOption) {
        _uiState.update { it.copy(yourCountry = country) }
        syncProfileToFirestore(onlineStatus = "online")
    }

    fun selectConnectWithCountry(country: CountryOption) {
        _uiState.update { it.copy(connectWithCountry = country) }
        syncProfileToFirestore(onlineStatus = "online")
    }

    fun setAgeVerified(verified: Boolean) {
        _uiState.update { it.copy(ageVerified18Plus = verified) }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun startVideoChatSession() {
        val state = _uiState.value
        if (!state.ageVerified18Plus) {
            _uiState.update {
                it.copy(toastMessage = "You must confirm that you are 18+ to start video chat.")
            }
            return
        }
        _uiState.update {
            it.copy(
                activeScreen = ActiveAppScreen.VIDEO_CHAT,
                chatMessages = emptyList(),
                draftMessage = "",
                isChatInputVisible = false
            )
        }
        startMatchmakingSearch()
    }

    fun leaveVideoChatToWelcome() {
        val currentRoomId = _uiState.value.activeRoom?.id.orEmpty()
        cancelActiveRoomListeners()
        if (currentRoomId.isNotBlank()) {
            viewModelScope.launch {
                repository.endCallRoom(currentRoomId)
            }
        }
        syncProfileToFirestore(onlineStatus = "online")
        _uiState.update {
            it.copy(
                activeScreen = ActiveAppScreen.WELCOME,
                matchmakingState = MatchmakingState.Idle,
                activeRoom = null,
                chatMessages = emptyList(),
                draftMessage = "",
                isChatInputVisible = false
            )
        }
    }

    fun onNextStrangerClicked() {
        val now = System.currentTimeMillis()
        if (now - lastNextTapTimeMs < 700L) {
            return // Rate limit rapid taps
        }
        lastNextTapTimeMs = now

        val previousRoom = _uiState.value.activeRoom
        val previousPartner = previousRoom?.partnerUid(currentUserId)?.takeIf { it.isNotBlank() }

        cancelActiveRoomListeners()
        _uiState.update {
            it.copy(
                activeRoom = null,
                chatMessages = emptyList(),
                draftMessage = "",
                previousPartnerId = previousPartner ?: it.previousPartnerId,
                matchmakingState = MatchmakingState.Searching(
                    statusText = "Searching for a stranger..."
                )
            )
        }

        viewModelScope.launch {
            if (!previousRoom?.id.isNullOrBlank()) {
                repository.endCallRoom(previousRoom!!.id)
            }
            startMatchmakingSearch()
        }
    }

    private fun startMatchmakingSearch() {
        matchmakingJob?.cancel()
        matchmakingJob = viewModelScope.launch {
            val state = _uiState.value
            val targetLabel = if (state.connectWithCountry.code == "ALL") {
                "Worldwide"
            } else {
                "${state.connectWithCountry.flagEmoji} ${state.connectWithCountry.name}"
            }

            _uiState.update {
                it.copy(
                    matchmakingState = MatchmakingState.Searching(
                        statusText = "Searching in $targetLabel..."
                    ),
                    chatMessages = emptyList()
                )
            }
            syncProfileToFirestore(onlineStatus = "searching")

            val sdpOffer = WebRtcSessionDescriptor.createOfferSdp(
                userId = currentUserId,
                cameraFacing = if (state.useFrontCamera) "front" else "back",
                audioMuted = state.isMicMuted
            )

            val matchResult = repository.findOrCreateMatchRoom(
                userName = state.displayName,
                userCountryCode = state.yourCountry.code,
                userCountryName = state.yourCountry.name,
                targetCountryCode = state.connectWithCountry.code,
                rtcOfferOrAnswerSdp = sdpOffer,
                blockedUserIds = state.blockedUserIds,
                previousPartnerId = state.previousPartnerId
            )

            matchResult.onSuccess { room ->
                _uiState.update { it.copy(activeRoom = room) }
                observeActiveCallRoom(room.id)
                if (room.status == "waiting") {
                    _uiState.update {
                        it.copy(
                            matchmakingState = MatchmakingState.Searching(
                                roomId = room.id,
                                statusText = "Waiting for an available partner ($targetLabel)..."
                            )
                        )
                    }
                    observeWaitingPoolWhileSearching(myWaitingRoomId = room.id)
                }
            }.onFailure { err ->
                Log.w(TAG, "Matchmaking failed", err)
                _uiState.update {
                    it.copy(
                        matchmakingState = MatchmakingState.Error(
                            err.localizedMessage ?: "Network error while searching. Tap Next to retry."
                        )
                    )
                }
            }
        }
    }

    private fun observeActiveCallRoom(roomId: String) {
        roomObserverJob?.cancel()
        roomObserverJob = viewModelScope.launch {
            repository.observeCallRoom(roomId)
                .catch { e ->
                    Log.w(TAG, "Error observing call room $roomId", e)
                }
                .collect { room ->
                    if (room == null) return@collect
                    _uiState.update { it.copy(activeRoom = room) }

                    when (room.status) {
                        "waiting" -> {
                            val targetCountry = _uiState.value.connectWithCountry
                            _uiState.update {
                                it.copy(
                                    matchmakingState = MatchmakingState.Searching(
                                        roomId = room.id,
                                        statusText = "Waiting for stranger in ${targetCountry.flagEmoji} ${targetCountry.name}..."
                                    )
                                )
                            }
                        }
                        "connected" -> {
                            waitingPoolObserverJob?.cancel()
                            val wasAlreadyConnected =
                                _uiState.value.matchmakingState is MatchmakingState.Connected
                            _uiState.update {
                                it.copy(matchmakingState = MatchmakingState.Connected(room))
                            }
                            if (!wasAlreadyConnected) {
                                syncProfileToFirestore(onlineStatus = "in_call")
                                startObservingMessages(room.id)
                            }
                        }
                        "ended" -> {
                            // Stranger disconnected or tapped Next -> automatically search for next stranger!
                            val partnerId = room.partnerUid(currentUserId).takeIf { it.isNotBlank() }
                            _uiState.update {
                                it.copy(
                                    toastMessage = "Stranger disconnected. Searching for next partner...",
                                    previousPartnerId = partnerId ?: it.previousPartnerId
                                )
                            }
                            delay(400L)
                            startMatchmakingSearch()
                        }
                    }
                }
        }
    }

    private fun observeWaitingPoolWhileSearching(myWaitingRoomId: String) {
        waitingPoolObserverJob?.cancel()
        waitingPoolObserverJob = viewModelScope.launch {
            repository.observeWaitingRooms()
                .catch { e -> Log.w(TAG, "Waiting pool listener error", e) }
                .collect { rooms ->
                    val state = _uiState.value
                    val currentRoom = state.activeRoom
                    if (currentRoom != null && currentRoom.status == "connected") return@collect

                    val candidate = rooms.firstOrNull { room ->
                        room.id.isNotBlank() &&
                            room.id != myWaitingRoomId &&
                            room.hostId != currentUserId &&
                            room.hostId !in state.blockedUserIds &&
                            (state.connectWithCountry.code == "ALL" ||
                                room.hostCountryCode.equals(state.connectWithCountry.code, ignoreCase = true)) &&
                            (room.targetCountryCode == "ALL" ||
                                room.targetCountryCode.equals(state.yourCountry.code, ignoreCase = true)) &&
                            // Deterministic tie-breaker so two waiting hosts don't both abandon their room simultaneously
                            currentUserId < room.hostId
                    }

                    if (candidate != null) {
                        waitingPoolObserverJob?.cancel()
                        roomObserverJob?.cancel()
                        repository.endCallRoom(myWaitingRoomId)
                        val joinRes = repository.joinWaitingRoom(
                            roomId = candidate.id,
                            guestName = state.displayName,
                            guestCountryCode = state.yourCountry.code,
                            guestCountryName = state.yourCountry.name,
                            rtcAnswerSdp = WebRtcSessionDescriptor.createOfferSdp(
                                userId = currentUserId,
                                cameraFacing = if (state.useFrontCamera) "front" else "back",
                                audioMuted = state.isMicMuted
                            )
                        )
                        if (joinRes.isSuccess) {
                            observeActiveCallRoom(candidate.id)
                        } else {
                            startMatchmakingSearch()
                        }
                    }
                }
        }
    }

    private fun startObservingMessages(roomId: String) {
        messagesObserverJob?.cancel()
        messagesObserverJob = viewModelScope.launch {
            repository.observeRoomMessages(roomId, currentUserId)
                .catch { e -> Log.w(TAG, "Error observing chat messages", e) }
                .collect { messages ->
                    _uiState.update { it.copy(chatMessages = messages) }
                }
        }
    }

    fun toggleChatInput() {
        _uiState.update { it.copy(isChatInputVisible = !it.isChatInputVisible) }
    }

    fun updateDraftMessage(text: String) {
        _uiState.update { it.copy(draftMessage = text.take(500)) }
    }

    fun sendCurrentMessage(customText: String? = null) {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        val textToSend = (customText ?: state.draftMessage).trim()
        if (textToSend.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastMessageSentTimeMs < 350L) {
            _uiState.update { it.copy(toastMessage = "Sending too fast. Please wait a moment.") }
            return
        }
        lastMessageSentTimeMs = now

        if (customText == null) {
            _uiState.update { it.copy(draftMessage = "") }
        }

        val participants = listOf(room.hostId, room.guestId).filter { it.isNotBlank() }.distinct()
        viewModelScope.launch {
            repository.sendChatMessage(
                roomId = room.id,
                senderName = state.displayName,
                text = textToSend,
                participants = participants
            ).onFailure { err ->
                _uiState.update {
                    it.copy(toastMessage = err.localizedMessage ?: "Failed to send message")
                }
            }
        }
    }

    fun switchCameraLens() {
        val newFront = !_uiState.value.useFrontCamera
        _uiState.update { it.copy(useFrontCamera = newFront) }
        val room = _uiState.value.activeRoom ?: return
        if (room.status != "ended") {
            viewModelScope.launch {
                repository.updateRoomMediaState(
                    roomId = room.id,
                    isHost = room.isHost(currentUserId),
                    cameraFacing = if (newFront) "front" else "back"
                )
            }
        }
    }

    fun toggleMicrophoneMute() {
        val newMuted = !_uiState.value.isMicMuted
        _uiState.update { it.copy(isMicMuted = newMuted) }
        val room = _uiState.value.activeRoom ?: return
        if (room.status != "ended") {
            viewModelScope.launch {
                repository.updateRoomMediaState(
                    roomId = room.id,
                    isHost = room.isHost(currentUserId),
                    audioMuted = newMuted
                )
            }
        }
    }

    fun publishLocalVideoFrame(base64Frame: String) {
        val room = _uiState.value.activeRoom ?: return
        if (room.status != "connected") return
        viewModelScope.launch {
            repository.updateRoomMediaState(
                roomId = room.id,
                isHost = room.isHost(currentUserId),
                frameData = base64Frame
            )
        }
    }

    fun reportOrBlockCurrentPartner(actionType: String, reason: String) {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        val partnerUid = room.partnerUid(currentUserId)
        val partnerCountryCode = room.partnerCountryCode(currentUserId)

        if (partnerUid.isBlank()) {
            onNextStrangerClicked()
            return
        }

        viewModelScope.launch {
            repository.submitModerationAction(
                targetUserId = partnerUid,
                targetCountryCode = partnerCountryCode,
                actionType = actionType,
                reason = reason,
                roomId = room.id
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        blockedUserIds = it.blockedUserIds + partnerUid,
                        toastMessage = if (actionType == "block") {
                            "User blocked. Searching for a new stranger..."
                        } else {
                            "Report submitted & user blocked. Searching for next..."
                        }
                    )
                }
                onNextStrangerClicked()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(toastMessage = err.localizedMessage ?: "Could not submit report")
                }
                onNextStrangerClicked()
            }
        }
    }

    private fun syncProfileToFirestore(onlineStatus: String) {
        val state = _uiState.value
        viewModelScope.launch {
            repository.upsertUserProfile(
                displayName = state.displayName,
                countryCode = state.yourCountry.code,
                countryName = state.yourCountry.name,
                targetCountryCode = state.connectWithCountry.code,
                onlineStatus = onlineStatus,
                ageVerified = state.ageVerified18Plus
            )
        }
    }

    private fun cancelActiveRoomListeners() {
        matchmakingJob?.cancel()
        roomObserverJob?.cancel()
        waitingPoolObserverJob?.cancel()
        messagesObserverJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelActiveRoomListeners()
    }

    private companion object {
        const val TAG = "JimmyViewModel"
    }
}
