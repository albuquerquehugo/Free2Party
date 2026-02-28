package com.example.free2party.ui.screens.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.free2party.data.model.FriendRequest
import com.example.free2party.data.model.FriendRequestStatus
import com.example.free2party.data.repository.PlanRepositoryImpl
import com.example.free2party.data.repository.SocialRepository
import com.example.free2party.data.repository.SocialRepositoryImpl
import com.example.free2party.data.repository.UserRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val socialRepository: SocialRepository = SocialRepositoryImpl(
        db = Firebase.firestore,
        userRepository = UserRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore,
            storage = Firebase.storage
        ),
        planRepository = PlanRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore
        )
    )
) : ViewModel() {
    private var observationJob: Job? = null

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    init {
        // We need a UserRepository to observe the auth state
        val userRepository = UserRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore,
            storage = Firebase.storage
        )
        
        viewModelScope.launch {
            userRepository.userIdFlow.collectLatest { uid ->
                if (uid.isNotBlank()) {
                    listenToIncomingRequests()
                } else {
                    observationJob?.cancel()
                    _friendRequests.value = emptyList()
                }
            }
        }
    }

    private fun listenToIncomingRequests() {
        observationJob?.cancel()
        observationJob = socialRepository.getIncomingFriendRequests()
            .onEach { requests ->
                _friendRequests.value = requests
            }
            .catch { e ->
                Log.e("NotificationsViewModel", "Error observing requests", e)
            }
            .launchIn(viewModelScope)
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            socialRepository.updateFriendRequestStatus(
                request.id,
                FriendRequestStatus.ACCEPTED
            )
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.updateFriendRequestStatus(requestId, FriendRequestStatus.DECLINED)
        }
    }

    // TODO: Update Notifications to show accepted or declined friend requests
}
