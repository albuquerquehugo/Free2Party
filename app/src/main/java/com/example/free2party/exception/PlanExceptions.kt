package com.example.free2party.exception

import androidx.annotation.StringRes
import com.example.free2party.R

sealed class PlanException(
    message: String,
    @get:StringRes val messageRes: Int? = null
) : Exception(message)

class PlanNotFoundException(
    message: String? = null
) : PlanException(message ?: "PlanNotFoundException", R.string.error_plan_not_found)

class OverlappingPlanException(
    message: String? = null
) : PlanException(message ?: "OverlappingPlanException", R.string.error_overlapping_plan)

class InvalidPlanDataException(
    message: String? = null
) : PlanException(message ?: "InvalidPlanDataException", R.string.error_invalid_plan_data)

class PastDateTimeException(
    message: String? = null
) : PlanException(message ?: "PastDateTimeException", R.string.error_past_date_time)
