package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JimmyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndGetUserProfile_authenticatedOwner_succeedsAndBlocksCrossUser() = runBlocking {
        val aliceUid = signInTestUser("alice@jimmy.test")
        val repository = JimmyRepository(firestore, auth)

        val upsertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                displayName = "Alice",
                countryCode = "MM",
                countryName = "Myanmar",
                targetCountryCode = "ALL"
            )
        }
        assertTrue(upsertResult.isSuccess)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(aliceUid)
        }
        assertTrue(profileResult.isSuccess)
        assertEquals("MM", profileResult.getOrNull()?.countryCode)

        // Bob cannot read Alice's UserProfile
        signInTestUser("bob@jimmy.test")
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(aliceUid)
        }
        assertTrue(crossReadResult.isFailure)
        val firestoreEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun matchmakingAndChat_twoUsersMatchAndExchangeRealtimeMessages() = runBlocking {
        val aliceUid = signInTestUser("alice_match@jimmy.test")
        val repository = JimmyRepository(firestore, auth)

        // Alice creates a waiting room targeting Thailand ("TH")
        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWaitingRoom(
                hostName = "Alice",
                hostCountryCode = "MM",
                hostCountryName = "Myanmar",
                targetCountryCode = "TH",
                rtcOfferSdp = "v=0 offer"
            ).getOrThrow()
        }
        assertTrue(roomId.isNotBlank())

        // Bob from Thailand ("TH") joins Alice's waiting room
        val bobUid = signInTestUser("bob_match@jimmy.test")
        val joinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinWaitingRoom(
                roomId = roomId,
                guestName = "Bob",
                guestCountryCode = "TH",
                guestCountryName = "Thailand",
                rtcAnswerSdp = "v=0 answer"
            )
        }
        assertTrue(joinResult.isSuccess)

        // Bob sends an in-call chat message
        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendChatMessage(
                roomId = roomId,
                senderName = "Bob",
                text = "Sawasdee from Thailand!",
                participants = listOf(aliceUid, bobUid)
            ).getOrThrow()
        }
        assertTrue(msgId.isNotBlank())

        // Alice observes messages in real time
        signInTestUser("alice_match@jimmy.test")
        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRoomMessages(roomId, aliceUid).first { it.isNotEmpty() }
        }
        assertEquals("Sawasdee from Thailand!", messages.first().text)

        // Alice reports/blocks Bob -> creates moderation_action and ends the room
        val modId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitModerationAction(
                targetUserId = bobUid,
                targetCountryCode = "TH",
                actionType = "block",
                reason = "Test block",
                roomId = roomId
            ).getOrThrow()
        }
        assertNotNull(modId)
    }

    @Test
    fun observeWaitingRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = JimmyRepository(firestore, auth)

        val thrown = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWaitingRooms().first()
            }
        }.exceptionOrNull()

        assertNotNull(thrown)
        val firestoreEx = thrown?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
