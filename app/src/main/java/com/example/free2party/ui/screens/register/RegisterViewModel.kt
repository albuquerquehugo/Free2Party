package com.example.free2party.ui.screens.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class RegisterViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var displayName by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private fun createUserDocumentTask(user: FirebaseUser): Task<Void> {
        val initialData = hashMapOf(
            "uid" to user.uid,
            "email" to user.email,
            "isFreeNow" to false,
            "displayName" to displayName
        )
        return db.collection("users").document(user.uid).set(initialData, SetOptions.merge())
    }

    fun onRegisterClick(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            errorMessage = "All fields are required"
            return
        }

        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .continueWithTask { task ->
                val user = task.result?.user
                if (task.isSuccessful && user != null) {
                    // Chain the Firestore write task
                    createUserDocumentTask(user)
                } else {
                    // If Auth failed, throw the exception to the next block
                    throw task.exception ?: Exception("Auth failed")
                }
            }
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    errorMessage = task.exception?.localizedMessage
                }
            }
    }
}
