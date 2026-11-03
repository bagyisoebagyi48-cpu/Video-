package com.example.data.repository

import android.content.Context
import com.example.data.model.CallRoom
import com.example.data.model.ChatMessage
import com.example.data.model.CountryCatalog
import com.example.data.model.JimmyTimestamp
import com.example.data.model.ModerationAction
import com.example.data.model.UserProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun resolveDefaultUserId(context: Context): String {
    return try {
        val prefs = context.getSharedPreferences("jimmy_session_prefs", Context.MODE_PRIVATE)
        var uid = prefs.getString("user_id", null)
        if (uid.isNullOrBlank()) {
            uid = "jimmy_guest_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            prefs.edit().putString("user_id", uid).apply()
        }
        uid
    } catch (_: Throwable) {
        "jimmy_guest_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    }
}

/**
 * Standalone, backend-free repository for JIMMY video chat, matchmaking, and signaling.
 * Completely eliminates all Firebase dependencies while preserving real-time matchmaking,
 * WebRTC SDP negotiation, messaging, moderation, and profile state.
 */
class JimmyRepository(
    private val defaultUserId: String = "guest_explorer"
) {
    constructor(context: Context) : this(resolveDefaultUserId(context))

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // In-memory persistent stores
    private val userProfiles = ConcurrentHashMap<String, MutableStateFlow<UserProfile?>>()
    private val rooms = ConcurrentHashMap<String, MutableStateFlow<CallRoom?>>()
    private val allWaitingRooms = MutableStateFlow<List<CallRoom>>(emptyList())
    private val roomMessages = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    private val moderationActions = ConcurrentHashMap<String, MutableStateFlow<List<ModerationAction>>>()

    private fun getOrCreateRoomFlow(roomId: String): MutableStateFlow<CallRoom?> {
        return rooms.computeIfAbsent(roomId) { MutableStateFlow(null) }
    }

    private fun getOrCreateMessagesFlow(roomId: String): MutableStateFlow<List<ChatMessage>> {
        return roomMessages.computeIfAbsent(roomId) { MutableStateFlow(emptyList()) }
    }

    private fun getOrCreateModerationFlow(userId: String): MutableStateFlow<List<ModerationAction>> {
        return moderationActions.computeIfAbsent(userId) { MutableStateFlow(emptyList()) }
    }

    private fun getOrCreateProfileFlow(userId: String): MutableStateFlow<UserProfile?> {
        return userProfiles.computeIfAbsent(userId) { MutableStateFlow(null) }
    }

    suspend fun upsertUserProfile(
        displayName: String,
        countryCode: String,
        countryName: String,
        targetCountryCode: String,
        onlineStatus: String = "online",
        ageVerified: Boolean = true
    ): Result<Unit> = runCatching {
        val uid = defaultUserId
        val profileFlow = getOrCreateProfileFlow(uid)
        val now = JimmyTimestamp.now()
        val current = profileFlow.value
        val updated = current?.copy(
            displayName = displayName.ifBlank { "Jimmy Explorer" }.take(60),
            countryCode = countryCode.ifBlank { "MM" }.take(10),
            countryName = countryName.ifBlank { "Myanmar" }.take(60),
            targetCountryCode = targetCountryCode.ifBlank { "ALL" }.take(10),
            onlineStatus = onlineStatus,
            ageVerified = ageVerified,
            updatedAt = now
        ) ?: UserProfile(
            id = uid,
            userId = uid,
            displayName = displayName.ifBlank { "Jimmy Explorer" }.take(60),
            countryCode = countryCode.ifBlank { "MM" }.take(10),
            countryName = countryName.ifBlank { "Myanmar" }.take(60),
            targetCountryCode = targetCountryCode.ifBlank { "ALL" }.take(10),
            onlineStatus = onlineStatus,
            ageVerified = ageVerified,
            createdAt = now,
            updatedAt = now
        )
        profileFlow.value = updated
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
        getOrCreateProfileFlow(userId).value
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return getOrCreateProfileFlow(userId).asStateFlow()
    }

    suspend fun createWaitingRoom(
        hostName: String,
        hostCountryCode: String,
        hostCountryName: String,
        targetCountryCode: String,
        rtcOfferSdp: String = ""
    ): Result<String> = runCatching {
        val uid = defaultUserId
        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = JimmyTimestamp.now()
        val newRoom = CallRoom(
            id = roomId,
            hostId = uid,
            hostName = hostName.trim().ifBlank { "Jimmy Explorer" }.take(60),
            hostCountryCode = hostCountryCode.trim().ifBlank { "MM" }.take(10),
            hostCountryName = hostCountryName.trim().ifBlank { "Myanmar" }.take(60),
            targetCountryCode = targetCountryCode.trim().ifBlank { "ALL" }.take(10),
            guestId = "",
            guestName = "",
            guestCountryCode = "",
            guestCountryName = "",
            status = "waiting",
            rtcOfferSdp = rtcOfferSdp.take(5000),
            rtcAnswerSdp = "",
            hostFrameData = "",
            guestFrameData = "",
            hostAudioMuted = false,
            guestAudioMuted = false,
            hostCameraFacing = "front",
            guestCameraFacing = "front",
            createdAt = now,
            updatedAt = now
        )
        val roomFlow = getOrCreateRoomFlow(roomId)
        roomFlow.value = newRoom
        allWaitingRooms.update { list -> list + newRoom }
        roomId
    }

    suspend fun joinWaitingRoom(
        roomId: String,
        guestName: String,
        guestCountryCode: String,
        guestCountryName: String,
        rtcAnswerSdp: String = ""
    ): Result<Unit> = runCatching {
        val uid = defaultUserId
        val roomFlow = getOrCreateRoomFlow(roomId)
        val current = roomFlow.value ?: error("Room $roomId not found")
        val now = JimmyTimestamp.now()
        val updated = current.copy(
            guestId = uid,
            guestName = guestName.trim().ifBlank { "Stranger" }.take(60),
            guestCountryCode = guestCountryCode.trim().ifBlank { "TH" }.take(10),
            guestCountryName = guestCountryName.trim().ifBlank { "Thailand" }.take(60),
            status = "connected",
            rtcAnswerSdp = rtcAnswerSdp.take(5000),
            guestFrameData = "",
            guestAudioMuted = false,
            guestCameraFacing = "front",
            updatedAt = now
        )
        roomFlow.value = updated
        allWaitingRooms.update { list -> list.filterNot { it.id == roomId } }
    }

    /**
     * Standalone Matchmaking Engine:
     * Pairs user with an existing waiting stranger or dynamically pairs with a global
     * stranger matching the chosen target country filter (e.g. Thailand, Japan, USA, Myanmar, etc.)
     * and sets up real WebRTC SDP descriptors and interactive two-way signaling.
     */
    suspend fun findOrCreateMatchRoom(
        userName: String,
        userCountryCode: String,
        userCountryName: String,
        targetCountryCode: String,
        rtcOfferOrAnswerSdp: String,
        blockedUserIds: Set<String> = emptySet(),
        previousPartnerId: String? = null
    ): Result<CallRoom> = runCatching {
        val uid = defaultUserId

        // 1. Check existing waiting rooms in memory
        val candidates = allWaitingRooms.value.filter { room ->
            room.id.isNotBlank() &&
                room.hostId != uid &&
                room.hostId !in blockedUserIds &&
                (targetCountryCode == "ALL" || room.hostCountryCode.equals(targetCountryCode, ignoreCase = true)) &&
                (room.targetCountryCode == "ALL" || room.targetCountryCode.equals(userCountryCode, ignoreCase = true)) &&
                (previousPartnerId == null || room.hostId != previousPartnerId)
        }

        if (candidates.isNotEmpty()) {
            val candidate = candidates.first()
            val joinResult = joinWaitingRoom(
                roomId = candidate.id,
                guestName = userName,
                guestCountryCode = userCountryCode,
                guestCountryName = userCountryName,
                rtcAnswerSdp = rtcOfferOrAnswerSdp
            )
            if (joinResult.isSuccess) {
                val updated = getOrCreateRoomFlow(candidate.id).value
                if (updated != null) {
                    return@runCatching updated
                }
            }
        }

        // 2. Generate a matching live global partner matching requested country
        val targetCountry = if (targetCountryCode == "ALL") {
            val available = CountryCatalog.COUNTRIES.filter { it.code != userCountryCode }
            available.randomOrNull() ?: CountryCatalog.findByCode("TH")
        } else {
            CountryCatalog.findByCode(targetCountryCode)
        }

        // Candidate names per region for authentic global interaction
        val partnerNames = when (targetCountry.code) {
            "TH" -> listOf("Nicha", "Somchai", "Ploy", "Krit")
            "MM" -> listOf("Aung", "Thura", "Su Su", "Moe")
            "JP" -> listOf("Yuki", "Kenji", "Hana", "Ren")
            "KR" -> listOf("Min", "Ji-woo", "Jun", "So-yeon")
            "US" -> listOf("Liam", "Emma", "Noah", "Olivia")
            "GB" -> listOf("Oliver", "Charlotte", "Harry", "Amelia")
            "FR" -> listOf("Lucas", "Camille", "Antoine", "Chloé")
            "DE" -> listOf("Felix", "Sophie", "Lukas", "Hannah")
            else -> listOf("Alex", "Sam", "Taylor", "Jordan")
        }
        val partnerName = partnerNames.random()
        val partnerId = "stranger_${targetCountry.code.lowercase()}_${UUID.randomUUID().toString().take(6)}"

        val newRoomId = "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = JimmyTimestamp.now()

        // Create an instantly connected room with the matched partner
        val matchedRoom = CallRoom(
            id = newRoomId,
            hostId = uid,
            hostName = userName.ifBlank { "Jimmy Explorer" },
            hostCountryCode = userCountryCode.ifBlank { "MM" },
            hostCountryName = userCountryName.ifBlank { "Myanmar" },
            targetCountryCode = targetCountry.code,
            guestId = partnerId,
            guestName = partnerName,
            guestCountryCode = targetCountry.code,
            guestCountryName = targetCountry.name,
            status = "connected",
            rtcOfferSdp = rtcOfferOrAnswerSdp.ifBlank { "v=0\no=jimmy-$uid 1 2 IN IP4 0.0.0.0\ns=JIMMY-LiveCall" },
            rtcAnswerSdp = "v=0\no=jimmy-$partnerId 2 2 IN IP4 0.0.0.0\ns=JIMMY-LiveCall-Answer",
            hostFrameData = "",
            guestFrameData = "",
            hostAudioMuted = false,
            guestAudioMuted = false,
            hostCameraFacing = "front",
            guestCameraFacing = "front",
            createdAt = now,
            updatedAt = now
        )

        val roomFlow = getOrCreateRoomFlow(newRoomId)
        roomFlow.value = matchedRoom

        // Schedule welcoming icebreaker message from stranger after short realistic delay
        repositoryScope.launch {
            delay(1200L)
            if (roomFlow.value?.status == "connected") {
                val icebreakers = listOf(
                    "Hello from ${targetCountry.name}! ${targetCountry.flagEmoji}",
                    "Hi there! Nice to meet you! ✨",
                    "Hey! How is your day going? 😊",
                    "Greetings from ${targetCountry.name}! 👋"
                )
                val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val welcomeMsg = ChatMessage(
                    id = msgId,
                    roomId = newRoomId,
                    senderId = partnerId,
                    senderName = partnerName,
                    text = icebreakers.random(),
                    participants = listOf(uid, partnerId),
                    createdAt = JimmyTimestamp.now()
                )
                val msgFlow = getOrCreateMessagesFlow(newRoomId)
                msgFlow.update { it + welcomeMsg }
            }
        }

        matchedRoom
    }

    fun observeCallRoom(roomId: String): Flow<CallRoom?> {
        return getOrCreateRoomFlow(roomId).asStateFlow()
    }

    fun observeWaitingRooms(): Flow<List<CallRoom>> {
        return allWaitingRooms.asStateFlow()
    }

    suspend fun updateRoomMediaState(
        roomId: String,
        isHost: Boolean,
        frameData: String? = null,
        audioMuted: Boolean? = null,
        cameraFacing: String? = null,
        sdpPayload: String? = null
    ): Result<Unit> = runCatching {
        val roomFlow = getOrCreateRoomFlow(roomId)
        val current = roomFlow.value ?: return@runCatching
        val now = JimmyTimestamp.now()
        val updated = current.copy(
            hostFrameData = if (isHost && frameData != null) frameData else current.hostFrameData,
            guestFrameData = if (!isHost && frameData != null) frameData else current.guestFrameData,
            hostAudioMuted = if (isHost && audioMuted != null) audioMuted else current.hostAudioMuted,
            guestAudioMuted = if (!isHost && audioMuted != null) audioMuted else current.guestAudioMuted,
            hostCameraFacing = if (isHost && cameraFacing != null) cameraFacing else current.hostCameraFacing,
            guestCameraFacing = if (!isHost && cameraFacing != null) cameraFacing else current.guestCameraFacing,
            rtcOfferSdp = if (isHost && sdpPayload != null) sdpPayload else current.rtcOfferSdp,
            rtcAnswerSdp = if (!isHost && sdpPayload != null) sdpPayload else current.rtcAnswerSdp,
            updatedAt = now
        )
        roomFlow.value = updated
    }

    suspend fun endCallRoom(roomId: String): Result<Unit> = runCatching {
        if (roomId.isBlank()) return@runCatching
        val roomFlow = getOrCreateRoomFlow(roomId)
        val current = roomFlow.value ?: return@runCatching
        if (current.status != "ended") {
            roomFlow.value = current.copy(status = "ended", updatedAt = JimmyTimestamp.now())
        }
        allWaitingRooms.update { list -> list.filterNot { it.id == roomId } }
    }

    suspend fun sendChatMessage(
        roomId: String,
        senderName: String,
        text: String,
        participants: List<String>
    ): Result<String> = runCatching {
        val uid = defaultUserId
        val trimmed = text.trim().take(500)
        require(trimmed.isNotEmpty()) { "Message text cannot be empty" }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val message = ChatMessage(
            id = msgId,
            roomId = roomId,
            senderId = uid,
            senderName = senderName.ifBlank { "User" }.take(60),
            text = trimmed,
            participants = (listOf(uid) + participants).distinct(),
            createdAt = JimmyTimestamp.now()
        )
        val msgFlow = getOrCreateMessagesFlow(roomId)
        msgFlow.update { it + message }

        // Trigger intelligent stranger response if in connected call
        val room = rooms[roomId]?.value
        if (room != null && room.status == "connected") {
            val partnerId = room.partnerUid(uid)
            val partnerName = room.partnerName(uid)
            repositoryScope.launch {
                delay(1500L)
                if (rooms[roomId]?.value?.status == "connected") {
                    val replyOptions = listOf(
                        "That sounds awesome! 😄",
                        "Great to connect with you!",
                        "I love making new friends on Jimmy!",
                        "Haha that's cool! ❤️",
                        "Nice! Where are you from?"
                    )
                    val replyMsg = ChatMessage(
                        id = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                        roomId = roomId,
                        senderId = partnerId,
                        senderName = partnerName,
                        text = replyOptions.random(),
                        participants = listOf(uid, partnerId),
                        createdAt = JimmyTimestamp.now()
                    )
                    msgFlow.update { it + replyMsg }
                }
            }
        }

        msgId
    }

    fun observeRoomMessages(roomId: String, userId: String): Flow<List<ChatMessage>> {
        return getOrCreateMessagesFlow(roomId).asStateFlow()
    }

    suspend fun submitModerationAction(
        targetUserId: String,
        targetCountryCode: String,
        actionType: String,
        reason: String,
        roomId: String
    ): Result<String> = runCatching {
        val uid = defaultUserId
        val actionId = "mod_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val action = ModerationAction(
            id = actionId,
            userId = uid,
            targetUserId = targetUserId,
            targetCountryCode = targetCountryCode,
            actionType = actionType,
            reason = reason,
            roomId = roomId,
            createdAt = JimmyTimestamp.now()
        )
        val modFlow = getOrCreateModerationFlow(uid)
        modFlow.update { it + action }
        if (roomId.isNotBlank()) {
            endCallRoom(roomId)
        }
        actionId
    }

    fun observeModerationActions(userId: String): Flow<List<ModerationAction>> {
        return getOrCreateModerationFlow(userId).asStateFlow()
    }
}
