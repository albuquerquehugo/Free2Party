package com.free2party

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.free2party.data.billing.BillingManager
import com.free2party.data.model.PlanVisibility
import com.free2party.data.repository.PlanRepository
import com.free2party.data.repository.UserRepository
import com.free2party.exception.UnauthorizedException
import com.free2party.exception.UserNotFoundException
import com.free2party.util.NotificationHelper
import com.free2party.util.isPlanActive
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.storage
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.gms.tasks.Tasks
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.milliseconds

@HiltAndroidApp
class Free2PartyApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var planRepository: PlanRepository
    @Inject
    lateinit var billingManager: BillingManager
    private var automationJob: Job? = null
    private var currentAutomationUid: String? = null

    companion object {
        var isAppInForeground: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        
        billingManager.initialize()

        val useEmulators = false

        if (BuildConfig.DEBUG && useEmulators) {
            val computerIp = BuildConfig.COMPUTER_IP
            Log.d("Free2PartyApp", "Connecting to Firebase Emulators at: $computerIp")
            Firebase.auth.useEmulator(computerIp, 9099)
            Firebase.firestore.useEmulator(computerIp, 8080)
            Firebase.storage.useEmulator(computerIp, 9199)
        }

        // Initialize Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        NotificationHelper.createNotificationChannel(this)

        MobileAds.initialize(this) {}

        setupUserStatusAutomation()
        setupForegroundTracking()
        setupFcmTokenSync()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        val url = request.url.toString()
                        if (url.contains("firebasestorage.googleapis.com")) {
                            val user = Firebase.auth.currentUser
                            if (user != null) {
                                val tokenTask = user.getIdToken(false)
                                val token = runCatching { Tasks.await(tokenTask).token }.getOrNull()
                                if (!token.isNullOrBlank()) {
                                    val newRequest = request.newBuilder()
                                        .header("Authorization", "Bearer $token")
                                        .build()
                                    return@addInterceptor chain.proceed(newRequest)
                                }
                            }
                        }
                        chain.proceed(request)
                    }
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private fun setupForegroundTracking() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppInForeground = true
                Log.d("Free2PartyApp", "App in foreground")
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppInForeground = false
                Log.d("Free2PartyApp", "App in background")
            }
        })
    }

    private fun setupUserStatusAutomation() {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            Firebase.auth.addAuthStateListener { auth ->
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    startUserStatusAutomation(uid)
                } else {
                    stopUserStatusAutomation()
                }
            }
        }
    }

    private fun setupFcmTokenSync() {
        ProcessLifecycleOwner.get().lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            Firebase.auth.addAuthStateListener { auth ->
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    launch {
                        try {
                            Firebase.messaging.register().await()
                            Log.d("Free2PartyApp", "FCM Registration triggered for user $uid")
                        } catch (e: Exception) {
                            Log.e("Free2PartyApp", "Failed to trigger FCM registration", e)
                        }
                    }
                }
            }
        }
    }

    private fun startUserStatusAutomation(uid: String) {
        if (currentAutomationUid == uid && automationJob?.isActive == true) {
            return
        }

        stopUserStatusAutomation()
        currentAutomationUid = uid

        automationJob = ProcessLifecycleOwner.get().lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            var lastAutomatedStatus: Boolean? = null

            try {
                // Initial sync: check if the stored status is from a plan that's no longer active
                val user = userRepository.getUserById(uid).getOrNull()
                if (user != null) {
                    lastAutomatedStatus = user.isStatusFromPlan
                }

                planRepository.getOwnPlans().collectLatest { plans ->
                    while (true) {
                        val activePlans = plans.filter { isPlanActive(it) }
                        val hasAnyActivePlan = activePlans.isNotEmpty()
                        val shouldBeFreePublicly =
                            activePlans.any { it.visibility == PlanVisibility.EVERYONE }

                        if (lastAutomatedStatus != hasAnyActivePlan) {
                            userRepository.toggleAvailability(
                                shouldBeFreePublicly,
                                fromPlan = hasAnyActivePlan
                            ).onFailure { e ->
                                Log.e("Automation", "Failed to toggle availability", e)
                                if (e is UnauthorizedException || e is UserNotFoundException) {
                                    stopUserStatusAutomation()
                                    return@collectLatest
                                }
                            }

                            lastAutomatedStatus = hasAnyActivePlan
                        }

                        // Ensure frequency is at least 1s to avoid tight loops on older devices or bugged configs
                        val safeDelay = maxOf(BuildConfig.updateFrequency, 1000L)
                        delay(safeDelay.milliseconds)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
                if (e !is UnauthorizedException && e !is UserNotFoundException) {
                    Log.e("Automation", "Error in automation loop", e)
                }
                stopUserStatusAutomation()
            }
        }
    }

    private fun stopUserStatusAutomation() {
        automationJob?.cancel()
        automationJob = null
        currentAutomationUid = null
    }
}
