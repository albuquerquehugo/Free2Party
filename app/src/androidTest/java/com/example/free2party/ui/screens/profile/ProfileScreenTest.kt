package com.example.free2party.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.example.free2party.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun discardButton_appearsWhenTextIsChanged_andResetsValuesOnClick() {
        val initialUser = User(
            uid = "123",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            bio = "Initial bio"
        )

        composeTestRule.setContent {
            ProfileScreenContent(
                paddingValues = PaddingValues(0.dp),
                user = initialUser,
                isSaving = false,
                isUploadingImage = false,
                onUpdateProfile = {},
                onUploadImage = {}
            )
        }

        // Initially, the discard button should not exist
        composeTestRule.onNodeWithTag("discard_button").assertDoesNotExist()

        // Change the first name
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("Jane")

        // Now the discard button should appear
        composeTestRule.onNodeWithTag("discard_button").assertExists()

        // Change the bio as well
        composeTestRule.onNodeWithTag("bio_field").performTextReplacement("New bio")

        // Click discard
        composeTestRule.onNodeWithTag("discard_button").performClick()

        // Verify values are reset to initial
        composeTestRule.onNodeWithTag("first_name_field").assertTextEquals("John")
        composeTestRule.onNodeWithTag("last_name_field").assertTextEquals("Doe")
        composeTestRule.onNodeWithTag("bio_field").assertTextEquals("Initial bio")

        // Verify the discard button is gone again
        composeTestRule.onNodeWithTag("discard_button").assertDoesNotExist()
    }

    @Test
    fun saveButton_enabledOnlyWhenChangesExistAndFieldsAreNotEmpty() {
        val initialUser = User(
            uid = "123",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            bio = "Initial bio"
        )

        composeTestRule.setContent {
            ProfileScreenContent(
                paddingValues = PaddingValues(0.dp),
                user = initialUser,
                isSaving = false,
                isUploadingImage = false,
                onUpdateProfile = {},
                onUploadImage = {}
            )
        }

        // Initially, save button should be disabled (no changes)
        composeTestRule.onNodeWithTag("save_button").assertIsNotEnabled()

        // Change a field
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("Jane")
        composeTestRule.onNodeWithTag("save_button").assertIsEnabled()

        // Make first name empty
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("")
        composeTestRule.onNodeWithTag("save_button").assertIsNotEnabled()

        // Restore first name but make last name empty
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("John")
        composeTestRule.onNodeWithTag("last_name_field").performTextReplacement("")
        composeTestRule.onNodeWithTag("save_button").assertIsNotEnabled()
    }

    @Test
    fun saveButton_triggersUpdateProfileWithCorrectData() {
        val initialUser = User(
            uid = "123",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            bio = "Initial bio"
        )
        var updatedUser: User? = null

        composeTestRule.setContent {
            ProfileScreenContent(
                paddingValues = PaddingValues(0.dp),
                user = initialUser,
                isSaving = false,
                isUploadingImage = false,
                onUpdateProfile = { updatedUser = it },
                onUploadImage = {}
            )
        }

        // Modify fields
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("Jane")
        composeTestRule.onNodeWithTag("bio_field").performTextReplacement("New bio")

        // Click save
        composeTestRule.onNodeWithTag("save_button").performClick()

        // Verify the callback was triggered with correct data
        assertEquals("Jane", updatedUser?.firstName)
        assertEquals("Doe", updatedUser?.lastName)
        assertEquals("New bio", updatedUser?.bio)
    }
}
