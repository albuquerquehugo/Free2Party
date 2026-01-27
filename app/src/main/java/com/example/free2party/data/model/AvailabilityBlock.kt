package com.example.free2party.data.model

import java.util.Date

data class AvailabilityBlock(
    val userId: String = "",
    val startTime: Date = Date(),
    val endTime: Date = Date(),
    val type: String = "PARTY" // "PARTY", "MEETING", "CHILL"
)