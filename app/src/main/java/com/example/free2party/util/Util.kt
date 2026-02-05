package com.example.free2party.util

/**
 * Formats the given hour and minute into a string with the format "H:mm" or "HH:mm".
 *
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
 *
 * @param time The formatted time string to parse.
 * @return A [Pair] where the first element is the hour and the second is the minute.
 */
fun unformatTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    return parts[0].toInt() to parts[1].toInt()
}


/**
 * Converts a time string in the format "H:mm" or "HH:mm" into the total number of minutes.
 *
 * @param time The time string to convert.
 * @return The total number of minutes represented by the time string.
 */
fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return try {
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}
