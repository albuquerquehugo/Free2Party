package com.example.free2party.util

import com.example.free2party.data.model.DatePattern
import com.example.free2party.data.model.FuturePlan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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

fun formatPlanDateShort(dateStr: String, pattern: DatePattern = DatePattern.YYYY_MM_DD): String {
    val internalSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val targetSdf = SimpleDateFormat(pattern.pattern, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    return runCatching {
        val date = internalSdf.parse(dateStr)
        if (date != null) targetSdf.format(date) else dateStr
    }.getOrDefault(dateStr)
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
 * Validates whether a string of exactly 8 digits represents a valid date according to the provided pattern.
 * The function strips separators from the pattern to match the digit-only input and ensures
 * the resulting date is not in the future.
 * @param digits A string containing 8 numeric characters.
 * @param pattern The [DatePattern] defining the expected sequence of years, months, and days.
 * @return `true` if the string is a valid past or present date; `false` otherwise.
 */
fun isValidDateDigits(digits: String, pattern: DatePattern): Boolean {
    if (digits.length != 8) return false
    
    val format = SimpleDateFormat(pattern.pattern.replace("-", ""), Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    
    return runCatching {
        val date = format.parse(digits)
        // Ensure date is valid and not in the future (optional, depending on business logic)
        date != null && date.time <= System.currentTimeMillis()
    }.getOrDefault(false)
}
