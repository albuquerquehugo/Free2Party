package com.example.free2party.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
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
 * Formats the time elapsed since the given [Date] into a [UiText].
 * @param timestamp The date to calculate the elapsed time from.
 * @return A [UiText] representing the time ago.
 */
fun formatTimeAgo(timestamp: Date?): UiText {
    if (timestamp == null) return UiText.DynamicString("")
    val diff = System.currentTimeMillis() - timestamp.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> UiText.StringResource(R.string.time_just_now)
        minutes < 60 -> UiText.StringResource(R.string.time_minutes_ago, minutes.toInt())
        hours < 24 -> UiText.StringResource(R.string.time_hours_ago, hours.toInt())
        else -> UiText.StringResource(R.string.time_days_ago, days.toInt())
    }
}

/**
 * Formats the given internal time string into a user-friendly display string.
 * @param time Internal time string in the format "H:mm" or "HH:mm".
 * @param use24Hour Whether to use 24-hour or am/pm format.
 */
fun formatTimeForDisplay(time: String, use24Hour: Boolean): String {
    val (hour, minute) = unformatTime(time)
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }

    return if (use24Hour) {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(calendar.time)
    } else {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        format.format(calendar.time)
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
 * Formats a date "yyyy-MM-dd" to localized "MMM dd, yyyy".
 * @param dateStr The date string to be formatted.
 * @return A localized formatted date string. If the format is invalid, it returns the original string
 */
fun formatPlanDateInFull(dateStr: String): String {
    val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = runCatching { sdfSource.parse(dateStr) }.getOrNull() ?: return dateStr

    val sdfDest = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdfDest.format(date)
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
 * Calculates the duration between two date-time points and returns a localized string.
 * @param startDate The starting date in "yyyy-MM-dd" format.
 * @param endDate The ending date in "yyyy-MM-dd" format.
 * @param startTime The starting time in "H:mm" or "HH:mm" format.
 * @param endTime The ending time in "H:mm" or "HH:mm" format.
 * @param context The context used to retrieve string resources.
 * @return A user-friendly string representing the duration.
 * Returns localized "0m" if the duration is zero or negative.
 */
fun calculateDuration(
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String,
    context: Context
): String {
    val startDateMillis = parseDateToMillis(startDate) ?: 0L
    val endDateMillis = parseDateToMillis(endDate) ?: 0L
    val startTimeMinutes = parseTimeToMinutes(startTime) ?: 0
    val endTimeMinutes = parseTimeToMinutes(endTime) ?: 0

    val totalMins = ((endDateMillis - startDateMillis) / 60000L) + endTimeMinutes - startTimeMinutes

    if (totalMins <= 0) return context.getString(R.string.duration_minutes, 0)

    val days = (totalMins / 1440).toInt()
    val remainingMinsAfterDays = (totalMins % 1440).toInt()
    val hours = remainingMinsAfterDays / 60
    val minutes = remainingMinsAfterDays % 60

    return when {
        days > 0 -> {
            val d = context.getString(R.string.duration_days, days)
            val h = if (hours > 0) " " + context.getString(R.string.duration_hours, hours) else ""
            val m =
                if (minutes > 0) " " + context.getString(R.string.duration_minutes, minutes) else ""
            "$d$h$m".trim()
        }

        hours > 0 -> {
            val h = context.getString(R.string.duration_hours, hours)
            val m =
                if (minutes > 0) " " + context.getString(R.string.duration_minutes, minutes) else ""
            "$h$m".trim()
        }

        else -> context.getString(R.string.duration_minutes, minutes)
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

/**
 * Validates if the given birthday string and its corresponding format pattern represent a valid date.
 * @param birthday The birthday string to validate.
 * @param context The context used to retrieve resources and start the activity.
 * @param patternResId The date pattern (e.g., "MM/dd/yyyy") used to parse the birthday string.
 * @return `true` if the birthday matches the pattern and represents a real date, `false` otherwise.
 */
fun isBirthdayFieldValid(birthday: String, context: Context, patternResId: Int?): Boolean {
    if (birthday.isNotEmpty() && birthday.length != 8) return false

    if (birthday.isEmpty()) return true
    if (patternResId == null) return true

    val pattern = context.getString(patternResId).filter { it.isLetter() }
    return isValidDateDigits(birthday, pattern)
}

/**
 * Validates whether the given string is a valid phone number.
 * @param number The phone number string to validate.
 * @param countryCode The phone country code string to validate.
 * @return `true` if the phone number is valid; `false` otherwise.
 */
fun isPhoneValid(number: String, countryCode: String): Boolean {
    if (number.isEmpty()) return countryCode.isEmpty()
    val country = com.example.free2party.data.model.Countries.find { it.code == countryCode }
    return country == null || number.length == country.digitsCount
}

/**
 * Opens the default email client with a pre-filled recipient, subject, and body.
 * @param context The context used to retrieve resources and start the activity.
 * @param email The recipient's email address.
 * @param subject The subject line of the email. Defaults to the app name.
 * @param body The message body of the email. Defaults to a standard hang-out message.
 */
fun openEmail(
    context: Context,
    email: String,
    subject: String = context.getString(R.string.app_name),
    body: String = context.getString(R.string.text_hang_out_message)
) {
    val mailto = "mailto:$email" +
            "?subject=${Uri.encode(subject)}" +
            "&body=${Uri.encode(body)}"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = mailto.toUri()
    }
    context.startActivity(intent)
}

/**
 * Opens the device's default SMS application with a pre-filled phone number and message.
 * @param context The context used to start the activity and retrieve string resources.
 * @param phoneNumber The recipient's phone number.
 * @param message The body text of the SMS. Defaults to a standard hang-out message from resources.
 */
fun openSMS(
    context: Context,
    phoneNumber: String,
    message: String = context.getString(R.string.text_hang_out_message)
) {
    val smsto = "smsto:$phoneNumber" +
            "?body=${Uri.encode(message)}"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = smsto.toUri()
    }
    context.startActivity(intent)
}

/**
 * Supported social platforms for messaging.
 */
enum class SocialPlatform(val baseUrl: String, val packageName: String) {
    MESSENGER("https://m.me/", "com.facebook.orca"),
    INSTAGRAM("https://ig.me/m/", "com.instagram.android"),
    TELEGRAM("https://t.me/", "org.telegram.messenger"),
    WHATSAPP("https://wa.me/", "com.whatsapp"),
    TIKTOK("https://tiktok.com/@", "com.zhiliaoapp.musically"),
    X("https://x.com/", "com.twitter.android")
}

/**
 * Opens a social messaging app to a specific user's chat.
 * Fallbacks to the web version if the app is not installed.
 * @param context The context used to start the activity.
 * @param platform The [SocialPlatform] to open.
 * @param username The username to message.
 * @param message Optional message to pre-fill (supported by some platforms like WhatsApp).
 */
fun openSocialMessage(
    context: Context,
    platform: SocialPlatform,
    username: String,
    message: String? = null
) {
    val url = when (platform) {
        SocialPlatform.WHATSAPP -> {
            if (message != null) {
                "${platform.baseUrl}$username?text=${Uri.encode(message)}"
            } else {
                "${platform.baseUrl}$username"
            }
        }

        else -> "${platform.baseUrl}$username"
    }

    val uri = url.toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        // Try to specifically target the app if possible
        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            `package` = platform.packageName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appIntent)
    } catch (_: Exception) {
        // Fallback to generic intent (browser or user choice)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_no_app_available),
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }
}

