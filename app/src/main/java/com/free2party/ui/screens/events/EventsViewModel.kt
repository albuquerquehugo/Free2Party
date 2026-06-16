package com.free2party.ui.screens.events

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.R
import com.free2party.data.model.*
import com.free2party.data.repository.EventRepository
import com.free2party.data.repository.SocialRepository
import com.free2party.data.repository.UserRepository
import com.free2party.exception.EventException
import com.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EventsUiState {
    object Loading : EventsUiState
    data class Success(
        val myEvents: List<Event> = emptyList(),
        val pendingEvents: List<Event> = emptyList()
    ) : EventsUiState

    data class Error(val message: UiText) : EventsUiState
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    socialRepository: SocialRepository
) : ViewModel() {

    val currentUserId: String get() = userRepository.currentUserId

    var use24HourFormat by mutableStateOf(true)
        private set
    var gradientBackground by mutableStateOf(true)
        private set
    var membership by mutableStateOf(Membership.REGULAR)
        private set
    var selectedTabIndex by mutableIntStateOf(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<EventsUiState> = userRepository.userIdFlow
        .flatMapLatest { uid ->
            if (uid.isBlank()) {
                flowOf(EventsUiState.Error(UiText.StringResource(R.string.error_unauthorized)))
            } else {
                eventRepository.getEvents()
                    .map<List<Event>, EventsUiState> { events ->
                        val my = events.filter { it.hostId == uid }
                        val pending = events.filter { it.hostId != uid }
                        EventsUiState.Success(myEvents = my, pendingEvents = pending)
                    }
                    .catch { e ->
                        Log.e("EventsViewModel", "Error inside events flow", e)
                        emit(EventsUiState.Error(UiText.StringResource(R.string.error_unknown)))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventsUiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pendingInvitationsCount: StateFlow<Int> = userRepository.userIdFlow
        .flatMapLatest { uid ->
            if (uid.isBlank()) {
                flowOf(0)
            } else {
                eventRepository.getEvents()
                    .map { events ->
                        events.count { event ->
                            event.hostId != uid &&
                                    event.guestIds.contains(uid) &&
                                    (event.guests[uid]
                                        ?: GuestStatus.PENDING.name) == GuestStatus.PENDING.name
                        }
                    }
                    .catch { e ->
                        Log.e("EventsViewModel", "Error inside pending invitations flow", e)
                        emit(0)
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val circles: StateFlow<List<Circle>> = socialRepository.getCircles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsList: StateFlow<List<FriendInfo>> = socialRepository.getFriendsList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeUserSettings()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUserSettings() {
        userRepository.userIdFlow
            .flatMapLatest { uid ->
                if (uid.isBlank()) emptyFlow()
                else userRepository.observeUser(uid)
            }
            .onEach { user ->
                gradientBackground = user.settings.gradientBackground
                use24HourFormat = user.settings.use24HourFormat
                membership = user.membership
            }
            .catch { e -> Log.e("EventsViewModel", "Error observing user settings", e) }
            .launchIn(viewModelScope)
    }

    // Detail states (observed when detailed screen is active)
    private val _currentEventId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentEvent: StateFlow<Event?> = _currentEventId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else eventRepository.getEventDetails(id)
                .map<Event, Event?> { it }
                .catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventGuests: StateFlow<Map<String, User>> = currentEvent
        .flatMapLatest { event ->
            if (event == null || event.guestIds.isEmpty()) {
                flowOf(emptyMap())
            } else {
                flow {
                    val guestsMap = mutableMapOf<String, User>()
                    event.guestIds.forEach { uid ->
                        userRepository.getUserById(uid).getOrNull()?.let { user ->
                            guestsMap[uid] = user
                        }
                    }
                    emit(guestsMap)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    @OptIn(ExperimentalCoroutinesApi::class)
    val comments: StateFlow<List<EventComment>> = _currentEventId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else eventRepository.getComments(id).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val photos: StateFlow<List<EventPhoto>> = _currentEventId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else eventRepository.getPhotos(id).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectEvent(eventId: String?) {
        _currentEventId.value = eventId
    }

    fun saveEvent(
        title: String,
        description: String,
        type: EventType,
        startDate: String,
        startTime: String,
        endDate: String,
        endTime: String,
        timezone: String,
        locationName: String,
        latitude: Double?,
        longitude: Double?,
        guests: Map<String, String>,
        usefulLinks: List<EventLink>,
        onSuccess: (String) -> Unit,
        onError: (UiText) -> Unit
    ) {
        viewModelScope.launch {
            // Fetch current user details to cache host info on event
            val user = userRepository.getUserById(currentUserId).getOrNull()
            val hostName = user?.fullName ?: ""
            val hostProfilePic = user?.profilePicUrl ?: ""
            val hostEmail = user?.email ?: ""

            val event = Event(
                title = title.trim(),
                description = description.trim(),
                type = type,
                startDate = startDate,
                startTime = startTime,
                endDate = endDate,
                endTime = endTime,
                timezone = timezone,
                locationName = locationName.trim(),
                latitude = latitude,
                longitude = longitude,
                guests = guests,
                usefulLinks = usefulLinks,
                hostName = hostName,
                hostProfilePic = hostProfilePic,
                hostEmail = hostEmail
            )

            eventRepository.saveEvent(event)
                .onSuccess { eventId -> onSuccess(eventId) }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error saving event", error)
                    val errorMsg = when (error) {
                        is EventException -> {
                            if (error.messageRes != null) {
                                UiText.StringResource(error.messageRes)
                            } else {
                                UiText.DynamicString(error.message ?: "")
                            }
                        }
                        else -> UiText.StringResource(R.string.error_database_operation)
                    }
                    onError(errorMsg)
                }
        }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        type: EventType,
        startDate: String,
        startTime: String,
        endDate: String,
        endTime: String,
        timezone: String,
        locationName: String,
        latitude: Double?,
        longitude: Double?,
        guests: Map<String, String>,
        usefulLinks: List<EventLink>,
        onSuccess: () -> Unit,
        onError: (UiText) -> Unit
    ) {
        viewModelScope.launch {
            val event = Event(
                id = eventId,
                title = title.trim(),
                description = description.trim(),
                type = type,
                startDate = startDate,
                startTime = startTime,
                endDate = endDate,
                endTime = endTime,
                timezone = timezone,
                locationName = locationName.trim(),
                latitude = latitude,
                longitude = longitude,
                guests = guests,
                usefulLinks = usefulLinks
            )

            eventRepository.updateEvent(event)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error updating event", error)
                    val errorMsg = when (error) {
                        is EventException -> {
                            if (error.messageRes != null) {
                                UiText.StringResource(error.messageRes)
                            } else {
                                UiText.DynamicString(error.message ?: "")
                            }
                        }
                        else -> UiText.StringResource(R.string.error_database_operation)
                    }
                    onError(errorMsg)
                }
        }
    }

    fun deleteEvent(eventId: String, onSuccess: () -> Unit, onError: (UiText) -> Unit) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error deleting event", error)
                    onError(UiText.StringResource(R.string.error_database_operation))
                }
        }
    }

    fun respondToEvent(
        eventId: String,
        status: GuestStatus,
        onSuccess: () -> Unit,
        onError: (UiText) -> Unit
    ) {
        viewModelScope.launch {
            eventRepository.respondToEvent(eventId, status)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error responding to event", error)
                    onError(UiText.StringResource(R.string.error_database_operation))
                }
        }
    }

    fun addComment(
        eventId: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (UiText) -> Unit
    ) {
        viewModelScope.launch {
            eventRepository.addComment(eventId, text)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error adding comment", error)
                    onError(UiText.StringResource(R.string.error_database_operation))
                }
        }
    }

    fun deleteComment(
        eventId: String,
        commentId: String,
        onSuccess: () -> Unit,
        onError: (UiText) -> Unit
    ) {
        viewModelScope.launch {
            eventRepository.deleteComment(eventId, commentId)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error deleting comment", error)
                    onError(UiText.StringResource(R.string.error_database_operation))
                }
        }
    }

    fun uploadPhoto(
        eventId: String,
        uri: android.net.Uri,
        onSuccess: () -> Unit,
        onError: (UiText) -> Unit
    ) {
        viewModelScope.launch {
            eventRepository.uploadPhoto(eventId, uri)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error uploading photo", error)
                    onError(UiText.StringResource(R.string.error_uploading_image))
                }
        }
    }

    fun deletePhoto(
        eventId: String,
        photoId: String,
        storageUrl: String,
        onSuccess: () -> Unit,
        onError: (UiText) -> Unit
    ) {
        viewModelScope.launch {
            eventRepository.deletePhoto(eventId, photoId, storageUrl)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    Log.e("EventsViewModel", "Error deleting photo", error)
                    onError(UiText.StringResource(R.string.error_database_operation))
                }
        }
    }
}
