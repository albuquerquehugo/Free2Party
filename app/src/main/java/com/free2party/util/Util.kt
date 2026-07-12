package com.free2party.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.free2party.data.model.Countries
import com.free2party.data.model.BirthdayShowType
import com.free2party.data.model.DistanceUnit
import com.free2party.data.model.FuturePlan
import com.free2party.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import kotlin.math.*

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
 * Extracts a name and an email address from a string, typically formatted as "Name (email)".
 * Cleans up common prefixes used in system notifications based on the current context.
 * @return A [Pair] containing the cleaned name and email if a match is found;
 * otherwise, `null` if the string does not follow the expected pattern.
 */
fun String.matchNameAndEmail(context: Context): Pair<String, String>? {
    val regex = Regex("""([^()]+) \(([^()]+)\)""")
    val matchResult = regex.find(this)
    return if (matchResult != null && matchResult.groupValues.size >= 3) {
        var cleanName = matchResult.groupValues[1]

        // Strip prefixes defined in strings.xml
        val prefixes = context.resources.getStringArray(R.array.notification_prefixes_to_strip)
        for (prefix in prefixes) {
            if (cleanName.contains(prefix, ignoreCase = true)) {
                cleanName = cleanName.substringAfter(prefix)
            }
        }

        cleanName.trim() to matchResult.groupValues[2].trim()
    } else null
}

/**
 * Parses an event invitation notification message to extract the host name, host email, and event title.
 * @return A [Triple] containing (hostName, hostEmail, eventTitle) if a match is found;
 * otherwise, `null` if the message does not follow the expected pattern.
 */
fun String.matchEventInvitation(): Triple<String, String, String>? {
    val regex = Regex("""^([^()]+) \(([^()]+)\).*[\\"]([^\\"]+)[\\"].*$""")
    val matchResult = regex.find(this)
    return if (matchResult != null && matchResult.groupValues.size >= 4) {
        val name = matchResult.groupValues[1].trim()
        val email = matchResult.groupValues[2].trim()
        val eventTitle = matchResult.groupValues[3].trim()
        Triple(name, email, eventTitle)
    } else null
}

/**
 * Parses an event comment notification message to extract the commenter's name, event title, and comment text.
 * @return A [Triple] containing (commenterName, eventTitle, commentText) if a match is found;
 * otherwise, `null` if the message does not follow the expected pattern.
 */
fun String.matchEventComment(): Triple<String, String, String>? {
    val regex = Regex("""^([^"]+?)\s+(?:commented on event|comentou no evento)\s+\\?[\\"]([^\\"]+)\\?[\\"]:\s*(.*)$""", RegexOption.IGNORE_CASE)
    val matchResult = regex.find(this)
    return if (matchResult != null && matchResult.groupValues.size >= 4) {
        val name = matchResult.groupValues[1].trim()
        val eventTitle = matchResult.groupValues[2].trim()
        val commentText = matchResult.groupValues[3].trim()
        Triple(name, eventTitle, commentText)
    } else null
}


/**
 * Capitalizes the first letter of each word in the string, separated by spaces.
 * @return A new string with each word capitalized.
 */
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.capitalizeFirstLetter() }
}

/**
 * Removes accents (diacritics) from the string.
 * @return A new string without accents.
 */
fun String.removeAccents(): String {
    val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(normalized).replaceAll("")
}

/**
 * Capitalizes the first letter of a string using the system's default locale.
 * Useful for month names and other localized strings that might be lowercase by default.
 * @return The string with the first character converted to title case, or the original string
 * if it is empty or the first character is already capitalized.
 */
fun String.capitalizeFirstLetter(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
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
    val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = runCatching { sdfSource.parse(dateStr) }.getOrNull() ?: return dateStr

    val sdfDest = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdfDest.format(date).capitalizeFirstLetter()
}

/**
 * Parses a date string into milliseconds representing UTC midnight of that date into epoch milliseconds.
 * @param dateString The date string in "yyyy-MM-dd" format to parse.
 * @return The time in milliseconds since the epoch, or null if parsing fails.
 */
fun parseDateToMillis(dateString: String): Long? {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
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
    val sdf = SimpleDateFormat("yyyy-MM-dd H:mm", Locale.US).apply {
        isLenient = false
    }
    return runCatching { sdf.parse("$dateString $timeString")?.time }.getOrNull()
}

/**
 * Logic for determining if a date/time has already passed.
 * Uses system default calendar to ensure consistency with real-time app usage.
 * @param dateUtcMillis The date in UTC milliseconds (start of day).
 * @param timeString Optional time string (HH:mm). If provided, checks time if date is today.
 * @param now Reference time for "today". Defaults to current system time.
 */
