package com.example.free2party.exception

import androidx.annotation.StringRes
import com.example.free2party.R

sealed class AuthException(
    message: String,
    @get:StringRes val messageRes: Int? = null
) : Exception(message)

class AuthNetworkException(
    message: String? = null
) : AuthException(message ?: "AuthNetworkException", R.string.error_auth_network)

class EmailAlreadyInUseException(
    message: String? = null
) : AuthException(message ?: "EmailAlreadyInUseException", R.string.error_email_already_in_use)

class EmailNotVerifiedException(
    message: String? = null
) : AuthException(message ?: "EmailNotVerifiedException", R.string.error_email_not_verified)

class InvalidCredentialsException(
    message: String? = null
) : AuthException(message ?: "InvalidCredentialsException", R.string.error_invalid_credentials)

class UnknownAuthException(
    message: String? = null
) : AuthException(message ?: "UnknownAuthException", R.string.error_unknown_auth)

class UserDisabledException(
    message: String? = null
) : AuthException(message ?: "UserDisabledException", R.string.error_user_disabled)

class UserNullException(
    message: String? = null
) : AuthException(message ?: "UserNullException", R.string.error_auth_failed_user_null)

class WeakPasswordException(
    message: String? = null
) : AuthException(message ?: "WeakPasswordException", R.string.error_weak_password)
