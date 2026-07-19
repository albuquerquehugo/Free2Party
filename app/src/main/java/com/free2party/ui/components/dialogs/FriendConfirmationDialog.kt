package com.free2party.ui.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.free2party.R
import com.free2party.ui.components.ProfileAvatar
import com.free2party.ui.components.ProfileAvatarSize

enum class FriendActionType {
    ADD,
    REMOVE,
    BLOCK
}

@Composable
fun FriendConfirmationDialog(
    name: String,
    email: String = "",
    profilePicUrl: String,
    actionType: FriendActionType,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    onSecondaryAction: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    val title = when (actionType) {
        FriendActionType.ADD -> stringResource(R.string.label_add_friend_name, name)
        FriendActionType.REMOVE -> stringResource(R.string.label_remove_friend_name, name)
        FriendActionType.BLOCK -> stringResource(R.string.label_block_user_name, name)
    }

    val text = when (actionType) {
        FriendActionType.ADD -> stringResource(
            R.string.text_send_friend_request_confirmation,
            name,
            email
        )

        FriendActionType.REMOVE -> stringResource(R.string.text_remove_friend_confirmation)
        FriendActionType.BLOCK -> stringResource(
            R.string.text_block_user_confirmation,
            stringResource(R.string.label_blocked_users)
        ) + "\n\n" + stringResource(R.string.text_block_info_message_1) + "\n" +
                stringResource(R.string.text_block_info_message_2)
    }

    AppConfirmationDialog(
        topContent = {
            ProfileAvatar(
                profilePicUrl = profilePicUrl,
                size = ProfileAvatarSize.LARGE
            )
        },
        title = title,
        text = text,
        content = content,
        confirmButtonText = when (actionType) {
            FriendActionType.ADD -> stringResource(R.string.label_confirm)
            FriendActionType.REMOVE -> stringResource(R.string.label_confirm)
            FriendActionType.BLOCK -> stringResource(R.string.label_block_and_report)
        },
        onConfirm = onConfirm,
        dismissButtonText = stringResource(R.string.label_cancel),
        onDismissRequest = onDismissRequest,
        secondaryButtonText =
            if (actionType == FriendActionType.BLOCK) stringResource(R.string.label_block)
            else null,
        onSecondaryAction = onSecondaryAction,
        isDestructive = actionType != FriendActionType.ADD,
        contentHorizontalAlignment = Alignment.CenterHorizontally
    )
}