fun isDateTimeInPast(
    dateUtcMillis: Long,
    timeString: String? = null,
    now: Calendar = Calendar.getInstance()
): Boolean {
    // We need to compare the "start of day" in UTC (dateUtcMillis) 
    // against the "start of day" of the current reference time.
    val today = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Convert dateUtcMillis (which is start of day UTC) to a Calendar for comparison
    val targetDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
        timeInMillis = dateUtcMillis
    }
    
    // Normalize targetDate to the same timezone as 'today' for a fair comparison of Y/M/D
    val targetInLocal = Calendar.getInstance(today.timeZone).apply {
        set(Calendar.YEAR, targetDate.get(Calendar.YEAR))
        set(Calendar.MONTH, targetDate.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, targetDate.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (targetInLocal.before(today)) return true
    
    if (targetInLocal.equals(today) && timeString != null) {
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
 * @return A UiText representing the duration.
 * Returns localized "0m" if the duration is zero or negative.
 */
fun calculateDuration(
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String
): UiText {
    val startDateMillis = parseDateToMillis(startDate) ?: 0L
    val endDateMillis = parseDateToMillis(endDate) ?: 0L
    val startTimeMinutes = parseTimeToMinutes(startTime) ?: 0
    val endTimeMinutes = parseTimeToMinutes(endTime) ?: 0

    val totalMins = ((endDateMillis - startDateMillis) / 60000L) + endTimeMinutes - startTimeMinutes

    if (totalMins <= 0) return UiText.StringResource(R.string.duration_minutes, 0)

    val days = (totalMins / 1440).toInt()
    val remainingMinsAfterDays = (totalMins % 1440).toInt()
    val hours = remainingMinsAfterDays / 60
    val minutes = remainingMinsAfterDays % 60

    val parts = buildList {
        if (days > 0) add(UiText.StringResource(R.string.duration_days, days))
        if (hours > 0) add(UiText.StringResource(R.string.duration_hours, hours))
        if (minutes > 0 || isEmpty()) add(UiText.StringResource(R.string.duration_minutes, minutes))
    }

    return if (parts.size == 1) parts[0] else UiText.Composite(parts, " ")
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

    val format = SimpleDateFormat(pattern, Locale.US).apply {
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
    val country = Countries.find { it.code == countryCode }
    return country == null || number.length == country.digitsCount
}

/**
 * Formats a birthday string based on its visibility and display type preference.
 * @param birthday The 8-digit birthday string in "yyyyMMdd" format.
 * @param showType The [BirthdayShowType] preference.
 * @return The formatted date string, or the original string if parsing fails.
 */
fun formatBirthday(birthday: String, showType: BirthdayShowType): String {
    if (birthday.length != 8) return birthday
    return try {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = sdf.parse(birthday) ?: return birthday
        if (showType == BirthdayShowType.DAY_MONTH) {
            val skeleton = "MMMMd"
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                skeleton
            )
            val format = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.format(date)
        } else {
            val format = java.text.DateFormat.getDateInstance(
                java.text.DateFormat.LONG,
                Locale.getDefault()
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.format(date)
        }
    } catch (_: Exception) {
        birthday
    }
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
 * Opens the device's dialer with a pre-filled phone number.
 * @param context The context used to start the activity.
 * @param phoneNumber The phone number to dial.
 */
fun openDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = "tel:$phoneNumber".toUri()
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
 * @param platform [SocialPlatform] to open.
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
 * Validates whether the given string is a valid URL.
 * @param url The URL string to validate.
 * @return `true` if the URL is valid; `false` otherwise.
 */
fun isUrlValid(url: String): Boolean {
    val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }
    return android.util.Patterns.WEB_URL.matcher(formattedUrl).matches()
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

/**
 * Formats a phone number string according to a target mask (e.g. "###-###-####").
 * @param number The raw input phone number.
 * @param mask The pattern mask.
 * @return The formatted phone number.
 */
fun formatPhoneNumber(number: String, mask: String): String {
    val trimmed = number.filter { it.isDigit() }
    val out = StringBuilder()
    var maskIndex = 0
    var textIndex = 0

    while (maskIndex < mask.length && textIndex < trimmed.length) {
        if (mask[maskIndex] == '#') {
            out.append(trimmed[textIndex])
            textIndex++
        } else {
            out.append(mask[maskIndex])
        }
        maskIndex++
    }

    if (textIndex < trimmed.length) {
        out.append(trimmed.substring(textIndex))
    }

    return out.toString()
}

/**
 * Calculates the distance between two geographical coordinates using the Haversine formula.
 * @return The distance in meters.
 */
fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371e3 // Earth's radius in meters
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaPhi = Math.toRadians(lat2 - lat1)
    val deltaLambda = Math.toRadians(lon2 - lon1)

    val a = sin(deltaPhi / 2.0).pow(2.0) +
            cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0).pow(2.0)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

    return r * c
}

/**
 * Formats a distance in meters to a human-readable localized string.
 */
fun formatDistance(
    context: Context,
    meters: Double,
    unit: DistanceUnit = DistanceUnit.KILOMETERS
): String {
    return when (unit) {
        DistanceUnit.KILOMETERS -> {
            if (meters < 1000) {
                context.getString(R.string.label_distance_metric_m, meters.roundToInt())
            } else {
                val km = meters / 1000.0
                context.getString(R.string.label_distance_metric_km, km)
            }
        }

        DistanceUnit.MILES -> {
            val miles = meters * 0.000621371
            if (miles < 0.1) {
                val feet = meters * 3.28084
                context.getString(R.string.label_distance_imperial_ft, feet.roundToInt())
            } else {
                context.getString(R.string.label_distance_imperial_mi, miles)
            }
        }
    }
}


/**
 * Gets the last known device location from available providers.
 */
fun getLastKnownLocation(context: Context): Location? {
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }
    val providers = locationManager.getProviders(true)
    var bestLocation: Location? = null
    for (provider in providers) {
        val l = locationManager.getLastKnownLocation(provider) ?: continue
        if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
            bestLocation = l
        }
    }
    return bestLocation
}
