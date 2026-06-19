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

class PastEventDateTimeException(
    message: String? = null
) : EventException(message ?: "PastEventDateTimeException", R.string.error_past_event_date_time)

class GuestsMandatoryPrivateException(
    message: String? = null
) : EventException(message ?: "GuestsMandatoryPrivateException", R.string.error_guests_mandatory_private)

class LocationMandatoryException(
    message: String? = null
) : EventException(message ?: "LocationMandatoryException", R.string.error_location_mandatory)

class EventAlreadyStartedException(
    message: String? = null
) : EventException(message ?: "EventAlreadyStartedException", R.string.error_event_already_started)
