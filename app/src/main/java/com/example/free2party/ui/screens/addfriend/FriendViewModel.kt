package com.example.free2party.ui.screens.addfriend

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

class FriendViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    var searchQuery by mutableStateOf("")
    var isSearching by mutableStateOf(false)
    var statusMessage by mutableStateOf("")

    fun findAndAddFriend() {
        val currentEmail = auth.currentUser?.email
        val inputEmail = searchQuery.trim().lowercase()

        if (inputEmail.isBlank()) {
            statusMessage = "Please enter a valid email."
            return
        }

        if (inputEmail == currentEmail) {
            statusMessage = "You cannot add yourself as a friend."
            return
        }

        isSearching = true
        db.collection("users")
            .whereEqualTo("email", inputEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    statusMessage = "User not found."
                    isSearching = false
                } else {
                    val friendDoc = documents.documents[0]
                    val friendUid = friendDoc.id
                    val friendName = friendDoc.getString("displayName") ?: "Friend"

                    addFriendToCollection(friendUid, friendName)
                }
            }
    }

    private fun addFriendToCollection(friendUid: String, friendName: String) {
        val myUid = auth.currentUser?.uid ?: return
        val friendRef =
            db.collection("users").document(myUid)
                .collection("friends").document(friendUid)

        friendRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                statusMessage = "Friend already added."
                isSearching = false
            } else {
                val friendData = hashMapOf(
                    "uid" to friendUid,
                    "displayName" to friendName,
                    "addedAt" to FieldValue.serverTimestamp()
                )

                friendRef.set(friendData).addOnSuccessListener {
                    statusMessage = "Friend \"$friendName\" added successfully."
                    isSearching = false
                    searchQuery = ""
                }
            }
        }.addOnFailureListener {
            statusMessage = "Error checking friend status."
            isSearching = false
        }
    }
}
