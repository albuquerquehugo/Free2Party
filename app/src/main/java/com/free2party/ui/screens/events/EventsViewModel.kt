package com.free2party.ui.screens.events

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.model.*
import com.free2party.data.repository.EventRepository
import com.free2party.data.repository.SocialRepository
import com.free2party.data.repository.UserRepository
import com.free2party.exception.EventException
import com.free2party.R
import com.free2party.util.calculateHaversineDistance
import com.free2party.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserLocation(val latitude: Double, val longitude: Double)

enum class EventFilter {
    ALL,
    GOING,
    NOT_GOING,
    PENDING
}

sealed interface EventsUiState {
    object Loading : EventsUiState
    data class Success(
        val myEvents: List<Event> = emptyList(),
        val pendingEvents: List<Event> = emptyList(),
        val publicEvents: List<Event> = emptyList()
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

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _eventFilter = MutableStateFlow(EventFilter.ALL)
    val eventFilter: StateFlow<EventFilter> = _eventFilter.asStateFlow()

    fun setUserLocation(location: UserLocation?) {
        _userLocation.value = location
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setEventFilter(filter: EventFilter) {
        _eventFilter.value = filter
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<EventsUiState> = combine(
        userRepository.userIdFlow,
        userRepository.userIdFlow.flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList())
            else eventRepository.getEvents()
        },
        _userLocation,
        _searchQuery,
        _eventFilter
    ) { uid, events, location, query, filter ->
        if (uid.isBlank()) {
            EventsUiState.Error(UiText.StringResource(R.string.error_unauthorized))
        } else {
            val myEvents = events.filter { it.hostId == uid }
            val pendingEvents = events.filter {
                it.hostId != uid && (it.invitedGuestIds ?: it.guestIds).contains(uid)
            }
            val publicEvents = events.filter { it.type == EventType.PUBLIC && it.hostId != uid }
                .let { list ->
                    if (query.isBlank()) {
                        list
                    } else {
                        val lowerQuery = query.lowercase().trim()
                        list.filter {
                            it.title.lowercase().contains(lowerQuery) ||
                                    it.description.lowercase().contains(lowerQuery) ||
                                    it.locationName.lowercase().contains(lowerQuery) ||
                                    it.hostName.lowercase().contains(lowerQuery)
                        }
                    }
                }
                .let { list ->
                    if (location != null) {
                        list.sortedWith { e1, e2 ->
                            val d1 = if (e1.latitude != null && e1.longitude != null) {
                                calculateHaversineDistance(
                                    location.latitude,
                                    location.longitude,
                                    e1.latitude,
                                    e1.longitude
                                )
                            } else {
                                Double.MAX_VALUE
                            }
                            val d2 = if (e2.latitude != null && e2.longitude != null) {
                                calculateHaversineDistance(
                                    location.latitude,
                                    location.longitude,
                                    e2.latitude,
                                    e2.longitude
                                )
                            } else {
                                Double.MAX_VALUE
                            }
                            d1.compareTo(d2)
                        }
                    } else {
                        list.sortedWith(compareBy({ it.startDate }, { it.startTime }))
                    }
                }

            val finalMyEvents = when (filter) {
                EventFilter.ALL, EventFilter.GOING -> myEvents // Host is always attending their own event
                else -> emptyList()
            }

            val finalPendingEvents = when (filter) {
                EventFilter.ALL -> pendingEvents
                EventFilter.GOING -> pendingEvents.filter { it.guests[uid] == GuestStatus.ACCEPTED.name }
                EventFilter.NOT_GOING -> pendingEvents.filter { it.guests[uid] == GuestStatus.DECLINED.name }
                EventFilter.PENDING -> pendingEvents.filter { (it.guests[uid] ?: GuestStatus.PENDING.name) == GuestStatus.PENDING.name }
            }

            val finalPublicEvents = when (filter) {
                EventFilter.ALL -> publicEvents
                EventFilter.GOING -> publicEvents.filter { it.guests[uid] == GuestStatus.ACCEPTED.name }
                EventFilter.NOT_GOING -> publicEvents.filter { it.guests[uid] == GuestStatus.DECLINED.name }
                EventFilter.PENDING -> publicEvents.filter { (it.guests[uid] ?: GuestStatus.PENDING.name) == GuestStatus.PENDING.name }
            }

            EventsUiState.Success(
                myEvents = finalMyEvents,
                pendingEvents = finalPendingEvents,
                publicEvents = finalPublicEvents
            )
        }
    }
        .catch { e ->
            Log.e("EventsViewModel", "Error inside events flow", e)
            emit(EventsUiState.Error(UiText.StringResource(R.string.error_unknown)))
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
                                    (event.invitedGuestIds ?: event.guestIds).contains(uid) &&
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
        fetchFallbackLocation()
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fetchFallbackLocation() {
        viewModelScope.launch {
            // 1. Try IP Geolocation
            val ipLoc = fetchLocationByIp()
            if (ipLoc != null) {
                if (_userLocation.value == null) {
                    _userLocation.value = ipLoc
                    Log.d("EventsViewModel", "Set fallback location from IP: $ipLoc")
                }
                return@launch
            }

            // 2. Try Country Code from profile
            userRepository.userIdFlow
                .filter { it.isNotBlank() }
                .flatMapLatest { uid -> userRepository.observeUser(uid) }
                .firstOrNull()?.let { user ->
                    if (_userLocation.value == null && user.countryCode.isNotBlank()) {
                        val countryLoc = CountryCoordinates[user.countryCode.uppercase().trim()]
                        if (countryLoc != null) {
                            _userLocation.value = countryLoc
                            Log.d(
                                "EventsViewModel",
                                "Set fallback location from country code: $countryLoc"
                            )
                        }
                    }
                }
        }
    }

    private suspend fun fetchLocationByIp(): UserLocation? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://ip-api.com/json")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val latRegex = Regex(""""lat"\s*:\s*(-?\d+\.?\d*)""")
                val lonRegex = Regex(""""lon"\s*:\s*(-?\d+\.?\d*)""")

                val latMatch = latRegex.find(responseText)
                val lonMatch = lonRegex.find(responseText)
                if (latMatch != null && lonMatch != null) {
                    val lat = latMatch.groupValues[1].toDoubleOrNull()
                    val lon = lonMatch.groupValues[1].toDoubleOrNull()
                    if (lat != null && lon != null) {
                        UserLocation(lat, lon)
                    } else null
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}

private val CountryCoordinates = mapOf(
    "US" to UserLocation(37.0902, -95.7129),
    "PT" to UserLocation(39.3999, -8.2245),
    "BR" to UserLocation(-14.2350, -51.9253),
    "ES" to UserLocation(40.4637, -3.7492),
    "FR" to UserLocation(46.2276, 2.2137),
    "UK" to UserLocation(55.3781, -3.4360),
    "GB" to UserLocation(55.3781, -3.4360),
    "DE" to UserLocation(51.1657, 10.4515),
    "IT" to UserLocation(41.8719, 12.5674),
    "CA" to UserLocation(56.1304, -106.3468),
    "AU" to UserLocation(-25.2744, 133.7751),
    "IN" to UserLocation(20.5937, 78.9629),
    "JP" to UserLocation(36.2048, 138.2529),
    "CN" to UserLocation(35.8617, 104.1954),
    "RU" to UserLocation(61.5240, 105.3188),
    "ZA" to UserLocation(-30.5595, 22.9375),
    "MX" to UserLocation(23.6345, -102.5528),
    "AR" to UserLocation(-38.4161, -63.6167),
    "CO" to UserLocation(4.5709, -74.2973),
    "CH" to UserLocation(46.8182, 8.2275),
    "NL" to UserLocation(52.1326, 5.2913),
    "BE" to UserLocation(50.5039, 4.4699),
    "AT" to UserLocation(47.5162, 14.5501),
    "DK" to UserLocation(56.2639, 9.5018),
    "NO" to UserLocation(60.4720, 8.4689),
    "SE" to UserLocation(60.1282, 18.6435),
    "FI" to UserLocation(61.9241, 25.7482),
    "IE" to UserLocation(53.4129, -8.2439),
    "NZ" to UserLocation(-40.9006, 174.8860)
)
