package com.free2party.data.repository

import com.free2party.data.model.FuturePlan
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun getOwnPlans(): Flow<List<FuturePlan>>
    fun getPublicPlans(userId: String): Flow<List<FuturePlan>>
    fun getAllPlans(userId: String): Flow<List<FuturePlan>>
    suspend fun savePlan(plan: FuturePlan): Result<Unit>
    suspend fun updatePlan(plan: FuturePlan): Result<Unit>
    suspend fun deletePlan(planId: String): Result<Unit>
}
