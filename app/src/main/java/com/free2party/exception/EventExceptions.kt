package com.free2party.exception

import androidx.annotation.StringRes
import com.free2party.R

sealed class EventException(
    message: String,
    @get:StringRes val messageRes: Int? = null
) : Exception(message)

class EventNotFoundException(
    message: String? = null
) : EventException(message ?: "EventNotFoundException", R.string.error_event_not_found)

class InvalidEventDataException(
    message: String? = null
) : EventException(message ?: "InvalidEventDataException", R.string.error_invalid_event_data)
