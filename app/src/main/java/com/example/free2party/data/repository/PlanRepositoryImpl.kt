package com.example.free2party.data.repository

import com.example.free2party.data.model.FuturePlan
import com.example.free2party.data.model.PlanVisibility
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

    override fun getOwnPlans(): Flow<List<FuturePlan>> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("users").document(currentUserId)
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

    override fun getPublicPlans(userId: String): Flow<List<FuturePlan>> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection("plans")
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToPlanException(error))
                    return@addSnapshotListener
                }
                val allPlans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FuturePlan::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Only return plans that are visible to the current user
                val visiblePlans = allPlans.filter { plan ->
                    when (plan.visibility) {
                        PlanVisibility.EVERYONE -> true
                        PlanVisibility.EXCEPT -> currentUserId !in plan.friendsSelection
                        PlanVisibility.ONLY -> currentUserId in plan.friendsSelection
                    }
                }
                trySend(visiblePlans)
            }
        awaitClose { listener.remove() }
    }

    override fun getAllPlans(userId: String): Flow<List<FuturePlan>> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .collection("plans")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapToPlanException(error))
                    return@addSnapshotListener
                }
                val allPlans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FuturePlan::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(allPlans)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun savePlan(plan: FuturePlan): Result<Unit> = try {
        validateSession()
        validatePlanDateTime(plan)

        val existingPlans = fetchPlansSync(currentUserId)
        if (isOverlapping(plan, existingPlans)) {
            throw OverlappingPlanException()
        }

        val docRef = db.collection("users").document(currentUserId)
            .collection("plans").document()

        val planWithId = plan.copy(id = docRef.id, userId = currentUserId)
        docRef.set(planWithId).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToPlanException(e))
    }

    override suspend fun updatePlan(plan: FuturePlan): Result<Unit> = try {
        validateSession()
        validatePlanDateTime(plan)

        val existingPlans = fetchPlansSync(currentUserId).filter { it.id != plan.id }
        if (isOverlapping(plan, existingPlans)) {
            throw OverlappingPlanException()
        }

        val updatedData = mapOf(
            "userId" to currentUserId,
            "startDate" to plan.startDate,
            "endDate" to plan.endDate,
            "startTime" to plan.startTime,
            "endTime" to plan.endTime,
            "note" to plan.note,
            "visibility" to plan.visibility.name,
            "friendsSelection" to plan.friendsSelection
        )
        db.collection("users").document(currentUserId)
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

        val startTimeMinutes = parseTimeToMinutes(plan.startTime)
            ?: throw InvalidPlanDataException()
        val endTimeMinutes = parseTimeToMinutes(plan.endTime)
            ?: throw InvalidPlanDataException()

        if (startDateMillis == endDateMillis && startTimeMinutes >= endTimeMinutes) {
            throw InvalidPlanDataException()
        }
    }

    private fun isOverlapping(newPlan: FuturePlan, existingPlans: List<FuturePlan>): Boolean {
        val startDateMillis = parseDateToMillis(newPlan.startDate) ?: return false
        val endDateMillis = parseDateToMillis(newPlan.endDate) ?: return false
        val startTimeMillis = parseTimeToMillis(newPlan.startTime) ?: return false
        val endTimeMillis = parseTimeToMillis(newPlan.endTime) ?: return false

        val startDateTimeMillis = startDateMillis + startTimeMillis
        val endDateTimeMillis = endDateMillis + endTimeMillis

        return existingPlans.any { plan ->
            val existingStartDateMillis = parseDateToMillis(plan.startDate)
                ?: return@any false
            val existingEndDateMillis = parseDateToMillis(plan.endDate)
                ?: return@any false
            val existingStartTimeMillis = parseTimeToMillis(plan.startTime)
                ?: return@any false
            val existingEndTimeMillis = parseTimeToMillis(plan.endTime)
                ?: return@any false

            val existingStartDateTimeMillis = existingStartDateMillis + existingStartTimeMillis
            val existingEndDateTimeMillis = existingEndDateMillis + existingEndTimeMillis

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
