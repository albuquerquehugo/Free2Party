package com.example.free2party.data.repository

import com.example.free2party.data.model.FuturePlan
import com.example.free2party.exception.DatabaseOperationException
import com.example.free2party.exception.InvalidPlanDataException
import com.example.free2party.exception.NetworkUnavailableException
import com.example.free2party.exception.OverlappingPlanException
import com.example.free2party.exception.PastDateTimeException
import com.example.free2party.exception.PlanException
import com.example.free2party.exception.PlanNotFoundException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.util.isDateTimeInPast
import com.example.free2party.util.parseDateToMillis
import com.example.free2party.util.parseTimeToMillis
import com.example.free2party.util.parseTimeToMinutes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlanRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : PlanRepository {
    private val currentUserId: String get() = auth.currentUser?.uid ?: ""

    override fun getPlans(userId: String): Flow<List<FuturePlan>> = callbackFlow {
        val targetUserId = userId.ifBlank { currentUserId }
        if (targetUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("users").document(targetUserId)
            .collection("plans")
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToPlanException(error))
                    return@addSnapshotListener
                }
                val plans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FuturePlan::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(plans)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun savePlan(plan: FuturePlan): Result<Unit> = try {
        validateSession()
        validatePlanDateTime(plan)

        val targetUserId = plan.userId.ifBlank { currentUserId }

        // Validation: Check for overlaps
        val existingPlans = fetchPlansSync(targetUserId)
        if (isOverlapping(plan, existingPlans)) {
            throw OverlappingPlanException()
        }

        // Get a new document reference to generate an ID
        val docRef = db.collection("users").document(targetUserId)
            .collection("plans").document()

        // Assign the generated ID to the plan and save it
        val planWithId = plan.copy(id = docRef.id, userId = targetUserId)
        docRef.set(planWithId).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToPlanException(e))
    }

    override suspend fun updatePlan(plan: FuturePlan): Result<Unit> = try {
        validateSession()
        validatePlanDateTime(plan)

        val targetUserId = plan.userId.ifBlank { currentUserId }

        // Validation: Check for overlaps (excluding the current plan being updated)
        val existingPlans = fetchPlansSync(targetUserId).filter { it.id != plan.id }
        if (isOverlapping(plan, existingPlans)) {
            throw OverlappingPlanException()
        }

        val updatedData = mapOf(
            "userId" to targetUserId,
            "startDate" to plan.startDate,
            "endDate" to plan.endDate,
            "startTime" to plan.startTime,
            "endTime" to plan.endTime,
            "note" to plan.note
        )
        db.collection("users").document(targetUserId)
            .collection("plans").document(plan.id)
            .update(updatedData)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToPlanException(e))
    }

    override suspend fun deletePlan(planId: String): Result<Unit> = try {
        validateSession()
        db.collection("users").document(currentUserId)
            .collection("plans").document(planId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToPlanException(e))
    }

    private suspend fun fetchPlansSync(userId: String): List<FuturePlan> {
        val snapshot = db.collection("users").document(userId)
            .collection("plans").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(FuturePlan::class.java)?.copy(id = doc.id)
        }
    }

    private fun validateSession() {
        if (currentUserId.isBlank()) throw UnauthorizedException()
    }

    private fun validatePlanDateTime(plan: FuturePlan) {
        val startDateMillis = parseDateToMillis(plan.startDate)
            ?: throw InvalidPlanDataException()
        val endDateMillis = parseDateToMillis(plan.endDate)
            ?: throw InvalidPlanDataException()

        if (isDateTimeInPast(startDateMillis, plan.startTime)) {
            throw PastDateTimeException()
        }

        if (startDateMillis > endDateMillis) {
            throw InvalidPlanDataException()
        }

        if (startDateMillis == endDateMillis &&
            parseTimeToMinutes(plan.startTime) >= parseTimeToMinutes(plan.endTime)
        ) {
            throw InvalidPlanDataException()
        }
    }

    private fun isOverlapping(newPlan: FuturePlan, existingPlans: List<FuturePlan>): Boolean {
        val startDateMillis = parseDateToMillis(newPlan.startDate) ?: return false
        val endDateMillis = parseDateToMillis(newPlan.endDate) ?: return false
        val startDateTimeMillis = startDateMillis + parseTimeToMillis(newPlan.startTime)
        val endDateTimeMillis = endDateMillis + parseTimeToMillis(newPlan.endTime)

        return existingPlans.any { plan ->
            val existingStartDateMillis = parseDateToMillis(plan.startDate)
                ?: return@any false
            val existingEndDateMillis = parseDateToMillis(plan.endDate)
                ?: return@any false
            val existingStartDateTimeMillis =
                existingStartDateMillis + parseTimeToMillis(plan.startTime)
            val existingEndDateTimeMillis = existingEndDateMillis + parseTimeToMillis(plan.endTime)

            startDateTimeMillis < existingEndDateTimeMillis &&
                    endDateTimeMillis > existingStartDateTimeMillis
        }
    }

    private fun mapToPlanException(e: Throwable): Exception {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.NOT_FOUND -> PlanNotFoundException()
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        UnauthorizedException("You don't have permission to modify this plan")
                    FirebaseFirestoreException.Code.UNAVAILABLE -> NetworkUnavailableException()
                    else -> DatabaseOperationException(e.localizedMessage ?: "Database error")
                }
            }

            is FirebaseNetworkException -> NetworkUnavailableException()
            is PlanException -> e
            else -> DatabaseOperationException(e.localizedMessage ?: "Unknown error")
        }
    }
}
