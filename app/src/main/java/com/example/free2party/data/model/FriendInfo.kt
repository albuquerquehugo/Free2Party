package com.example.free2party.data.model

data class FriendInfo(
    val uid: String = "",
    val name: String = "",
    val isFreeNow: Boolean = false,
    val inviteStatus: InviteStatus = InviteStatus.ACCEPTED,
    val phoneCode: String = "",
    val phoneNumber: String = "",
    val socials: UserSocials = UserSocials()
)

enum class InviteStatus {
    INVITED,
    ACCEPTED
}
