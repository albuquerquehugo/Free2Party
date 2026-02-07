package com.example.free2party.exception

sealed class InfrastructureException(message: String) : Exception(message)

class UnauthorizedException(message: String = "You must be logged in to perform this action") :
    InfrastructureException(message)

class NetworkUnavailableException(message: String = "No internet connection available") :
    InfrastructureException(message)

class DatabaseOperationException(message: String = "An error occurred while saving data. Please try again.") :
    InfrastructureException(message)
