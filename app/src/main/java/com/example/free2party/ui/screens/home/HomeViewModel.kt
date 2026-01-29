package com.example.free2party.ui.screens.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.free2party.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class HomeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // State to track if the current user is free
    var isUserFree by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    init {
        if (BuildConfig.DEBUG) {
            db.useEmulator("10.0.2.2", 8080)
            auth.useEmulator("10.0.2.2", 9099)
        }
        fetchCurrentStatus()
    }

    private fun fetchCurrentStatus() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error fetching status", error)
                    return@addSnapshotListener
                }
                isUserFree = snapshot?.getBoolean("isFreeNow") ?: false
            }
    }

    fun toggleAvailability() {
        val uid = auth.currentUser?.uid ?: return
        isLoading = true

        val data = mapOf("isFreeNow" to !isUserFree)

        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .addOnCompleteListener {
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("HomeViewModel", "Error updating availability", e)
            }
    }
}
