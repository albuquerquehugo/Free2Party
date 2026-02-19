package com.example.free2party.util

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
 * @return A [Pair] where the first element is the hour and the second is the minute or 0:0 if the format is invalid.
 */
fun unformatTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    if (parts.size != 2) return 0 to 0
    val hour = parts[0].toIntOrNull() ?: return 0 to 0
    val minute = parts[1].toIntOrNull() ?: return 0 to 0
    return hour to minute
}

/**
 * Parses a time string in the format "H:mm" or "HH:mm" into the total number of minutes from the
 * start of the day.
 * @param time The time string to parse.
 * @return The total number of minutes, or null if the string format is invalid.
 */
fun parseTimeToMinutes(time: String): Int? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return hour * 60 + minute
}

/**
 * Parses a time string in the format "H:mm" or "HH:mm" into the total number of milliseconds
 * from the start of the day.
 * @param time The time string to parse.
 * @return The total number of milliseconds, or null if the string format is invalid.
 */
fun parseTimeToMillis(time: String): Long? {
    return parseTimeToMinutes(time)?.let { it * 60000L }
}


/**
 * Formats a date "yyyy-MM-dd" to "MMM dd, yyyy".
 * @param dateStr The date string to be formatted.
 * @return A formatted date string in the format "MMM dd, yyyy". If the format is invalid, it
 * returns the original string
 */
fun formatPlanDate(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size < 3) return dateStr

    val month = when (parts[1]) {
        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
        else -> return dateStr // Return original string if month is invalid
    }
    val day = parts[2].toIntOrNull() ?: return dateStr
    return "$month $day, ${parts[0]}"
}

/**
 * Parses a date string in "yyyy-MM-dd" format into milliseconds representing UTC midnight of that date.
 * @param dateString The date string to parse.
 * @return The time in milliseconds since the epoch, or null if parsing fails.
 */
fun parseDateToMillis(dateString: String): Long? {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    return runCatching { sdf.parse(dateString)?.time }.getOrNull()
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
        val targetMins = parseTimeToMinutes(timeString) ?: 0
        // If the current minute is the same as or greater than the target minute, it is in the past/started
        return targetMins <= currentMins
    }

    return false
}
