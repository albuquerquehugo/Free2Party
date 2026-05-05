package com.example.free2party.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FriendInfo(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val isFreeNow: Boolean = false,
    val inviteStatus: InviteStatus = InviteStatus.ACCEPTED,
    val phoneCode: String = "",
    val phoneNumber: String = "",
    val socials: UserSocials = UserSocials()
)

@IgnoreExtraProperties
data class UserSearchResult(
    val uid: String = "",
    val profilePicUrl: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val relationship: UserRelationship = UserRelationship.NONE
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

enum class InviteStatus {
    INVITED,
    ACCEPTED
}

enum class UserRelationship {
    NONE,
    FRIEND,
    INVITED,
    BLOCKED
}
