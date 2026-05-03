package com.example.free2party.exception

import androidx.annotation.StringRes
import com.example.free2party.R

sealed class SocialException(
    message: String,
    @get:StringRes val messageRes: Int? = null
) : Exception(message)

class UserNotFoundException(
    message: String? = null
) : SocialException(message ?: "UserNotFoundException", R.string.error_user_not_found)

class FriendRequestAlreadySentException(
    message: String? = null
) : SocialException(
    message ?: "FriendRequestAlreadySentException",
    R.string.error_friend_request_already_sent
)

class FriendRequestPendingException(
    message: String? = null
) : SocialException(
    message ?: "FriendRequestPendingException",
    R.string.error_friend_request_pending
)

class FriendRequestBlockedException(
    message: String? = null
) : SocialException(
    message ?: "FriendRequestBlockedException",
    R.string.error_friend_request_blocked
)

class FriendRequestAlreadyAcceptedException(
    message: String? = null
) : SocialException(
    message ?: "FriendRequestAlreadyAcceptedException",
    R.string.error_friend_request_already_accepted
)

class CannotAddSelfException(
    message: String? = null
) : SocialException(message ?: "CannotAddSelfException", R.string.error_cannot_add_self)

class FriendRequestNotFoundException(
    message: String? = null
) : SocialException(message ?: "FriendRequestNotFoundException", R.string.error_friend_request_not_found)
