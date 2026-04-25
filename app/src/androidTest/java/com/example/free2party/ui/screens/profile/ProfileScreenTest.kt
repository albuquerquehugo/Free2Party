package com.example.free2party.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.free2party.TestActivity
import com.example.free2party.data.model.User
import com.example.free2party.ui.theme.Free2PartyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    /**
     * A wrapper to provide state management for testing ProfileScreenContent,
     * which is now a stateless component.
     */
    @Composable
    private fun ProfileScreenTestWrapper(
        initialUser: User,
        onUpdateProfileClicked: (User) -> Unit = {}
    ) {
        val firstName = remember { mutableStateOf(initialUser.firstName) }
        val lastName = remember { mutableStateOf(initialUser.lastName) }
        val bio = remember { mutableStateOf(initialUser.bio) }
        val isWhatsappSameAsPhone = remember { mutableStateOf(false) }

        val uiState = ProfileUiState.Success(user = initialUser)

        val hasChanges = firstName.value != initialUser.firstName ||
                lastName.value != initialUser.lastName ||
                bio.value != initialUser.bio ||
                isWhatsappSameAsPhone.value

        val isFormValid = firstName.value.isNotBlank() && lastName.value.isNotBlank()

        Free2PartyTheme {
            Surface {
                ProfileScreenContent(
                    paddingValues = PaddingValues(0.dp),
                    uiState = uiState,
                    onUploadImage = {},
                    firstName = firstName.value,
                    onFirstNameChange = { firstName.value = it },
                    lastName = lastName.value,
                    onLastNameChange = { lastName.value = it },
                    countryCode = initialUser.countryCode,
                    onCountryCodeChange = {},
                    phoneNumber = initialUser.phoneNumber,
                    onPhoneNumberChange = {},
                    birthday = initialUser.birthday,
                    onBirthdayChange = {},
                    bio = bio.value,
                    onBioChange = { bio.value = it },
                    whatsappCountryCode = initialUser.socials.whatsappCountryCode,
                    onWhatsappCountryCodeChange = {},
                    whatsappNumber = initialUser.socials.whatsappNumber,
                    onWhatsappNumberChange = {},
                    isWhatsappSameAsPhone = isWhatsappSameAsPhone.value,
                    onWhatsappSameAsPhoneChange = { isWhatsappSameAsPhone.value = it },
                    telegramUsername = initialUser.socials.telegramUsername,
                    onTelegramUsernameChange = {},
                    facebookUsername = initialUser.socials.facebookUsername,
                    onFacebookUsernameChange = {},
                    instagramUsername = initialUser.socials.instagramUsername,
                    onInstagramUsernameChange = {},
                    tiktokUsername = initialUser.socials.tiktokUsername,
                    onTiktokUsernameChange = {},
                    xUsername = initialUser.socials.xUsername,
                    onXUsernameChange = {},
                    hasChanges = hasChanges,
                    isFormValid = isFormValid,
                    onDiscardChanges = {
                        firstName.value = initialUser.firstName
                        lastName.value = initialUser.lastName
                        bio.value = initialUser.bio
                        isWhatsappSameAsPhone.value = false
                    },
                    onUpdateProfile = {
                        onUpdateProfileClicked(
                            initialUser.copy(
                                firstName = firstName.value,
                                lastName = lastName.value,
                                bio = bio.value
                            )
                        )
                    },
                    onDeleteAccount = {}
                )
            }
        }
    }

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
            ProfileScreenTestWrapper(initialUser = initialUser)
        }

        // Initially, the discard button should not exist
        composeTestRule.onNodeWithTag("discard_button").assertDoesNotExist()

        // Change the first name
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("Jane")

        // Now the discard button should appear
        composeTestRule.onNodeWithTag("discard_button").assertExists()

        // Change the bio as well
        composeTestRule.onNodeWithTag("bio_field").performTextReplacement("New bio")

        // Click discard (scroll to it first as it might be at the bottom)
        composeTestRule.onNodeWithTag("discard_button").performScrollTo().performClick()

        // Verify values are reset to initial
        composeTestRule.onNodeWithTag("first_name_field").assert(hasText("John"))
        composeTestRule.onNodeWithTag("last_name_field").assert(hasText("Doe"))
        composeTestRule.onNodeWithTag("bio_field").assert(hasText("Initial bio"))

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
            ProfileScreenTestWrapper(initialUser = initialUser)
        }

        // Initially, save button should be disabled (no changes)
        composeTestRule.onNodeWithTag("save_button").performScrollTo().assertIsNotEnabled()

        // Change a field
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("Jane")
        composeTestRule.onNodeWithTag("save_button").performScrollTo().assertIsEnabled()

        // Make first name empty
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("")
        composeTestRule.onNodeWithTag("save_button").performScrollTo().assertIsNotEnabled()

        // Restore first name but make last name empty
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("John")
        composeTestRule.onNodeWithTag("last_name_field").performTextReplacement("")
        composeTestRule.onNodeWithTag("save_button").performScrollTo().assertIsNotEnabled()
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
            ProfileScreenTestWrapper(
                initialUser = initialUser,
                onUpdateProfileClicked = { updatedUser = it }
            )
        }

        // Modify fields
        composeTestRule.onNodeWithTag("first_name_field").performTextReplacement("Jane")
        composeTestRule.onNodeWithTag("bio_field").performTextReplacement("New bio")

        // Click save
        composeTestRule.onNodeWithTag("save_button").performScrollTo().performClick()

        // Verify the callback was triggered with correct data
        assertEquals("Jane", updatedUser?.firstName)
        assertEquals("Doe", updatedUser?.lastName)
        assertEquals("New bio", updatedUser?.bio)
    }
}
