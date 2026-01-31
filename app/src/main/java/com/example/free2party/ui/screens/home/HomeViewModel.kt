package com.example.free2party.ui.screens.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

data class FriendStatus(
    val uid: String,
    val name: String,
    val isFree: Boolean
)

class HomeViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    var isUserFree by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var friendsStatusList by mutableStateOf<List<FriendStatus>>(emptyList())

    init {
        fetchCurrentStatus()
        fetchFriendsAndListen()
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        auth.signOut()
        onLogoutSuccess()
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

    private fun fetchFriendsAndListen() {
        val myUid = auth.currentUser?.uid ?: return

        db.collection("users").document(myUid)
            .collection("friends")
            .addSnapshotListener { snapshot, _ ->
                val friendIds =
                    snapshot?.documents?.map { it.id }?.filter { it != myUid } ?: emptyList()

                friendIds.forEach { friendUid ->
                    db.collection("users").document(friendUid)
                        .addSnapshotListener { friendSnapshot, _ ->
                            if (friendSnapshot != null) {
                                val updatedFriend = FriendStatus(
                                    uid = friendUid,
                                    name = friendSnapshot.getString("displayName") ?: "Unknown",
                                    isFree = friendSnapshot.getBoolean("isFreeNow") ?: false
                                )
                                updateFriendInList(updatedFriend)
                            }
                        }
                }
            }
    }

    private fun updateFriendInList(updatedFriend: FriendStatus) {
        val newList = friendsStatusList.toMutableList()
        val index = newList.indexOfFirst { it.uid == updatedFriend.uid }
        if (index != -1) {
            newList[index] = updatedFriend
        } else {
            newList.add(updatedFriend)
        }
        friendsStatusList = newList.sortedByDescending { it.isFree }
    }
}
