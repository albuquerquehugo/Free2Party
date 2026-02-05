package com.example.free2party.ui.screens.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.free2party.util.timeToMinutes
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FuturePlan(
    val id: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val note: String = ""
)

class CalendarViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

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
        fetchPlans()
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

    private fun isOverlapping(
        newStartMins: Int,
        newEndMins: Int,
        existingPlans: List<FuturePlan>
    ): Boolean {
        return existingPlans.any { plan ->
            val existingStartMins = timeToMinutes(plan.startTime)
            val existingEndMins = timeToMinutes(plan.endTime)

            newStartMins < existingEndMins && newEndMins > existingStartMins
        }
    }

    fun formatMillisToDateString(millis: Long): String {
        return dateFormatter.format(Date(millis))
    }

    fun validatePlan(
        planId: String?,
        date: Long,
        startTime: String,
        endTime: String,
        onValidationError: (String) -> Unit
    ): Boolean {
        val plansOnDay =
            plansList.filter { it.date == formatMillisToDateString(date) && it.id != planId }
        if (isOverlapping(timeToMinutes(startTime), timeToMinutes(endTime), plansOnDay)) {
            onValidationError("This time slot overlaps with an existing plan")
            return false
        }
        return true
    }

    fun savePlan(
        date: Long,
        startTime: String,
        endTime: String,
        note: String,
        onValidationError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        if (validatePlan(null, date, startTime, endTime, onValidationError)) {
            val plan = hashMapOf(
                "date" to formatMillisToDateString(date),
                "startTime" to startTime,
                "endTime" to endTime,
                "note" to note,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val uid = auth.currentUser?.uid ?: return
            db.collection("users").document(uid)
                .collection("plans")
                .add(plan)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onValidationError("Failed to add the plan in the database.")
                    e.printStackTrace()
                }
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
        if (validatePlan(planId, date, startTime, endTime, onError)) {
            val updatedData = mapOf(
                "date" to formatMillisToDateString(date),
                "startTime" to startTime,
                "endTime" to endTime,
                "note" to note
            )
            val uid = auth.currentUser?.uid ?: return
            val planRef = db.collection("users").document(uid)
                .collection("plans").document(planId)

            planRef.update(updatedData)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError("Failed to update the plan in the database.")
                    e.printStackTrace()
                }
        }
    }

    fun deletePlan(planId: String, onError: (String) -> Unit, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("plans").document(planId).delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Failed to delete the plan in the database.")
                e.printStackTrace()
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
