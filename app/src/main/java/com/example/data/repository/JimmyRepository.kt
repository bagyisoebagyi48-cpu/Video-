package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CallRoom
import com.example.data.model.ChatMessage
import com.example.data.model.ModerationAction
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class JimmyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) {
    constructor(context: Context) : this(
        com.example.ui.auth.getSafeFirestore(context.applicationContext ?: context),
        com.example.ui.auth.getSafeFirebaseAuth(context.applicationContext ?: context)
    )

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun upsertUserProfile(
        displayName: String,
        countryCode: String,
        countryName: String,
        targetCountryCode: String,
        onlineStatus: String = "online",
        ageVerified: Boolean = true
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeName = displayName.trim().ifBlank { "Jimmy User" }.take(60)
        val safeCountryCode = countryCode.trim().ifBlank { "MM" }.take(10)
        val safeCountryName = countryName.trim().ifBlank { "Myanmar" }.take(60)
        val safeTargetCode = targetCountryCode.trim().ifBlank { "ALL" }.take(10)

        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updateData = mapOf(
                    "displayName" to safeName,
                    "countryCode" to safeCountryCode,
                    "countryName" to safeCountryName,
                    "targetCountryCode" to safeTargetCode,
                    "onlineStatus" to onlineStatus,
                    "ageVerified" to ageVerified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateData).await()
            } else {
                val createData = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "countryCode" to safeCountryCode,
                    "countryName" to safeCountryName,
                    "targetCountryCode" to safeTargetCode,
                    "onlineStatus" to onlineStatus,
                    "ageVerified" to ageVerified,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createData).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            val snap = docRef.get().await()
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun createWaitingRoom(
        hostName: String,
        hostCountryCode: String,
        hostCountryName: String,
        targetCountryCode: String,
        rtcOfferSdp: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("call_rooms").document(roomId)
        val payload = mapOf(
            "hostId" to uid,
            "hostName" to hostName.trim().ifBlank { "Jimmy Explorer" }.take(60),
            "hostCountryCode" to hostCountryCode.trim().ifBlank { "MM" }.take(10),
            "hostCountryName" to hostCountryName.trim().ifBlank { "Myanmar" }.take(60),
            "targetCountryCode" to targetCountryCode.trim().ifBlank { "ALL" }.take(10),
            "guestId" to "",
            "guestName" to "",
            "guestCountryCode" to "",
            "guestCountryName" to "",
            "status" to "waiting",
            "rtcOfferSdp" to rtcOfferSdp.take(5000),
            "rtcAnswerSdp" to "",
            "hostFrameData" to "",
            "guestFrameData" to "",
            "hostAudioMuted" to false,
            "guestAudioMuted" to false,
            "hostCameraFacing" to "front",
            "guestCameraFacing" to "front",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            roomId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun joinWaitingRoom(
        roomId: String,
        guestName: String,
        guestCountryCode: String,
        guestCountryName: String,
        rtcAnswerSdp: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("call_rooms").document(roomId)
        val updates = mapOf(
            "guestId" to uid,
            "guestName" to guestName.trim().ifBlank { "Stranger" }.take(60),
            "guestCountryCode" to guestCountryCode.trim().ifBlank { "TH" }.take(10),
            "guestCountryName" to guestCountryName.trim().ifBlank { "Thailand" }.take(60),
            "status" to "connected",
            "rtcAnswerSdp" to rtcAnswerSdp.take(5000),
            "guestFrameData" to "",
            "guestAudioMuted" to false,
            "guestCameraFacing" to "front",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Matchmaking engine:
     * 1. Queries waiting rooms (`status == "waiting"`).
     * 2. Excludes self (`hostId != uid`) and blocked users (`hostId !in blockedUserIds`).
     * 3. Matches destination country filter (`targetCountryCode == "ALL"` or `room.hostCountryCode == targetCountryCode`)
     *    and respects host's filter (`room.targetCountryCode == "ALL"` or `room.targetCountryCode == userCountryCode`).
     * 4. Prefers strangers different from `previousPartnerId` when alternatives exist.
     * 5. Attempts to join a matching room; if none available, creates a new waiting room.
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
        val uid = requireUserId()
        val path = "call_rooms"
        val waitingRooms = try {
            db.collection(path)
                .whereEqualTo("status", "waiting")
                .get()
                .await()
                .toObjects(CallRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }

        val compatibleRooms = waitingRooms.filter { room ->
            room.id.isNotBlank() &&
                room.hostId != uid &&
                room.hostId !in blockedUserIds &&
                (targetCountryCode == "ALL" || room.hostCountryCode.equals(targetCountryCode, ignoreCase = true)) &&
                (room.targetCountryCode == "ALL" || room.targetCountryCode.equals(userCountryCode, ignoreCase = true))
        }

        // Prefer a stranger we didn't just talk to if possible
        val prioritizedRooms = compatibleRooms.sortedBy { room ->
            if (previousPartnerId != null && room.hostId == previousPartnerId) 1 else 0
        }

        for (candidate in prioritizedRooms) {
            val joinResult = joinWaitingRoom(
                roomId = candidate.id,
                guestName = userName,
                guestCountryCode = userCountryCode,
                guestCountryName = userCountryName,
                rtcAnswerSdp = rtcOfferOrAnswerSdp
            )
            if (joinResult.isSuccess) {
                val updatedDoc = db.collection(path).document(candidate.id).get().await()
                val joinedRoom = updatedDoc.toObject(
                    CallRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (joinedRoom != null) {
                    return@runCatching joinedRoom
                }
            }
        }

        // No compatible waiting room found; create our own waiting room
        val newRoomId = createWaitingRoom(
            hostName = userName,
            hostCountryCode = userCountryCode,
            hostCountryName = userCountryName,
            targetCountryCode = targetCountryCode,
            rtcOfferSdp = rtcOfferOrAnswerSdp
        ).getOrThrow()

        val createdSnap = db.collection(path).document(newRoomId).get().await()
        checkNotNull(
            createdSnap.toObject(
                CallRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        ) { "Created room could not be deserialized" }
    }

    fun observeCallRoom(roomId: String): Flow<CallRoom?> {
        val docRef = db.collection("call_rooms").document(roomId)
        return docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    CallRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    fun observeWaitingRooms(): Flow<List<CallRoom>> {
        val path = "call_rooms"
        return db.collection(path)
            .whereEqualTo("status", "waiting")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun updateRoomMediaState(
        roomId: String,
        isHost: Boolean,
        frameData: String? = null,
        audioMuted: Boolean? = null,
        cameraFacing: String? = null,
        sdpPayload: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("call_rooms").document(roomId)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (frameData != null) {
            updates[if (isHost) "hostFrameData" else "guestFrameData"] = frameData.take(8000)
        }
        if (audioMuted != null) {
            updates[if (isHost) "hostAudioMuted" else "guestAudioMuted"] = audioMuted
        }
        if (cameraFacing != null) {
            updates[if (isHost) "hostCameraFacing" else "guestCameraFacing"] =
                if (cameraFacing == "back") "back" else "front"
        }
        if (sdpPayload != null) {
            updates[if (isHost) "rtcOfferSdp" else "rtcAnswerSdp"] = sdpPayload.take(5000)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun endCallRoom(roomId: String): Result<Unit> = runCatching {
        if (roomId.isBlank()) return@runCatching
        requireUserId()
        val docRef = db.collection("call_rooms").document(roomId)
        try {
            val snap = docRef.get().await()
            if (snap.exists() && snap.getString("status") != "ended") {
                docRef.update(
                    mapOf(
                        "status" to "ended",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun sendChatMessage(
        roomId: String,
        senderName: String,
        text: String,
        participants: List<String>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val trimmed = text.trim().take(500)
        require(trimmed.isNotEmpty()) { "Message text cannot be empty" }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("call_rooms")
            .document(roomId)
            .collection("messages")
            .document(msgId)

        val validParticipants = participants.filter { it.isNotBlank() }.distinct().take(2).let {
            if (uid in it) it else (listOf(uid) + it).take(2)
        }

        val payload = mapOf(
            "roomId" to roomId,
            "senderId" to uid,
            "senderName" to senderName.trim().ifBlank { "User" }.take(60),
            "text" to trimmed,
            "participants" to validParticipants,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeRoomMessages(roomId: String, userId: String): Flow<List<ChatMessage>> {
        val path = "call_rooms/$roomId/messages"
        return db.collection("call_rooms")
            .document(roomId)
            .collection("messages")
            .whereArrayContains("participants", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitModerationAction(
        targetUserId: String,
        targetCountryCode: String,
        actionType: String,
        reason: String,
        roomId: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(targetUserId.isNotBlank() && targetUserId != uid) {
            "Invalid target user for moderation"
        }
        val actionId = "mod_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("users")
            .document(uid)
            .collection("moderation_actions")
            .document(actionId)
        val payload = mapOf(
            "userId" to uid,
            "targetUserId" to targetUserId.take(128),
            "targetCountryCode" to targetCountryCode.ifBlank { "ALL" }.take(10),
            "actionType" to if (actionType == "block") "block" else "report",
            "reason" to reason.trim().ifBlank { "Community guideline violation" }.take(300),
            "roomId" to roomId.ifBlank { "unknown_room" }.take(128),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            if (roomId.isNotBlank()) {
                runCatching { endCallRoom(roomId) }
            }
            actionId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeModerationActions(userId: String): Flow<List<ModerationAction>> {
        val path = "users/$userId/moderation_actions"
        return db.collection("users")
            .document(userId)
            .collection("moderation_actions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ModerationAction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }
}
