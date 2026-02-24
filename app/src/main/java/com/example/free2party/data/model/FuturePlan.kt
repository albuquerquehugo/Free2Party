package com.example.free2party.data.model

enum class PlanVisibility {
    EVERYONE,
    EXCEPT,
    ONLY
}

data class FuturePlan(
    val id: String = "",
    val userId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val note: String = "",
    val visibility: PlanVisibility = PlanVisibility.EVERYONE,
    val friendsSelection: List<String> = emptyList()
)
