package com.example.free2party.ui.components.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.free2party.data.model.FriendInfo
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class PlanDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockFriends = listOf(
        FriendInfo(uid = "1", name = "Alice"),
        FriendInfo(uid = "2", name = "Bob")
    )

    @Test
    fun addButton_disabled_whenVisibilityIsExceptAndNoFriendsSelected() {
        composeTestRule.setContent {
            PlanDialog(
                editingPlan = null,
                use24HourFormat = true,
                friends = mockFriends,
                onDismiss = {},
                onConfirm = { _, _, _, _, _, _, _ -> },
                startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                startTimeState = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = true),
                endTimeState = rememberTimePickerState(initialHour = 13, initialMinute = 0, is24Hour = true)
            )
        }

        // Switch to "Everyone except..."
        composeTestRule.onNodeWithText("Everyone except...").performClick()

        // Button should be disabled
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()

        // Select a friend
        composeTestRule.onNodeWithText("Alice").performClick()

        // Button should be enabled
        composeTestRule.onNodeWithText("Add").assertIsEnabled()
    }

    @Test
    fun selectAll_selectsAllFriends() {
        composeTestRule.setContent {
            PlanDialog(
                editingPlan = null,
                use24HourFormat = true,
                friends = mockFriends,
                onDismiss = {},
                onConfirm = { _, _, _, _, _, _, _ -> },
                startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                startTimeState = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = true),
                endTimeState = rememberTimePickerState(initialHour = 13, initialMinute = 0, is24Hour = true)
            )
        }

        composeTestRule.onNodeWithText("Only selected people...").performClick()
        composeTestRule.onNodeWithText("Select all").performClick()

        composeTestRule.onNodeWithText("Add").assertIsEnabled()
    }

    @Test
    fun unselectAll_clearsSelection() {
        composeTestRule.setContent {
            PlanDialog(
                editingPlan = null,
                use24HourFormat = true,
                friends = mockFriends,
                onDismiss = {},
                onConfirm = { _, _, _, _, _, _, _ -> },
                startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000),
                startTimeState = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = true),
                endTimeState = rememberTimePickerState(initialHour = 13, initialMinute = 0, is24Hour = true)
            )
        }

        composeTestRule.onNodeWithText("Only selected people...").performClick()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("Add").assertIsEnabled()

        composeTestRule.onNodeWithText("Unselect all").performClick()
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()
    }
}
