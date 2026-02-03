package com.example.free2party.ui.screens.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.util.Calendar
import java.util.TimeZone

data class FuturePlan(
    val id: String = "",
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val note: String = ""
)

class CalendarViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    var plansList by mutableStateOf<List<FuturePlan>>(emptyList())
    val filteredPlans: List<FuturePlan>
        get() {
            val selectedDate = selectedDateMillis ?: return emptyList()
            return plansList.filter { isSameDate(it.date, selectedDate) }
        }

    var displayedMonth by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var displayedYear by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))

    var selectedDateMillis by mutableStateOf<Long?>(null)

    init {
        fetchPlans()
    }

    private fun isSameDate(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = date2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun fetchPlans() {
        val uid = auth.currentUser?.uid ?: return

        // Listen for changes and sort the scheduled date
        db.collection("users").document(uid)
            .collection("plans")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val plans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FuturePlan::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                plansList = plans
            }
    }

    fun savePlan(date: Long, startTime: String, endTime: String, note: String) {
        val uid = auth.currentUser?.uid ?: return

        val plan = hashMapOf(
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "note" to note,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(uid)
            .collection("plans")
            .add(plan)
    }

    fun deletePlan(planId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("plans").document(planId).delete()
    }

    fun getPlannedDaysForMonth(year: Int, month: Int): Set<Int> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return plansList.filter { plan ->
            calendar.timeInMillis = plan.date
            calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month
        }.map {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.DAY_OF_MONTH)
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
        val today = Calendar.getInstance()
        displayedMonth = today.get(Calendar.MONTH)
        displayedYear = today.get(Calendar.YEAR)
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

    fun clearSelectedDate() {
        selectedDateMillis = null
    }
}
