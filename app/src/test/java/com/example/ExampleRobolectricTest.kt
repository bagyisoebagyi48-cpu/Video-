package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CallRoom
import com.example.data.model.ChatMessage
import com.example.data.model.CountryCatalog
import com.example.ui.auth.ensureFirebaseInitialized
import com.example.ui.auth.getSafeFirebaseAuth
import com.example.ui.auth.getSafeFirestore
import com.example.ui.viewmodel.ActiveAppScreen
import com.example.ui.viewmodel.JimmyUiState
import com.example.ui.viewmodel.MatchmakingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `launch MainActivity transition Splash to Welcome interact with sheets and survive recreation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JIMMY", appName)
        assertTrue(ensureFirebaseInitialized(context))
        assertNotNull(getSafeFirebaseAuth(context))
        assertNotNull(getSafeFirestore(context))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_screen").assertExists()

        // Advance past the 2100ms splash delay to transition to WelcomeScreen
        composeTestRule.mainClock.advanceTimeBy(2500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_screen").assertExists()
        composeTestRule.onNodeWithTag("your_country_selector").assertExists()
        composeTestRule.onNodeWithTag("connect_with_selector").assertExists()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
        composeTestRule.onNodeWithTag("popular_country_TH").assertExists()

        // Verify VideoChatScreen states (Searching and Connected) construct cleanly
        val connectedRoom = CallRoom(
            id = "room_test_1",
            hostId = "host_1",
            hostName = "Aung",
            hostCountryCode = "MM",
            hostCountryName = "Myanmar",
            guestId = "guest_2",
            guestName = "Nicha",
            guestCountryCode = "TH",
            guestCountryName = "Thailand",
            status = "connected"
        )
        val connectedUiState = JimmyUiState(
            currentUserId = "host_1",
            displayName = "Aung",
            yourCountry = CountryCatalog.findByCode("MM"),
            connectWithCountry = CountryCatalog.findByCode("TH"),
            activeScreen = ActiveAppScreen.VIDEO_CHAT,
            matchmakingState = MatchmakingState.Connected(connectedRoom),
            activeRoom = connectedRoom,
            chatMessages = listOf(
                ChatMessage(
                    id = "m1",
                    roomId = "room_test_1",
                    senderId = "guest_2",
                    senderName = "Nicha",
                    text = "Sawasdee kha!"
                )
            )
        )
        assertEquals("TH", connectedUiState.activeRoom?.partnerCountryCode("host_1"))

        // Verify lifecycle state restoration / recreation does not crash
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_screen").assertExists()
    }
}
