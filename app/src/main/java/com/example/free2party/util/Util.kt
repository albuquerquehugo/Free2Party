package com.example.free2party.util

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.free2party.ui.screens.calendar.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Formats the given hour and minute into a string with the format "H:mm" or "HH:mm".
 * @param hour The hour of the day.
 * @param minute The minute of the hour.
 * @return A string representation of the time, ensuring the minute is always two digits.
 */
fun formatTime(hour: Int, minute: Int): String {
    val mm = minute.toString().padStart(2, '0')
    return "$hour:$mm"
}

/**
 * Parses a time string in the format "H:mm" or "HH:mm" back into hour and minute integers.
 * @param time The formatted time string to parse.
 * @return A [Pair] where the first element is the hour and the second is the minute.
 */
fun unformatTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    return parts[0].toInt() to parts[1].toInt()
}

/**
 * Parses a time string in the format "H:mm" or "HH:mm" into the total number of minutes from the
 * start of the day.
 * @param time The time string to parse.
 * @return The total number of minutes, or 0 if the string format is invalid.
 */
fun parseTimeToMinutes(time: String): Int {
    val parts = time.split(":")
    return try {
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) {
        Log.e("timeToMinutes", "Error converting time to minutes: ${e.message}")
        0
    }
}

/**
 * Parses a time string in the format "H:mm" or "HH:mm" into the total number of milliseconds
 * from the start of the day.
 * @param time The time string to parse.
 * @return The total number of milliseconds, or 0 if the string format is invalid.
 */
fun parseTimeToMillis(time: String): Long {
    return parseTimeToMinutes(time) * 60000L
}


/**
 * Formats a date represented in milliseconds into a human-readable string (e.g., "MMM dd, yyyy").
 * @param dateStr The date in UTC milliseconds to be formatted.
 * @return A formatted date string according to the default locale.
 */
fun formatPlanDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size < 3) return dateStr
        val month = when (parts[1]) {
            "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
            "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
            "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
            else -> parts[1]
        }
        "$month ${parts[2].toInt()}, ${parts[0]}"
    } catch (e: Exception) {
        Log.e("formatPlanDate", "Error formatting date: ${e.message}")
        dateStr
    }
}

/**
 * Parses a date string in "yyyy-MM-dd" format into milliseconds representing UTC midnight of that date.
 * @param dateString The date string to parse.
 * @return The time in milliseconds since the epoch, or null if parsing fails.
 */
fun parseDateToMillis(dateString: String): Long? {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return try {
        sdf.parse(dateString)?.time
    } catch (e: Exception) {
        Log.e("dateToMillis", "Error converting date to millis: ${e.message}")
        null
    }
}

/**
 * Logic for determining if a date/time has already passed.
 * @param dateUtcMillis The date in UTC milliseconds (start of day).
 * @param timeString Optional time string (HH:mm). If provided, checks time if date is today.
 * @param now The reference time to check against.
 */
fun isDateTimeInPast(
    dateUtcMillis: Long,
    timeString: String? = null,
    now: Calendar = Calendar.getInstance()
): Boolean {
    val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        set(Calendar.MILLISECOND, 0)
    }

    if (dateUtcMillis < todayUtc.timeInMillis) return true

    if (dateUtcMillis == todayUtc.timeInMillis && timeString != null) {
        val currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val targetMins = parseTimeToMinutes(timeString)
        // If the current minute is the same as or greater than the target minute, it is in the past/started
        return targetMins <= currentMins
    }

    return false
}

/**
 * Creates a [ViewModelProvider.Factory] to instantiate [CalendarViewModel] with a specific user ID.
 * @param targetUserId The UID of the user whose calendar data should be loaded, or null to load the
 * current authenticated user's calendar.
 * @return A factory capable of creating instances of [CalendarViewModel].
 * @throws IllegalArgumentException if the requested ViewModel class is not [CalendarViewModel].
 */
fun provideCalendarViewModelFactory(targetUserId: String?): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                return CalendarViewModel(targetUserId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
