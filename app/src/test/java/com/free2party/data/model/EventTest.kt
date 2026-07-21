package com.free2party.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EventTest {

    @Test
    fun `event without end time uses end of day as end time`() {
        val event = Event(
            id = "event123",
            startDate = "2026-07-21",
            startTime = "14:00",
            endDate = "2026-07-21",
            endTime = ""
        )

        val endMillis = event.getEndMillis()
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = endMillis
        }

        assertTrue(endMillis > 0)
        org.junit.Assert.assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY))
        org.junit.Assert.assertEquals(59, calendar.get(Calendar.MINUTE))
        org.junit.Assert.assertEquals(59, calendar.get(Calendar.SECOND))
    }

    @Test
    fun `event without end time is not ended during the event day`() {
        val event = Event(
            id = "event123",
            startDate = "2026-07-21",
            startTime = "14:00",
            endDate = "",
            endTime = ""
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sameDayAfterStart = sdf.parse("2026-07-21 16:00")!!.time
        val nextDayAfterEnd = sdf.parse("2026-07-22 00:01")!!.time

        assertFalse(event.isEnded(now = sameDayAfterStart))
        assertTrue(event.isEnded(now = nextDayAfterEnd))
    }
}
