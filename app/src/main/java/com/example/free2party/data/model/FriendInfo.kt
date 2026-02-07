package com.example.free2party.data.model

data class FriendInfo(
    val uid: String = "",
    val name: String = "",
    val isFreeNow: Boolean = false,
    val inviteStatus: InviteStatus = InviteStatus.ACCEPTED
)

enum class InviteStatus {
    INVITED,
    ACCEPTED
}
