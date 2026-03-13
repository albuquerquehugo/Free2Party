package com.example.free2party.exception

import androidx.annotation.StringRes
import com.example.free2party.R

sealed class InfrastructureException(
    message: String,
    @get:StringRes val messageRes: Int? = null
) : Exception(message)

class UnauthorizedException(
    message: String? = null
) : InfrastructureException(message ?: "UnauthorizedException", R.string.error_unauthorized)

class NetworkUnavailableException(
    message: String? = null
) : InfrastructureException(
    message ?: "NetworkUnavailableException",
    R.string.error_network_unavailable
)

class DatabaseOperationException(
    message: String? = null
) : InfrastructureException(
    message ?: "DatabaseOperationException",
    R.string.error_database_operation
)
