package com.example.free2party.ui.screens.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    private var userRepository: UserRepository? = null
    private var socialRepository: SocialRepository? = null
    private var observationJob: Job? = null

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != null) {
            setupRepositories(uid)
            listenToIncomingRequests()
        } else {
            stopListening()
            _friendRequests.value = emptyList()
        }
    }

    init {
        Firebase.auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        Firebase.auth.removeAuthStateListener(authListener)
    }

    private fun setupRepositories(uid: String) {
        val db = Firebase.firestore
        val userRepo = UserRepositoryImpl(db = db, currentUserId = uid)
        userRepository = userRepo
        socialRepository = SocialRepositoryImpl(db = db, userRepository = userRepo)
    }

    private fun listenToIncomingRequests() {
        observationJob?.cancel()
        observationJob = socialRepository?.getIncomingFriendRequests()
            ?.catch { e ->
                Log.e("NotificationsViewModel", "Error observing requests", e)
            }
            ?.onEach { _friendRequests.value = it }
            ?.launchIn(viewModelScope)
    }

    private fun stopListening() {
        observationJob?.cancel()
        observationJob = null
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                socialRepository?.updateFriendRequestStatus(
                    request.id,
                    FriendRequestStatus.ACCEPTED
                )
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Error accepting request", e)
            }
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                socialRepository?.updateFriendRequestStatus(requestId, FriendRequestStatus.DECLINED)
            } catch (e: Exception) {
                Log.e("NotificationsViewModel", "Error declining request", e)
            }
        }
    }
}
