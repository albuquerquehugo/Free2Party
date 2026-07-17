package com.free2party.data.billing

import kotlinx.coroutines.flow.StateFlow

interface BillingManager {
    fun initialize()
    val isPremium: StateFlow<Boolean>
    val premiumPackages: StateFlow<List<PremiumPackage>>
    val isSandboxMode: Boolean
    val loadError: StateFlow<String?>

    suspend fun purchasePackage(activity: android.app.Activity, pkg: PremiumPackage): Result<Unit>
    suspend fun restorePurchases(): Result<Boolean>
}

data class PremiumPackage(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val durationType: DurationType
)

enum class DurationType {
    MONTHLY,
    ANNUALLY,
    LIFETIME
}
