package com.free2party.ui.screens.calendar

import com.free2party.data.model.FuturePlan
import com.free2party.data.model.Event

sealed interface CalendarEntry {
    val id: String
    val startDate: String
    val startTime: String
    val endDate: String
    val endTime: String

    data class Plan(val plan: FuturePlan) : CalendarEntry {
        override val id: String get() = plan.id
        override val startDate: String get() = plan.startDate
        override val startTime: String get() = plan.startTime
        override val endDate: String get() = plan.endDate
        override val endTime: String get() = plan.endTime
    }

    data class EventItem(val event: Event) : CalendarEntry {
        override val id: String get() = event.id
        override val startDate: String get() = event.startDate
        override val startTime: String get() = event.startTime
        override val endDate: String get() = event.endDate
        override val endTime: String get() = event.endTime
    }
}
