package com.example.free2party.data.repository

import com.example.free2party.data.model.FuturePlan
import com.example.free2party.exception.DatabaseOperationException
import com.example.free2party.exception.InvalidPlanDataException
import com.example.free2party.exception.NetworkUnavailableException
import com.example.free2party.exception.OverlappingPlanException
import com.example.free2party.exception.PlanNotFoundException
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.util.timeToMinutes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlanRepositoryImpl(
    private val db: FirebaseFirestore,
    private val currentUserId: String
) : PlanRepository {

    override fun getPlans(): Flow<List<FuturePlan>> = callbackFlow {
        if (currentUserId.isBlank()) {
            close(UnauthorizedException())
            return@callbackFlow
        }

        val listener = db.collection("users").document(currentUserId)
            .collection("plans")
            .orderBy("date", Query.Direction.ASCENDING)
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

        // Validation: Check for overlaps
        val existingPlans = fetchPlansSync()
        if (isOverlapping(plan, existingPlans)) {
            throw OverlappingPlanException()
        }

        // Get a new document reference to generate an ID
        val docRef = db.collection("users").document(currentUserId)
            .collection("plans").document()
        
        // Assign the generated ID to the plan and save it
        val planWithId = plan.copy(id = docRef.id)
        docRef.set(planWithId).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapToPlanException(e))
    }

    override suspend fun updatePlan(plan: FuturePlan): Result<Unit> = try {
        validateSession()

        // Validation: Check for overlaps (excluding the current plan being updated)
        val existingPlans = fetchPlansSync().filter { it.id != plan.id }
        if (isOverlapping(plan, existingPlans)) {
            throw OverlappingPlanException()
        }

        val updatedData = mapOf(
            "date" to plan.date,
            "startTime" to plan.startTime,
            "endTime" to plan.endTime,
            "note" to plan.note
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

    private suspend fun fetchPlansSync(): List<FuturePlan> {
        val snapshot = db.collection("users").document(currentUserId)
            .collection("plans").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(FuturePlan::class.java)?.copy(id = doc.id)
        }
    }

    private fun validateSession() {
        if (currentUserId.isBlank()) throw UnauthorizedException()
    }

    private fun isOverlapping(newPlan: FuturePlan, existingPlans: List<FuturePlan>): Boolean {
        val newStartMins = timeToMinutes(newPlan.startTime)
        val newEndMins = timeToMinutes(newPlan.endTime)

        return existingPlans.filter { it.date == newPlan.date }.any { plan ->
            val existingStartMins = timeToMinutes(plan.startTime)
            val existingEndMins = timeToMinutes(plan.endTime)

            newStartMins < existingEndMins && newEndMins > existingStartMins
        }
    }

    private fun mapToPlanException(e: Exception): Exception {
        return when (e) {
            is FirebaseNetworkException -> NetworkUnavailableException()
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.NOT_FOUND -> PlanNotFoundException()
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> UnauthorizedException("You don't have permission to modify this plan")
                    FirebaseFirestoreException.Code.UNAVAILABLE -> NetworkUnavailableException()
                    else -> DatabaseOperationException(e.localizedMessage ?: "Database error")
                }
            }

            is PlanNotFoundException,
            is UnauthorizedException,
            is OverlappingPlanException,
            is InvalidPlanDataException -> e

            else -> DatabaseOperationException(e.localizedMessage ?: "Unknown error")
        }
    }
}
