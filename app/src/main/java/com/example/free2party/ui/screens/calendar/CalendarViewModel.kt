package com.example.free2party.ui.screens.calendar

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.R
import com.example.free2party.data.model.DatePattern
import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.model.PlanVisibility
import com.example.free2party.data.model.FriendInfo
import com.example.free2party.data.repository.PlanRepository
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.exception.InvalidPlanDataException
import com.example.free2party.exception.OverlappingPlanException
import com.example.free2party.exception.PastDateTimeException
import com.example.free2party.util.UiText
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMillis
import com.example.free2party.util.parseTimeToMinutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.free2party.data.model.Circle
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.TimeZone

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val userRepository: UserRepository,
    socialRepository: SocialRepository
) : ViewModel() {

    private var targetUserId = MutableStateFlow<String?>(null)
    private val currentUserId: String get() = userRepository.currentUserId

    fun setTargetUser(uid: String?) {
        targetUserId.value = uid
    }

    var plansList by mutableStateOf<List<FuturePlan>>(emptyList())
    val filteredPlans: List<FuturePlan>
        get() {
            val selectedDate = selectedDateMillis ?: return emptyList()

            return plansList.filter { plan ->
                val planStartMillis = parseDateToMillis(plan.startDate) ?: return@filter false
                val planEndMillis = parseDateToMillis(plan.endDate) ?: return@filter false

                val planDateTimeStart = planStartMillis + (parseTimeToMillis(plan.startTime) ?: 0L)
                val planDateTimeEnd = planEndMillis + (parseTimeToMillis(plan.endTime) ?: 0L)

                val nextDay = selectedDate + 86400000L // 24 hours later

                // Overlap: max(start1, start2) < min(end1, end2)
                planDateTimeStart.coerceAtLeast(selectedDate) < planDateTimeEnd.coerceAtMost(nextDay)
            }.sortedBy { plan -> parseTimeToMinutes(plan.startTime) ?: 0 }
        }

    var displayedMonth by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var displayedYear by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))

    var selectedDateMillis by mutableStateOf<Long?>(null)

    var use24HourFormat by mutableStateOf(true)
        private set

    var gradientBackground by mutableStateOf(true)
        private set

    var datePattern by mutableStateOf(DatePattern.YYYY_MM_DD)
        private set

    private val userIdToObserve get() = targetUserId.value ?: currentUserId

    val circles: StateFlow<List<Circle>> = socialRepository.getCircles()
        .catch { e -> Log.e("CalendarViewModel", "Error in circles flow", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsList: StateFlow<List<FriendInfo>> = socialRepository.getFriendsList()
        .catch { e -> Log.e("CalendarViewModel", "Error in friendsList flow", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        goToToday()
        observeUserSettings()
        observePlans()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeUserSettings() {
        userRepository.userIdFlow
            .flatMapLatest { uid ->
                if (uid.isBlank()) emptyFlow()
                else userRepository.observeUser(uid)
            }
            .onEach { user ->
                gradientBackground = user.settings.gradientBackground
                use24HourFormat = user.settings.use24HourFormat
                datePattern = user.settings.datePattern
            }
            .catch { e -> Log.e("CalendarViewModel", "Error observing user settings", e) }
            .launchIn(viewModelScope)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observePlans() {
        combine(userRepository.userIdFlow, targetUserId) { currentUid, targetUid ->
            currentUid to targetUid
        }
            .flatMapLatest { (currentUid, targetUid) ->
                val uid = targetUid ?: currentUid
                if (uid.isBlank()) {
                    plansList = emptyList()
                    emptyFlow()
                } else {
                    val isOwn = targetUid == null || targetUid == currentUid
                    if (isOwn) {
                        planRepository.getOwnPlans()
                    } else {
                        planRepository.getPublicPlans(uid)
                    }
                }
            }
            .onEach { plansList = it }
            .catch { e ->
                Log.e("CalendarViewModel", "Error observing plans", e)
                plansList = emptyList()
            }
            .launchIn(viewModelScope)
    }

    fun savePlan(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        note: String,
        visibility: PlanVisibility,
        friendsSelection: List<String>,
        onValidationError: (UiText) -> Unit,
        onSuccess: () -> Unit
    ) {
        val plan = FuturePlan(
            userId = userIdToObserve,
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            note = note.trim(),
            visibility = visibility,
            friendsSelection = friendsSelection
        )
        viewModelScope.launch {
            planRepository.savePlan(plan)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    val message = when (error) {
                        is OverlappingPlanException -> UiText.StringResource(R.string.error_overlapping_plan)
                        is PastDateTimeException -> UiText.StringResource(R.string.error_past_date_time)
                        is InvalidPlanDataException -> UiText.StringResource(R.string.error_invalid_plan_data)
                        else -> UiText.StringResource(R.string.error_failed_save_plan)
                    }
                    onValidationError(message)
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
        visibility: PlanVisibility,
        friendsSelection: List<String>,
        onError: (UiText) -> Unit,
        onSuccess: () -> Unit
    ) {
        val updatedPlan = FuturePlan(
            id = planId,
            userId = userIdToObserve,
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            note = note.trim(),
            visibility = visibility,
            friendsSelection = friendsSelection
        )
        viewModelScope.launch {
            planRepository.updatePlan(updatedPlan)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    val message = when (error) {
                        is OverlappingPlanException -> UiText.StringResource(R.string.error_overlapping_plan)
                        is PastDateTimeException -> UiText.StringResource(R.string.error_past_date_time)
                        is InvalidPlanDataException -> UiText.StringResource(R.string.error_invalid_plan_data)
                        else -> UiText.StringResource(R.string.error_failed_update_plan)
                    }
                    onError(message)
                }
        }
    }

    fun deletePlan(planId: String, onError: (UiText) -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            planRepository.deletePlan(planId)
                .onSuccess { onSuccess() }
                .onFailure { _ -> onError(UiText.StringResource(R.string.error_failed_delete_plan)) }
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
