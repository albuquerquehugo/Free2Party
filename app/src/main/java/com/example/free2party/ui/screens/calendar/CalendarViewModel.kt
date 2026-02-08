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
import com.example.free2party.util.timeToMinutes
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CalendarViewModel : ViewModel() {
    private val planRepository: PlanRepository = PlanRepositoryImpl(
        db = Firebase.firestore,
        currentUserId = Firebase.auth.currentUser?.uid ?: ""
    )

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    var plansList by mutableStateOf<List<FuturePlan>>(emptyList())
    val filteredPlans: List<FuturePlan>
        get() {
            val selectedDate =
                selectedDateMillis?.let { formatMillisToDateString(it) } ?: return emptyList()
            return plansList.filter { it.date == selectedDate }
                .sortedBy { plan -> timeToMinutes(plan.startTime) }
        }

    var displayedMonth by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var displayedYear by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))

    var selectedDateMillis by mutableStateOf<Long?>(null)

    init {
        goToToday()
        observePlans()
    }

    private fun observePlans() {
        planRepository.getPlans()
            .onEach { plansList = it }
            .launchIn(viewModelScope)
    }

    fun formatMillisToDateString(millis: Long): String {
        return dateFormatter.format(Date(millis))
    }

    fun savePlan(
        date: Long,
        startTime: String,
        endTime: String,
        note: String,
        onValidationError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        val plan = FuturePlan(
            date = formatMillisToDateString(date),
            startTime = startTime,
            endTime = endTime,
            note = note
        )
        viewModelScope.launch {
            planRepository.savePlan(plan)
                .onSuccess { onSuccess() }
                .onFailure { e -> onValidationError(e.localizedMessage ?: "Failed to save the plan.") }
        }
    }

    fun updatePlan(
        planId: String,
        date: Long,
        startTime: String,
        endTime: String,
        note: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        val updatedPlan = FuturePlan(
            id = planId,
            date = formatMillisToDateString(date),
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
        val monthString = (month + 1).toString().padStart(2, '0')
        val yearString = year.toString()
        val prefix = "$yearString-$monthString-"

        return plansList.filter { it.date.startsWith(prefix) }
            .map { it.date.split("-").last().toInt() }
            .toSet()
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
        val today = Calendar.getInstance()
        displayedMonth = today.get(Calendar.MONTH)
        displayedYear = today.get(Calendar.YEAR)
        
        val utcToday = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, today.get(Calendar.YEAR))
            set(Calendar.MONTH, today.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
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
