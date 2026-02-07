package com.example.free2party.exception

class UserNotFoundException(message: String = "User not found") : Exception(message)

class FriendRequestAlreadySentException(message: String = "An invite has already been sent to this user") :
    Exception(message)

class FriendRequestAlreadyAcceptedException(message: String = "This user is already your friend") :
    Exception(message)

class CannotAddSelfException(message: String = "You cannot add yourself as a friend") :
    Exception(message)
