package com.example.free2party.exception

sealed class SocialException(message: String) : Exception(message)

class UserNotFoundException(message: String = "User not found") : SocialException(message)

class FriendRequestAlreadySentException(message: String = "An invite has already been sent to this user") :
    SocialException(message)

class FriendRequestAlreadyAcceptedException(message: String = "This user is already your friend") :
    SocialException(message)

class CannotAddSelfException(message: String = "You cannot add yourself as a friend") :
    SocialException(message)
