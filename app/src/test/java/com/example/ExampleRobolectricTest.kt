package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `launch MainActivity transition Splash to Welcome without crash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JIMMY", appName)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_screen").assertExists()

        // Advance past the 2100ms splash delay to transition to WelcomeScreen
        composeTestRule.mainClock.advanceTimeBy(2500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_screen").assertExists()
        composeTestRule.onNodeWithTag("your_country_selector").assertExists()
        composeTestRule.onNodeWithTag("connect_with_selector").assertExists()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
    }
}
