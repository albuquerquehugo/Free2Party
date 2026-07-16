package com.free2party.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.free2party.BuildConfig
import com.free2party.data.model.Membership
import com.free2party.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class BillingManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : BillingManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Configuration flags
    private val revenueCatApiKey = BuildConfig.REVENUECAT_API_KEY
    private val isSimulated = revenueCatApiKey.isBlank()
    override val isSandboxMode: Boolean = isSimulated

    private val _premiumPackages = MutableStateFlow<List<PremiumPackage>>(emptyList())
    override val premiumPackages: StateFlow<List<PremiumPackage>> = _premiumPackages

    private val _loadError = MutableStateFlow<String?>(null)
    override val loadError: StateFlow<String?> = _loadError

    // Listen to Firebase auth states to observe the current user's membership state in Firestore.
    override val isPremium: StateFlow<Boolean> = callbackFlow {
        var firestoreListenerRegistration: com.google.firebase.firestore.ListenerRegistration? =
            null

        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            firestoreListenerRegistration?.remove()
            firestoreListenerRegistration = null

            if (!uid.isNullOrBlank()) {
                val docRef = db.collection("users").document(uid)
                firestoreListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("BillingManager", "Error listening to user membership", error)
                        trySend(false)
                        return@addSnapshotListener
                    }
                    val membershipStr = snapshot?.getString("membership")
                    val hasPremium = (membershipStr == Membership.PREMIUM.name)
                    trySend(hasPremium)
                }
            } else {
                trySend(false)
            }
        }

        auth.addAuthStateListener(authStateListener)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
            firestoreListenerRegistration?.remove()
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    override fun initialize() {
        if (isSimulated) {
            Log.d("BillingManager", "Initializing BillingManager in SIMULATED/SANDBOX mode")
            loadSimulatedPackages()
        } else {
            Log.d(
                "BillingManager",
                "Initializing BillingManager in PRODUCTION mode with RevenueCat"
            )
            try {
                Purchases.configure(
                    PurchasesConfiguration.Builder(context, revenueCatApiKey).build()
                )

                // Track customer info updates
                Purchases.sharedInstance.updatedCustomerInfoListener =
                    UpdatedCustomerInfoListener { customerInfo ->
                        syncRevenueCatEntitlements(customerInfo)
                    }

                loadProductionPackages()
            } catch (e: Exception) {
                Log.e(
                    "BillingManager",
                    "Failed to configure RevenueCat.",
                    e
                )
                _loadError.value = e.message ?: "Failed to initialize billing services"
            }
        }
    }

    override suspend fun purchasePackage(activity: Activity, pkg: PremiumPackage): Result<Unit> {
        if (isSimulated) {
            Log.d("BillingManager", "Processing simulated purchase of: ${pkg.title}")
            kotlinx.coroutines.delay(1000.milliseconds) // Simulate network latency
            return updateMembershipInFirestore(Membership.PREMIUM)
        }

        return try {
            val offerings = fetchOfferingsSuspending()

            // Find the RevenueCat package that matches our package ID
            val rcPackage = offerings.all.values.flatMap { it.availablePackages }
                .find { it.identifier == pkg.id }
                ?: return Result.failure(Exception("Package not found in active offerings"))

            val customerInfo = purchasePackageSuspending(activity, rcPackage)
            syncRevenueCatEntitlements(customerInfo)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BillingManager", "Purchase failed", e)
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        if (isSimulated) {
            Log.d("BillingManager", "Processing simulated restore purchases")
            kotlinx.coroutines.delay(500.milliseconds)
            return updateMembershipInFirestore(Membership.PREMIUM)
        }

        return try {
            val customerInfo = restorePurchasesSuspending()
            syncRevenueCatEntitlements(customerInfo)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BillingManager", "Restore purchases failed", e)
            Result.failure(e)
        }
    }

    private fun loadSimulatedPackages() {
        _premiumPackages.value = listOf(
            PremiumPackage(
                id = "f2p_premium_monthly",
                title = context.getString(R.string.label_package_monthly),
                description = context.getString(R.string.text_package_monthly),
                price = "$2.99",
                durationType = DurationType.MONTHLY
            ),
            PremiumPackage(
                id = "f2p_premium_yearly",
                title = context.getString(R.string.label_package_yearly),
                description = context.getString(R.string.text_package_yearly),
                price = "$19.99",
                durationType = DurationType.ANNUALLY
            ),
            PremiumPackage(
                id = "f2p_premium_lifetime",
                title = context.getString(R.string.label_package_lifetime),
                description = context.getString(R.string.text_package_lifetime),
                price = "$49.99",
                durationType = DurationType.LIFETIME
            )
        )
    }

    private fun loadProductionPackages() {
        scope.launch {
            try {
                val offerings = fetchOfferingsSuspending()
                val currentOffering = offerings.current
                if (currentOffering != null) {
                    val packages = currentOffering.availablePackages.map { rcPkg ->
                        val type = when (rcPkg.packageType) {
                            PackageType.MONTHLY -> DurationType.MONTHLY
                            PackageType.ANNUAL -> DurationType.ANNUALLY
                            PackageType.LIFETIME -> DurationType.LIFETIME
                            else -> DurationType.MONTHLY
                        }
                        val product = rcPkg.product
                        PremiumPackage(
                            id = rcPkg.identifier,
                            title = product.title.substringBefore("(").trim(),
                            description = product.description,
                            price = product.price.formatted,
                            durationType = type
                        )
                    }
                    _premiumPackages.value = packages
                    _loadError.value = null // Clear any previous errors on successful load
                } else {
                    Log.e(
                        "BillingManager",
                        "No current offering configured in RevenueCat."
                    )
                    _loadError.value = "No active subscription products configured in store."
                }
            } catch (e: Exception) {
                Log.e(
                    "BillingManager",
                    "Error fetching offerings: ${e.message}"
                )
                _loadError.value = e.message ?: "Failed to load subscription plans from store."
            }
        }
    }

    private fun syncRevenueCatEntitlements(customerInfo: CustomerInfo) {
        val hasPremium = customerInfo.entitlements["premium_access"]?.isActive == true
        scope.launch {
            if (hasPremium) {
                updateMembershipInFirestore(Membership.PREMIUM)
            } else {
                updateMembershipInFirestore(Membership.FREE)
            }
        }
    }

    private suspend fun updateMembershipInFirestore(membership: Membership): Result<Unit> {
        val uid =
            auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        return try {
            db.collection("users").document(uid)
                .set(mapOf("membership" to membership.name), SetOptions.merge())
                .await()
            Log.d(
                "BillingManager",
                "Successfully synced membership $membership to Firestore for user $uid"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BillingManager", "Failed to sync membership state to Firestore", e)
            Result.failure(e)
        }
    }

    // Callback to Coroutine suspending bridge helpers
    private suspend fun fetchOfferingsSuspending(): com.revenuecat.purchases.Offerings {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getOfferings(object :
                com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback {
                override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                    continuation.resumeWith(Result.success(offerings))
                }

                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    continuation.resumeWith(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    private suspend fun purchasePackageSuspending(
        activity: Activity,
        rcPackage: com.revenuecat.purchases.Package
    ): CustomerInfo {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val purchaseParams =
                com.revenuecat.purchases.PurchaseParams.Builder(activity, rcPackage).build()
            Purchases.sharedInstance.purchase(
                purchaseParams,
                object : com.revenuecat.purchases.interfaces.PurchaseCallback {
                    override fun onCompleted(
                        storeTransaction: com.revenuecat.purchases.models.StoreTransaction,
                        customerInfo: CustomerInfo
                    ) {
                        continuation.resumeWith(Result.success(customerInfo))
                    }

                    override fun onError(
                        error: com.revenuecat.purchases.PurchasesError,
                        userCancelled: Boolean
                    ) {
                        val msg = if (userCancelled) "Purchase cancelled by user" else error.message
                        continuation.resumeWith(Result.failure(Exception(msg)))
                    }
                }
            )
        }
    }

    private suspend fun restorePurchasesSuspending(): CustomerInfo {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchases(object :
                com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    continuation.resumeWith(Result.success(customerInfo))
                }

                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    continuation.resumeWith(Result.failure(Exception(error.message)))
                }
            })
        }
    }
}
