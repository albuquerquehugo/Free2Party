package com.example.free2party.data.repository

import com.example.free2party.data.model.FuturePlan
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun getPlans(): Flow<List<FuturePlan>>
    suspend fun savePlan(plan: FuturePlan): Result<Unit>
    suspend fun updatePlan(plan: FuturePlan): Result<Unit>
    suspend fun deletePlan(planId: String): Result<Unit>
}
