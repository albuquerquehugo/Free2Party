package com.free2party.util

import com.free2party.data.model.DistanceUnit
import com.free2party.data.model.FuturePlan
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class UtilTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `formatTime correctly formats hour and minute`() {
        assertEquals("9:05", formatTime(9, 5))
        assertEquals("14:30", formatTime(14, 30))
        assertEquals("0:00", formatTime(0, 0))
    }

    @Test
    fun `formatTimeForDisplay handles 24-hour format`() {
        assertEquals("14:30", formatTimeForDisplay("14:30", true))
        assertEquals("09:05", formatTimeForDisplay("9:05", true))
        assertEquals("00:00", formatTimeForDisplay("0:00", true))
    }

    @Test
    fun `formatTimeForDisplay handles AM-PM format`() {
        assertEquals("2:30 PM", formatTimeForDisplay("14:30", false))
        assertEquals("9:05 AM", formatTimeForDisplay("9:05", false))
        assertEquals("12:00 AM", formatTimeForDisplay("0:00", false))
        assertEquals("12:00 PM", formatTimeForDisplay("12:00", false))
    }

    @Test
    fun `unformatTime correctly parses time string`() {
        val (hour1, min1) = unformatTime("9:05")
        assertEquals(9, hour1)
        assertEquals(5, min1)

        val (hour2, min2) = unformatTime("14:30")
        assertEquals(14, hour2)
        assertEquals(30, min2)
    }

    @Test
    fun `unformatTime returns 0-0 for invalid input`() {
        assertEquals(0 to 0, unformatTime("invalid"))
        assertEquals(0 to 0, unformatTime("abc:def"))
        assertEquals(0 to 0, unformatTime("12:"))
    }

    @Test
    fun `parseTimeToMinutes converts time string correctly`() {
        assertEquals(545, parseTimeToMinutes("9:05"))
        assertEquals(870, parseTimeToMinutes("14:30"))
        assertEquals(0, parseTimeToMinutes("0:00"))
        assertNull(parseTimeToMinutes("invalid"))
    }

    @Test
    fun `parseTimeToMinutes returns null for malformed strings`() {
        assertNull(parseTimeToMinutes("12:aa"))
        assertNull(parseTimeToMinutes("bb:30"))
        assertNull(parseTimeToMinutes("12:30:45"))
    }

    @Test
    fun `formatPlanDate correctly formats yyyy-MM-dd`() {
        assertEquals("Jan 01, 2026", formatPlanDateInFull("2026-01-01"))
        assertEquals("Dec 31, 2025", formatPlanDateInFull("2025-12-31"))
        assertEquals("invalid-date", formatPlanDateInFull("invalid-date"))
    }

    @Test
    fun `formatPlanDate returns original string for invalid parts`() {
        // Non-numeric day
        assertEquals("2026-01-aa", formatPlanDateInFull("2026-01-aa"))
    }

    @Test
    fun `parseDateToMillis returns UTC midnight in millis`() {
        val millis = parseDateToMillis("2026-01-01")
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis!!
        }
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun `parseDateToMillis returns null for invalid date`() {
        assertNull(parseDateToMillis("invalid"))
        assertNull(parseDateToMillis("2026-13-45"))
    }

    @Test
    fun `parseLocalDateTimeToMillis parses local time correctly`() {
        val millis = parseLocalDateTimeToMillis("2026-05-20", "14:30")
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis!!
        }

        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, calendar.get(Calendar.MONTH))
        assertEquals(20, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun `isPlanActive returns true when current time is within range`() {
        val plan = FuturePlan(
            startDate = "2026-05-20",
            endDate = "2026-05-20",
            startTime = "14:00",
            endTime = "16:00"
        )

        val now = parseLocalDateTimeToMillis("2026-05-20", "15:00")!!
        assertTrue(isPlanActive(plan, now))
    }

    @Test
    fun `isPlanActive returns false when current time is outside range`() {
        val plan = FuturePlan(
            startDate = "2026-05-20",
            endDate = "2026-05-20",
            startTime = "14:00",
            endTime = "16:00"
        )

        val before = parseLocalDateTimeToMillis("2026-05-20", "13:59")!!
        val after = parseLocalDateTimeToMillis("2026-05-20", "16:00")!!

        assertFalse(isPlanActive(plan, before))
        assertFalse(isPlanActive(plan, after))
    }

    @Test
    fun `isDateTimeInPast returns true for past dates`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 20, 12, 0)
        }
        val pastDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.MAY, 19, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertTrue(isDateTimeInPast(pastDateMillis, null, now))
    }

    @Test
    fun `isDateTimeInPast returns false for future dates`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 20, 12, 0)
        }
        val futureDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.MAY, 21, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertFalse(isDateTimeInPast(futureDateMillis, null, now))
    }

    @Test
    fun `isDateTimeInPast handles time for today correctly`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 20, 12, 30)
        }
        val todayMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.MAY, 20, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertTrue(isDateTimeInPast(todayMillis, "12:00", now))
        assertTrue(isDateTimeInPast(todayMillis, "12:30", now))
        assertFalse(isDateTimeInPast(todayMillis, "13:00", now))
        assertTrue(isDateTimeInPast(todayMillis, "invalid", now))
    }

    @Test
    fun `formatDistance formatting for Kilometers and Miles`() {
        val context = mockk<android.content.Context>(relaxed = true)

        every { context.getString(com.free2party.R.string.label_distance_metric_m, any()) } answers {
            val formatArgs = secondArg<Any>()
            val value = if (formatArgs is Array<*>) formatArgs[0] else formatArgs
            "$value m away"
        }
        every { context.getString(com.free2party.R.string.label_distance_metric_km, any()) } answers {
            val formatArgs = secondArg<Any>()
            val value = if (formatArgs is Array<*>) formatArgs[0] else formatArgs
            "$value km away"
        }
        every { context.getString(com.free2party.R.string.label_distance_imperial_ft, any()) } answers {
            val formatArgs = secondArg<Any>()
            val value = if (formatArgs is Array<*>) formatArgs[0] else formatArgs
            "$value ft away"
        }
        every { context.getString(com.free2party.R.string.label_distance_imperial_mi, any()) } answers {
            val formatArgs = secondArg<Any>()
            val value = if (formatArgs is Array<*>) formatArgs[0] else formatArgs
            "$value mi away"
        }

        // Test Kilometers
        assertEquals("500 m away", formatDistance(context, 500.0, DistanceUnit.KILOMETERS))
        assertEquals("1.5 km away", formatDistance(context, 1500.0, DistanceUnit.KILOMETERS))

        // Test Miles (meters to miles/feet)
        // 50 meters is ~164 feet, which is < 0.1 miles (~160 meters) -> feet
        assertEquals("164 ft away", formatDistance(context, 50.0, DistanceUnit.MILES))
        // 2000 meters is ~1.24 miles -> miles
        val milesText = formatDistance(context, 2000.0, DistanceUnit.MILES)
        assertTrue(milesText.contains("mi away"))
    }

    @Test
    fun `matchEventInvitation correctly parses english and portuguese invite messages`() {
        val engMsg1 = "John Doe (john@example.com) invited you to the event \"My Birthday\"."
        val ptMsg1 = "John Doe (john@example.com) convidou você para o evento \"My Birthday\"."
        val engMsg2 = "John Doe (john@example.com) invited you to the event \\\"My Birthday\\\"."
        val ptMsg2 = "John Doe (john@example.com) convidou você para o evento \\\"My Birthday\\\"."

        val expected = Triple("John Doe", "john@example.com", "My Birthday")

        assertEquals(expected, engMsg1.matchEventInvitation())
        assertEquals(expected, ptMsg1.matchEventInvitation())
        assertEquals(expected, engMsg2.matchEventInvitation())
        assertEquals(expected, ptMsg2.matchEventInvitation())

        assertNull("Invalid format".matchEventInvitation())
    }

    @Test
    fun `matchEventComment correctly parses english and portuguese comment messages`() {
        val engMsg1 = "Alice commented on event \"My Party\": Hey!"
        val ptMsg1 = "Alice comentou no evento \"My Party\": Hey!"
        val engMsg2 = "Alice Smith commented on event \\\"My Party\\\": Hey: \"what's up?\""
        val ptMsg2 = "Alice Smith comentou no evento \\\"My Party\\\": Hey: \"what's up?\""

        val expected1 = Triple("Alice", "My Party", "Hey!")
        val expected2 = Triple("Alice Smith", "My Party", "Hey: \"what's up?\"")

        assertEquals(expected1, engMsg1.matchEventComment())
        assertEquals(expected1, ptMsg1.matchEventComment())
        assertEquals(expected2, engMsg2.matchEventComment())
        assertEquals(expected2, ptMsg2.matchEventComment())

        assertNull("Invalid format".matchEventComment())
    }
}

