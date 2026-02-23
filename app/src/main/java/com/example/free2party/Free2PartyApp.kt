package com.example.free2party

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.free2party.data.repository.PlanRepository
import com.example.free2party.data.repository.PlanRepositoryImpl
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.util.isPlanActive
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Free2PartyApp : Application() {

    private lateinit var userRepository: UserRepository
    private lateinit var planRepository: PlanRepository
    private var automationJob: Job? = null
    private var currentAutomationUid: String? = null

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // Point Firebase to the local emulators
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.firestore.useEmulator("10.0.2.2", 8080)
            Firebase.storage.useEmulator("10.0.2.2", 9199)
        }

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

    private fun startUserStatusAutomation(uid: String) {
        if (currentAutomationUid == uid && automationJob?.isActive == true) return
        
        stopUserStatusAutomation()
        currentAutomationUid = uid
        
        automationJob = ProcessLifecycleOwner.get().lifecycleScope.launch {
            var lastAutomatedStatus: Boolean? = null

            planRepository.getPlans(uid).collectLatest { plans ->
                Log.d("Automation", "Plans updated, count: ${plans.size}")
                while (true) {
                    val shouldBeFree = plans.any { isPlanActive(it) }
                    
                    if (lastAutomatedStatus != shouldBeFree) {
                        Log.d("Automation", "Status transition detected: $lastAutomatedStatus -> $shouldBeFree")
                        
                        // If this is the first run, we don't want to force an update immediately
                        // unless we are sure we are entering a plan.
                        // Actually, if lastAutomatedStatus is null, it means the app just started.
                        // We set it but only toggle if it's true (entering a plan) 
                        // or if we were already running and it changed.
                        
                        if (lastAutomatedStatus != null || shouldBeFree) {
                            userRepository.toggleAvailability(shouldBeFree)
                        }
                        
                        lastAutomatedStatus = shouldBeFree
                    }

                    delay(BuildConfig.updateFrequency)
                }
            }
        }
    }

    private fun stopUserStatusAutomation() {
        automationJob?.cancel()
        automationJob = null
        currentAutomationUid = null
    }
}
