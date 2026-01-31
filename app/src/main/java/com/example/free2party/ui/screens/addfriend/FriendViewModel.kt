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
        if (searchQuery.isBlank()) return

        isSearching = true
        db.collection("users")
            .whereEqualTo("email", searchQuery.trim())
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

        val friendData = hashMapOf(
            "uid" to friendUid,
            "displayName" to friendName,
            "addedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(myUid)
            .collection("friends").document(friendUid)
            .set(friendData)
            .addOnSuccessListener {
                statusMessage = "Friend \"$friendName\" added successfully."
                isSearching = false
                searchQuery = ""
            }
    }
}