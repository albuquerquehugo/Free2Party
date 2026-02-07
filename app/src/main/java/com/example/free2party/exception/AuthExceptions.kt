package com.example.free2party.exception

sealed class AuthException(message: String) : Exception(message)

class EmailAlreadyInUseException(message: String = "This email is already in use") :
    AuthException(message)

class WeakPasswordException(message: String = "The password is too weak") : AuthException(message)

class InvalidCredentialsException(message: String = "Invalid email or password") :
    AuthException(message)

class UserDisabledException(message: String = "This user account has been disabled") :
    AuthException(message)

class AuthNetworkException(message: String = "A network error occurred. Please try again.") :
    AuthException(message)

class UnknownAuthException(message: String = "An unexpected authentication error occurred") :
    AuthException(message)

class UserNullException(message: String = "Authentication failed: User is null") :
    AuthException(message)
