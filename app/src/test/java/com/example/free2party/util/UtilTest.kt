package com.example.free2party.util

import com.example.free2party.data.model.FuturePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class UtilTest {

    @Test
    fun `formatTime correctly formats hour and minute`() {
        assertEquals("9:05", formatTime(9, 5))
        assertEquals("14:30", formatTime(14, 30))
        assertEquals("0:00", formatTime(0, 0))
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
        assertEquals("Jan 1, 2026", formatPlanDate("2026-01-01"))
        assertEquals("Dec 31, 2025", formatPlanDate("2025-12-31"))
        assertEquals("invalid-date", formatPlanDate("invalid-date"))
    }

    @Test
    fun `formatPlanDate returns original string for invalid parts`() {
        // Non-numeric day
        assertEquals("2026-01-aa", formatPlanDate("2026-01-aa"))
        // Invalid month
        assertEquals("2026-13-01", formatPlanDate("2026-13-01"))
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
    fun `parseDateTimeToMillis parses local time correctly`() {
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
}
