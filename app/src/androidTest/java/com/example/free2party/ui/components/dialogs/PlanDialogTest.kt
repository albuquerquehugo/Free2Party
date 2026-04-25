package com.example.free2party.ui.components.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.free2party.TestActivity
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.ui.theme.Free2PartyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class PlanDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private val mockFriends = listOf(
        FriendInfo(uid = "1", name = "Alice"),
        FriendInfo(uid = "2", name = "Bob")
    )

    @Test
    fun addButton_disabled_whenVisibilityIsExceptAndNoFriendsSelected() {
        composeTestRule.setContent {
            Free2PartyTheme {
                Surface {
                    PlanDialog(
                        editingPlan = null,
                        use24HourFormat = true,
                        friends = mockFriends,
                        onDismiss = {},
                        onConfirm = { _, _, _, _, _, _, _ -> },
                        startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                        endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                        startTimeState = rememberTimePickerState(
                            initialHour = 12,
                            initialMinute = 0,
                            is24Hour = true
                        ),
                        endTimeState = rememberTimePickerState(
                            initialHour = 13,
                            initialMinute = 0,
                            is24Hour = true
                        )
                    )
                }
            }
        }

        // Switch to "Everyone except..."
        composeTestRule.onNodeWithTag("visibility_except").performClick()

        // Button should be disabled
        composeTestRule.onNodeWithTag("plan_dialog_confirm_button").assertIsNotEnabled()

        // Select a friend
        composeTestRule.onNodeWithText("Alice").performClick()

        // Button should be enabled
        composeTestRule.onNodeWithTag("plan_dialog_confirm_button").assertIsEnabled()
    }

    @Test
    fun selectAll_selectsAllFriends() {
        composeTestRule.setContent {
            Free2PartyTheme {
                Surface {
                    PlanDialog(
                        editingPlan = null,
                        use24HourFormat = true,
                        friends = mockFriends,
                        onDismiss = {},
                        onConfirm = { _, _, _, _, _, _, _ -> },
                        startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                        endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                        startTimeState = rememberTimePickerState(
                            initialHour = 12,
                            initialMinute = 0,
                            is24Hour = true
                        ),
                        endTimeState = rememberTimePickerState(
                            initialHour = 13,
                            initialMinute = 0,
                            is24Hour = true
                        )
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("visibility_only").performClick()
        composeTestRule.onNodeWithTag("select_all").performClick()

        composeTestRule.onNodeWithTag("plan_dialog_confirm_button").assertIsEnabled()
    }

    @Test
    fun unselectAll_clearsSelection() {
        composeTestRule.setContent {
            Free2PartyTheme {
                Surface {
                    PlanDialog(
                        editingPlan = null,
                        use24HourFormat = true,
                        friends = mockFriends,
                        onDismiss = {},
                        onConfirm = { _, _, _, _, _, _, _ -> },
                        startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                        endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                        startTimeState = rememberTimePickerState(
                            initialHour = 12,
                            initialMinute = 0,
                            is24Hour = true
                        ),
                        endTimeState = rememberTimePickerState(
                            initialHour = 13,
                            initialMinute = 0,
                            is24Hour = true
                        )
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("visibility_only").performClick()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithTag("plan_dialog_confirm_button").assertIsEnabled()

        composeTestRule.onNodeWithTag("unselect_all").performClick()
        composeTestRule.onNodeWithTag("plan_dialog_confirm_button").assertIsNotEnabled()
    }
}
