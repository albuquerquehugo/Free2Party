package com.example.free2party.util

import android.content.Context
import android.content.Intent
import com.example.free2party.data.model.FuturePlan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import androidx.core.net.toUri
import java.util.Date

/**
 * Formats the given hour and minute into a string with the format "H:mm" or "HH:mm".
 * This is the internal storage format.
 * @param hour The hour of the day.
 * @param minute The minute of the hour.
 * @return A string representation of the time, ensuring the minute is always two digits.
 */
fun formatTime(hour: Int, minute: Int): String {
    val mm = minute.toString().padStart(2, '0')
    return "$hour:$mm"
}

/**
 * Formats the time elapsed since the given [Date] into a user-friendly string.
 * @param timestamp The date to calculate the elapsed time from.
 * @return A string representing the time ago (e.g., "just now", "5m ago", "2h ago", "3d ago").
 */
fun formatTimeAgo(timestamp: Date?): String {
    if (timestamp == null) return ""
    val diff = System.currentTimeMillis() - timestamp.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

/**
 * Formats the given internal time string into a user-friendly display string.
 * @param time Internal time string in the format "H:mm" or "HH:mm".
 * @param use24Hour Whether to use 24-hour or am/pm format.
 */
fun formatTimeForDisplay(time: String, use24Hour: Boolean): String {
    val (hour, minute) = unformatTime(time)
    return if (use24Hour) {
        val hh = hour.toString().padStart(2, '0')
        val mm = minute.toString().padStart(2, '0')
        "$hh:$mm"
    } else {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val mm = minute.toString().padStart(2, '0')
        "$displayHour:$mm $amPm"
    }
}

/**
 * Parses a time string back into hour and minute integers.
 * @param time The formatted time string in the format "H:mm" or "HH:mm" to parse.
 * @return A [Pair] where the first element is the hour and the second is the minute or
 * 0:0 if the format is invalid.
 */
fun unformatTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    if (parts.size != 2) return 0 to 0
    val hour = parts[0].toIntOrNull() ?: return 0 to 0
    val minute = parts[1].toIntOrNull() ?: return 0 to 0
    return hour to minute
}

/**
 * Parses a time string into the total number of minutes from the start of the day.
 * @param time The time string in the format "H:mm" or "HH:mm" to parse.
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
 * Parses a time string into the total number of milliseconds from the start of the day.
 * @param time The time string in the format "H:mm" or "HH:mm" to parse.
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
fun formatPlanDateInFull(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size < 3) return dateStr

    val month = when (parts[1]) {
        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
        else -> return dateStr
    }
    val day = parts[2].toIntOrNull() ?: return dateStr
    return "$month $day, ${parts[0]}"
}

/**
 * Parses a date string into milliseconds representing UTC midnight of that date into epoch milliseconds.
 * @param dateString The date string in "yyyy-MM-dd" format to parse.
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
 * Parses a date string and a time string into epoch milliseconds using the system's default time zone.
 * @param dateString The date string in "yyyy-MM-dd" format.
 * @param timeString The time string in "H:mm" or "HH:mm" format.
 * @return The epoch milliseconds, or null if the input strings are invalid.
 */
fun parseLocalDateTimeToMillis(dateString: String, timeString: String): Long? {
    val sdf = SimpleDateFormat("yyyy-MM-dd H:mm", Locale.getDefault()).apply {
        isLenient = false
    }
    return runCatching { sdf.parse("$dateString $timeString")?.time }.getOrNull()
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
        return targetMins <= currentMins
    }

    return false
}

/**
 * Checks if the specified time falls within the start and end range of a given [FuturePlan]
 * (inclusive start, exclusive end).
 * @param plan The plan containing the start and end dates and times to evaluate.
 * @param currentTimeMillis The reference time in milliseconds to check against,
 * defaults to the current system time.
 * @return `true` if [currentTimeMillis] is inclusive of the start time and exclusive of the end time,
 * `false` otherwise or if the date/time strings are invalid.
 */
fun isPlanActive(plan: FuturePlan, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
    val start = parseLocalDateTimeToMillis(plan.startDate, plan.startTime) ?: return false
    val end = parseLocalDateTimeToMillis(plan.endDate, plan.endTime) ?: return false
    return currentTimeMillis in start until end
}

/**
 * Calculates the duration between two date-time points and returns a formatted string.
 * @param startDate The starting date in "yyyy-MM-dd" format.
 * @param endDate The ending date in "yyyy-MM-dd" format.
 * @param startTime The starting time in "H:mm" or "HH:mm" format.
 * @param endTime The ending time in "H:mm" or "HH:mm" format.
 * @return A user-friendly string representing the duration (e.g., "1d 2h 30m", "5h 15m", "45m").
 * Returns "0m" if the duration is zero or negative.
 */
fun calculateDuration(
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String
): String {
    val startDateMillis = parseDateToMillis(startDate) ?: 0L
    val endDateMillis = parseDateToMillis(endDate) ?: 0L
    val startTimeMinutes = parseTimeToMinutes(startTime) ?: 0
    val endTimeMinutes = parseTimeToMinutes(endTime) ?: 0

    val totalMins = ((endDateMillis - startDateMillis) / 60000L) + endTimeMinutes - startTimeMinutes

    if (totalMins <= 0) return "0m"

    val days = totalMins / 1440
    val remainingMinsAfterDays = totalMins % 1440
    val hours = remainingMinsAfterDays / 60
    val minutes = remainingMinsAfterDays % 60

    return when {
        days > 0 -> {
            val d = "${days}d"
            val h = if (hours > 0) " ${hours}h" else ""
            val m = if (minutes > 0) " ${minutes}m" else ""
            "$d$h$m".trim()
        }

        hours > 0 -> {
            val h = "${hours}h"
            val m = if (minutes > 0) " ${minutes}m" else ""
            "$h$m".trim()
        }

        else -> "${minutes}m"
    }
}

/**
 * Validates whether a string of exactly 8 digits represents a valid date according to the provided pattern string.
 * The function ensures the resulting date is not in the future.
 * @param digits A string containing 8 numeric characters.
 * @param pattern The pattern string defining the expected sequence of years, months, and days.
 * @return `true` if the string is a valid past or present date; `false` otherwise.
 */
fun isValidDateDigits(digits: String, pattern: String): Boolean {
    if (digits.length != 8) return false

    val format = SimpleDateFormat(pattern, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

    return runCatching {
        val date = format.parse(digits)
        // Ensure date is valid and not in the future (optional, depending on business logic)
        date != null && date.time <= System.currentTimeMillis()
    }.getOrDefault(false)
}

fun openSMS(
    context: Context,
    phoneNumber: String,
    message: String = "Hey! Saw you're Free2Party, want to hang out?"
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "smsto:$phoneNumber".toUri()
        putExtra("sms_body", message)
    }
    context.startActivity(intent)
}

fun openThirdPartyApp(
    context: Context,
    url: String
) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = url.toUri()
    }
    context.startActivity(intent)
}
