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
import com.example.free2party.util.NotificationHelper
import com.example.free2party.util.isPlanActive
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Free2PartyApp : Application() {

    private lateinit var userRepository: UserRepository
    private lateinit var planRepository: PlanRepository
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
                planRepository.getOwnPlans().collectLatest { plans ->
                    Log.d("Automation", "Plans updated, count: ${plans.size}")
                    while (true) {
                        val shouldBeFree = plans.any { isPlanActive(it) }

                        if (lastAutomatedStatus != shouldBeFree) {
                            Log.d(
                                "Automation",
                                "Status transition detected: $lastAutomatedStatus -> $shouldBeFree"
                            )

                            if (lastAutomatedStatus != null || shouldBeFree) {
                                userRepository.toggleAvailability(shouldBeFree)
                                    .onFailure { e ->
                                        if (e is UnauthorizedException || e is UserNotFoundException) {
                                            Log.d("Automation", "User no longer valid, stopping.")
                                            stopUserStatusAutomation()
                                            return@collectLatest
                                        }
                                    }
                            }

                            lastAutomatedStatus = shouldBeFree
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
