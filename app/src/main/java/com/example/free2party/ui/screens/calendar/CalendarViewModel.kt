package com.example.free2party.ui.screens.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.repository.PlanRepository
import com.example.free2party.data.repository.PlanRepositoryImpl
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMillis
import com.example.free2party.util.parseTimeToMinutes
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.TimeZone

class CalendarViewModel(
    targetUserId: String? = null
) : ViewModel() {
    private val planRepository: PlanRepository = PlanRepositoryImpl(
        db = Firebase.firestore,
        currentUserId = Firebase.auth.currentUser?.uid ?: ""
    )

    var plansList by mutableStateOf<List<FuturePlan>>(emptyList())
    val filteredPlans: List<FuturePlan>
        get() {
            val selectedDate = selectedDateMillis ?: return emptyList()

            return plansList.filter { plan ->
                val planStartMillis = parseDateToMillis(plan.startDate) ?: return@filter false
                val planEndMillis = parseDateToMillis(plan.endDate) ?: return@filter false

                val planDateTimeStart = planStartMillis + parseTimeToMillis(plan.startTime)
                val planDateTimeEnd = planEndMillis + parseTimeToMillis(plan.endTime)

                val nextDay = selectedDate + 86400000L // 24 hours later

                // Overlap: max(start1, start2) < min(end1, end2)
                planDateTimeStart.coerceAtLeast(selectedDate) < planDateTimeEnd.coerceAtMost(nextDay)
            }.sortedBy { plan -> parseTimeToMinutes(plan.startTime) }
        }

    var displayedMonth by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var displayedYear by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))

    var selectedDateMillis by mutableStateOf<Long?>(null)

    val userIdToObserve = targetUserId ?: Firebase.auth.currentUser?.uid ?: ""

    init {
        goToToday()
        observePlans()
    }

    // TODO: Implement visibility restrictions to plans (all friends, except chosen ones,
    //  exclusively to chosen ones)

    private fun observePlans() {
        if (userIdToObserve.isBlank()) return

        planRepository.getPlans(userIdToObserve)
            .onEach { plansList = it }
            .launchIn(viewModelScope)
    }

    fun savePlan(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        note: String,
        onValidationError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        val plan = FuturePlan(
            userId = userIdToObserve,
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            note = note
        )
        viewModelScope.launch {
            planRepository.savePlan(plan)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    onValidationError(
                        e.localizedMessage ?: "Failed to save the plan."
                    )
                }
        }
    }

    fun updatePlan(
        planId: String,
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        note: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        val updatedPlan = FuturePlan(
            id = planId,
            userId = userIdToObserve,
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            note = note
        )
        viewModelScope.launch {
            planRepository.updatePlan(updatedPlan)
                .onSuccess { onSuccess() }
                .onFailure { e -> onError(e.localizedMessage ?: "Failed to update the plan.") }
        }
    }

    fun deletePlan(planId: String, onError: (String) -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            planRepository.deletePlan(planId)
                .onSuccess { onSuccess() }
                .onFailure { e -> onError(e.localizedMessage ?: "Failed to delete the plan.") }
        }
    }

    fun getPlannedDaysForMonth(year: Int, month: Int): Set<Int> {
        val targetMonth = YearMonth.of(year, month + 1)
        val monthStart = targetMonth.atDay(1)
        val monthEnd = targetMonth.atEndOfMonth()

        return plansList.flatMap { plan ->
            val planStart = runCatching { LocalDate.parse(plan.startDate) }.getOrNull()
                ?: return@flatMap emptyList<Int>()
            var planEnd = runCatching { LocalDate.parse(plan.endDate) }.getOrNull()
                ?: return@flatMap emptyList<Int>()

            // If it ends at exactly midnight (0:00), the end date is exclusive.
            if (parseTimeToMinutes(plan.endTime) == 0 && planEnd.isAfter(planStart)) {
                planEnd = planEnd.minusDays(1)
            }

            // Check if plan range overlaps with the target month
            if (!planStart.isAfter(monthEnd) && !planEnd.isBefore(monthStart)) {
                val startDay = if (planStart.isBefore(monthStart)) 1 else planStart.dayOfMonth
                val endDay =
                    if (planEnd.isAfter(monthEnd)) targetMonth.lengthOfMonth() else planEnd.dayOfMonth
                startDay..endDay
            } else {
                emptyList()
            }
        }.toSet()
    }

    fun moveToPreviousMonth() {
        if (displayedMonth == 0) {
            displayedMonth = 11
            displayedYear--
        } else {
            displayedMonth--
        }
    }

    fun moveToNextMonth() {
        if (displayedMonth == 11) {
            displayedMonth = 0
            displayedYear++
        } else {
            displayedMonth++
        }
    }

    fun goToToday() {
        val localNow = Calendar.getInstance()
        displayedMonth = localNow.get(Calendar.MONTH)
        displayedYear = localNow.get(Calendar.YEAR)

        val utcToday = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, localNow.get(Calendar.YEAR))
            set(Calendar.MONTH, localNow.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, localNow.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedDateMillis = utcToday.timeInMillis
    }

    fun selectDate(day: Int) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedDateMillis = calendar.timeInMillis
    }
}
