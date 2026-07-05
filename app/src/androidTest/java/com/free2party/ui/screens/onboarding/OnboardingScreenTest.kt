package com.free2party.ui.screens.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.free2party.R
import com.free2party.TestActivity
import com.free2party.ui.theme.Free2PartyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun onboardingScreen_initialState_displaysWelcomeText() {
        val context = composeTestRule.activity
        val welcomeTitle = context.getString(R.string.onboarding_title_welcome)
        val nextButtonLabel = context.getString(R.string.label_next)
        val skipButtonLabel = context.getString(R.string.label_skip)

        var completed = false
        composeTestRule.setContent {
            Free2PartyTheme {
                OnboardingScreen(onComplete = { completed = true })
            }
        }

        // Verify welcome title is displayed
        composeTestRule.onNodeWithText(welcomeTitle).assertIsDisplayed()

        // Verify "Next" and "Skip" buttons exist and are displayed
        composeTestRule.onNodeWithText(nextButtonLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(skipButtonLabel).assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_clickSkip_triggersCompletion() {
        val context = composeTestRule.activity
        val skipButtonLabel = context.getString(R.string.label_skip)

        var completed = false
        composeTestRule.setContent {
            Free2PartyTheme {
                OnboardingScreen(onComplete = { completed = true })
            }
        }

        // Click Skip
        composeTestRule.onNodeWithText(skipButtonLabel).performClick()

        // Assert callback was triggered
        assertTrue(completed)
    }

    @Test
    fun onboardingScreen_swipeThroughToLastPage_displaysGetStartedButton() {
        val context = composeTestRule.activity
        val nextButtonLabel = context.getString(R.string.label_next)
        val getStartedButtonLabel = context.getString(R.string.label_get_started)

        var completed = false
        composeTestRule.setContent {
            Free2PartyTheme {
                OnboardingScreen(onComplete = { completed = true })
            }
        }

        // Swipe or click Next 6 times to reach page 7 (index 6)
        repeat(6) {
            composeTestRule.onNodeWithText(nextButtonLabel).performClick()
            composeTestRule.waitForIdle()
        }

        // Verify "Get Started" is displayed
        composeTestRule.onNodeWithText(getStartedButtonLabel).assertIsDisplayed()

        // Click Get Started
        composeTestRule.onNodeWithText(getStartedButtonLabel).performClick()

        // Assert callback triggered
        assertTrue(completed)
    }
}
