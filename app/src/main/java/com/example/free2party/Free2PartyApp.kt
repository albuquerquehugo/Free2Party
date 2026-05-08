package com.example.free2party

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.free2party.data.repository.PlanRepository
import com.example.free2party.data.repository.PlanRepositoryImpl
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.exception.UnauthorizedException
import com.example.free2party.exception.UserNotFoundException
import com.example.free2party.data.model.PlanVisibility
import com.example.free2party.util.NotificationHelper
import com.example.free2party.util.isPlanActive
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

@HiltAndroidApp
class Free2PartyApp : Application() {

    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var planRepository: PlanRepository
    private var automationJob: Job? = null
    private var currentAutomationUid: String? = null

    companion object {
        var isAppInForeground: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

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

        userRepository = UserRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore,
            storage = Firebase.storage
        )
        planRepository = PlanRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore
        )

        setupUserStatusAutomation()
        setupForegroundTracking()
        setupFcmTokenSync()
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
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            Firebase.auth.addAuthStateListener { auth ->
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    launch {
                        try {
                            val token = Firebase.messaging.token.await()
                            userRepository.updateFcmToken(token)
                            Log.d("Free2PartyApp", "FCM Token synced for user $uid")
                        } catch (e: Exception) {
                            Log.e("Free2PartyApp", "Failed to sync FCM token", e)
                        }
                    }
                }
            }
        }
    }

    private fun startUserStatusAutomation(uid: String) {
        if (currentAutomationUid == uid && automationJob?.isActive == true) return

        stopUserStatusAutomation()
        currentAutomationUid = uid

        automationJob = ProcessLifecycleOwner.get().lifecycleScope.launch {
            var lastAutomatedStatus: Boolean? = null

            try {
                // Initial sync: check if the stored status is from a plan that's no longer active
                val user = userRepository.getUserById(uid).getOrNull()
                if (user != null) {
                    lastAutomatedStatus = user.isStatusFromPlan
                }

                planRepository.getOwnPlans().collectLatest { plans ->
                    Log.d("Automation", "Plans updated, count: ${plans.size}")
                    while (true) {
                        val activePlans = plans.filter { isPlanActive(it) }
                        val shouldBeFreePublicly =
                            activePlans.any { it.visibility == PlanVisibility.EVERYONE }
                        val hasAnyActivePlan = activePlans.isNotEmpty()

                        if (lastAutomatedStatus != hasAnyActivePlan) {
                            Log.d(
                                "Automation",
                                "Status transition detected. Any plan: $hasAnyActivePlan, Public plan: $shouldBeFreePublicly"
                            )

                            if (lastAutomatedStatus != null || hasAnyActivePlan) {
                                userRepository.toggleAvailability(
                                    shouldBeFreePublicly,
                                    fromPlan = hasAnyActivePlan
                                )
                                    .onFailure { e ->
                                        if (e is UnauthorizedException || e is UserNotFoundException) {
                                            Log.d("Automation", "User no longer valid, stopping.")
                                            stopUserStatusAutomation()
                                            return@collectLatest
                                        }
                                    }
                            }

                            lastAutomatedStatus = hasAnyActivePlan
                        }

                        delay(BuildConfig.updateFrequency)
                    }
                }
            } catch (e: Exception) {
                if (e is UnauthorizedException || e is UserNotFoundException) {
                    Log.d(
                        "Automation",
                        "Unauthorized access or user not found, stopping automation"
                    )
                } else {
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