/**
 * Detects the country code from a 3-digit North American Numbering Plan (NANP) area code.
 * This function maps specific area codes to their respective ISO 3166-1 alpha-2 country codes.
 * If the area code is not found in the specific mapping but starts with a digit between 2 and 9,
 * it defaults to "US".
 * @param areaCode A string containing at least 3 digits representing the area code.
 * @return The 2-letter ISO country code (e.g., "CA", "PR", "US") or `null` if the
 * input is invalid or not a recognized NANP area code.
 */
fun getCountryFromNanpAreaCode(areaCode: String): String? {
    if (areaCode.length < 3) return null
    val code = areaCode.take(3)
    val areaCodeMap = mapOf(
        "204" to "CA",
        "226" to "CA",
        "236" to "CA",
        "249" to "CA",
        "250" to "CA",
        "263" to "CA",
        "289" to "CA",
        "306" to "CA",
        "343" to "CA",
        "365" to "CA",
        "367" to "CA",
        "368" to "CA",
        "382" to "CA",
        "403" to "CA",
        "416" to "CA",
        "418" to "CA",
        "431" to "CA",
        "437" to "CA",
        "438" to "CA",
        "450" to "CA",
        "474" to "CA",
        "506" to "CA",
        "514" to "CA",
        "519" to "CA",
        "548" to "CA",
        "579" to "CA",
        "581" to "CA",
        "584" to "CA",
        "587" to "CA",
        "604" to "CA",
        "613" to "CA",
        "639" to "CA",
        "647" to "CA",
        "672" to "CA",
        "683" to "CA",
        "705" to "CA",
        "709" to "CA",
        "742" to "CA",
        "753" to "CA",
        "778" to "CA",
        "780" to "CA",
        "782" to "CA",
        "807" to "CA",
        "819" to "CA",
        "825" to "CA",
        "867" to "CA",
        "873" to "CA",
        "879" to "CA",
        "902" to "CA",
        "905" to "CA",
        "242" to "BS",
        "246" to "BB",
        "264" to "AI",
        "268" to "AG",
        "284" to "VG",
        "345" to "KY",
        "441" to "BM",
        "473" to "GD",
        "649" to "TC",
        "664" to "MS",
        "758" to "LC",
        "767" to "DM",
        "784" to "VC",
        "787" to "PR",
        "939" to "PR",
        "809" to "DO",
        "829" to "DO",
        "849" to "DO",
        "868" to "TT",
        "869" to "KN",
        "876" to "JM",
        "658" to "JM",
        "721" to "SX",
        "670" to "MP",
        "671" to "GU",
        "684" to "AS",
        "340" to "VI"
    )

    return areaCodeMap[code] ?: if (code[0] in '2'..'9') "US" else null
}
