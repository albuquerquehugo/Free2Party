package com.free2party.ui.screens.events

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.free2party.data.model.Event
import com.free2party.data.model.EventLink
import com.free2party.data.model.EventType
import com.free2party.data.model.GuestStatus
import com.free2party.R
import com.free2party.ui.components.dialogs.BaseDialog
import com.free2party.ui.components.dialogs.ConfirmationDialog
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.components.basic.AppOutlinedButton
import com.free2party.ui.components.basic.AppOutlinedCard
import com.free2party.ui.components.basic.AppOutlinedTextField
import com.free2party.ui.components.TopBar
import com.free2party.ui.components.FriendSelector
import com.free2party.util.formatTime
import com.free2party.util.formatTimeForDisplay
import com.free2party.util.isDateTimeInPast
import com.free2party.util.isUrlValid
import com.free2party.util.parseDateToMillis
import com.free2party.util.parseTimeToMinutes
import com.free2party.util.unformatTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: EventsViewModel,
    editingEventId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val gradientBackground = viewModel.gradientBackground
    val use24Hour = viewModel.use24HourFormat
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    val eventCreatedMsg = stringResource(R.string.toast_event_created)
    val eventUpdatedMsg = stringResource(R.string.toast_event_updated)

    val friends by viewModel.friendsList.collectAsState()
    val circles by viewModel.circles.collectAsState()

    // Query event if editing
    var initialLoadDone by remember { mutableStateOf(value = false) }
    val currentEventState by viewModel.currentEvent.collectAsState()
    var originalEvent by remember { mutableStateOf<Event?>(null) }

    // Form fields
    var title by remember { mutableStateOf("") }
    var isTitleFocused by remember { mutableStateOf(false) }
    var hasInteractedWithTitle by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf(EventType.PRIVATE) }
    var timezone by remember { mutableStateOf(TimeZone.getDefault().id) }
    var hasInteractedWithStart by remember { mutableStateOf(false) }
    var hasInteractedWithEnd by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isLocationFocused by remember { mutableStateOf(false) }
    var hasInteractedWithLocation by remember { mutableStateOf(false) }
    val locationBringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedGuestsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var hasInteractedWithGuests by remember { mutableStateOf(false) }
    var usefulLinksList by remember { mutableStateOf<List<EventLink>>(emptyList()) }

    // Date/Time States
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    val startTimeState =
        rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = use24Hour)
    val endTimeState =
        rememberTimePickerState(initialHour = 13, initialMinute = 0, is24Hour = use24Hour)

    // Selection indicators
    val (showStartDatePicker, setShowStartDatePicker) = remember { mutableStateOf(false) }
    val (showEndDatePicker, setShowEndDatePicker) = remember { mutableStateOf(false) }
    val (showStartTimePicker, setShowStartTimePicker) = remember { mutableStateOf(false) }
    val (showEndTimePicker, setShowEndTimePicker) = remember { mutableStateOf(false) }

    // Dialog state
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkTitle by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Timezone search states
    var showTimezonePicker by remember { mutableStateOf(false) }
    var timezoneSearchQuery by remember { mutableStateOf("") }
    val timezoneOptions = remember(timezone) {
        val baseIds = listOf(
            "Etc/GMT+12",
            "Pacific/Pago_Pago",
            "Pacific/Honolulu",
            "America/Anchorage",
            "America/Los_Angeles",
            "America/Denver",
            "America/Chicago",
            "America/New_York",
            "America/Halifax",
            "America/St_Johns",
            "America/Sao_Paulo",
            "America/Noronha",
            "Atlantic/Azores",
            "Europe/London",
            "Europe/Paris",
            "Europe/Athens",
            "Europe/Moscow",
            "Asia/Tehran",
            "Asia/Dubai",
            "Asia/Kabul",
            "Asia/Karachi",
            "Asia/Kolkata",
            "Asia/Kathmandu",
            "Asia/Dhaka",
            "Asia/Yangon",
            "Asia/Bangkok",
            "Asia/Singapore",
            "Asia/Tokyo",
            "Australia/Adelaide",
            "Australia/Sydney",
            "Pacific/Guadalcanal",
            "Pacific/Auckland",
            "Pacific/Chatham",
            "Pacific/Apia",
            "Pacific/Kiritimati"
        )
        val defaultTzId = TimeZone.getDefault().id
        val allIds = (baseIds + timezone + defaultTzId).distinct()
        allIds.map { TimeZone.getTimeZone(it) }
            .sortedWith(compareBy({ it.rawOffset }, { it.id }))
    }

    val filteredTzs = remember(timezoneSearchQuery, timezoneOptions) {
        if (timezoneSearchQuery.isBlank()) {
            timezoneOptions
        } else {
            timezoneOptions.filter { tz ->
                val rawOffset = tz.rawOffset
                val hours = abs(rawOffset) / 3600000
                val mins = (abs(rawOffset) / 60000) % 60
                val sign = if (rawOffset >= 0) "+" else "-"
                val offsetStr = "GMT$sign${String.format(locale, "%02d:%02d", hours, mins)}"

                tz.id.contains(timezoneSearchQuery, ignoreCase = true) ||
                        offsetStr.contains(timezoneSearchQuery, ignoreCase = true)
            }
        }
    }

    // Geocoding suggestions
    var showLocationSuggestions by remember { mutableStateOf(false) }
    var locationSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    // Set editing details
    LaunchedEffect(editingEventId) {
        viewModel.selectEvent(editingEventId)
    }

    LaunchedEffect(currentEventState) {
        val event = currentEventState
        if (editingEventId != null && event != null && !initialLoadDone) {
            originalEvent = event
            title = event.title
            description = event.description
            eventType = event.type
            timezone = event.timezone
            locationName = event.locationName
            latitude = event.latitude
            longitude = event.longitude
            selectedGuestsMap = event.guests
            usefulLinksList = event.usefulLinks

            val startMillis = parseDateToMillis(event.startDate)
            val endMillis = parseDateToMillis(event.endDate)
            startMillis?.let { startDatePickerState.selectedDateMillis = it }
            endMillis?.let { endDatePickerState.selectedDateMillis = it }

            val startParts = unformatTime(event.startTime)
            val endParts = unformatTime(event.endTime)
            startTimeState.hour = startParts.first
            startTimeState.minute = startParts.second
            endTimeState.hour = endParts.first
            endTimeState.minute = endParts.second

            initialLoadDone = true
        }
    }

    // Geocoder Lookup with Debounce
    LaunchedEffect(locationName) {
        if (locationName.length > 2 && locationSuggestions.firstOrNull()?.featureName != locationName) {
            delay(500.milliseconds)
            withContext(Dispatchers.IO) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(locationName, 5) { addresses ->
                            locationSuggestions = addresses
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(locationName, 5)
                        if (addresses != null) {
                            locationSuggestions = addresses
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } else {
            locationSuggestions = emptyList()
        }
    }

    LaunchedEffect(isLocationFocused) {
        if (isLocationFocused) {
            delay(100.milliseconds)
            keyboardController?.show()
            delay(100.milliseconds)
            locationBringIntoViewRequester.bringIntoView(rect = Rect(0f, 0f, 0f, 600f))
        }
    }

    // Format Dates
    val dateFormatter = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val startDateText =
        startDatePickerState.selectedDateMillis?.let { dateFormatter.format(Date(it)) }
            ?: stringResource(R.string.label_select_start_date)
    val endDateText = endDatePickerState.selectedDateMillis?.let { dateFormatter.format(Date(it)) }
        ?: stringResource(R.string.label_select_end_date)

    // Form validation
    val isDateTimeValid = remember(
        startDatePickerState.selectedDateMillis,
        endDatePickerState.selectedDateMillis,
        startTimeState.hour,
        startTimeState.minute,
        endTimeState.hour,
        endTimeState.minute
    ) {
        val startMillis = startDatePickerState.selectedDateMillis ?: 0L
        val endMillis = endDatePickerState.selectedDateMillis ?: 0L

        if (startMillis < endMillis) return@remember true
        if (startMillis > endMillis) return@remember false

        val startMins =
            parseTimeToMinutes(formatTime(startTimeState.hour, startTimeState.minute)) ?: 0
        val endMins = parseTimeToMinutes(formatTime(endTimeState.hour, endTimeState.minute)) ?: 0
        startMins < endMins
    }

    val isStartDateInPast = remember(startDatePickerState.selectedDateMillis) {
        startDatePickerState.selectedDateMillis?.let {
            isDateTimeInPast(it, null)
        } ?: false
    }

    val isStartTimeInPast = remember(
        startDatePickerState.selectedDateMillis,
        startTimeState.hour,
        startTimeState.minute
    ) {
        startDatePickerState.selectedDateMillis?.let {
            isDateTimeInPast(it, formatTime(startTimeState.hour, startTimeState.minute))
        } ?: false
    }

    val isFormValid = title.isNotBlank() && startDatePickerState.selectedDateMillis != null &&
            endDatePickerState.selectedDateMillis != null && isDateTimeValid &&
            !isStartDateInPast && !isStartTimeInPast &&
            locationName.isNotBlank() && latitude != null && longitude != null &&
            (eventType == EventType.PUBLIC || selectedGuestsMap.isNotEmpty())

    val hasChanges = remember(
        title, description, eventType, timezone, locationName, latitude, longitude,
        selectedGuestsMap, usefulLinksList,
        startDatePickerState.selectedDateMillis, endDatePickerState.selectedDateMillis,
        startTimeState.hour, startTimeState.minute, endTimeState.hour, endTimeState.minute,
        originalEvent, initialLoadDone, editingEventId
    ) {
        if (editingEventId == null) {
            title.isNotBlank() ||
                    description.isNotBlank() ||
                    locationName.isNotBlank() ||
                    selectedGuestsMap.isNotEmpty() ||
                    usefulLinksList.isNotEmpty() ||
                    startDatePickerState.selectedDateMillis != null ||
                    endDatePickerState.selectedDateMillis != null
        } else {
            val event = originalEvent ?: return@remember false
            if (!initialLoadDone) return@remember false

            val startD = parseDateToMillis(event.startDate)
            val endD = parseDateToMillis(event.endDate)
            val startT = unformatTime(event.startTime)
            val endT = unformatTime(event.endTime)

            title != event.title ||
                    description != event.description ||
                    eventType != event.type ||
                    timezone != event.timezone ||
                    locationName != event.locationName ||
                    latitude != event.latitude ||
                    longitude != event.longitude ||
                    selectedGuestsMap != event.guests ||
                    usefulLinksList != event.usefulLinks ||
                    startDatePickerState.selectedDateMillis != startD ||
                    endDatePickerState.selectedDateMillis != endD ||
                    startTimeState.hour != startT.first ||
                    startTimeState.minute != startT.second ||
                    endTimeState.hour != endT.first ||
                    endTimeState.minute != endT.second
        }
    }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = if (editingEventId == null) stringResource(R.string.label_create_event) else stringResource(
                    R.string.label_edit_event
                ),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = {
                    if (hasChanges) {
                        showDiscardDialog = true
                    } else {
                        onBack()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.text_required_fields_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Title Input
            Column(modifier = Modifier.fillMaxWidth()) {
                AppOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    labelText = stringResource(R.string.label_event_title) + " *",
                    placeholderText = stringResource(R.string.placeholder_event_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isTitleFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                hasInteractedWithTitle = true
                            }
                        },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    maxLines = 1
                )
                if (hasInteractedWithTitle && !isTitleFocused && title.isBlank()) {
                    Text(
                        text = stringResource(R.string.error_title_mandatory),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            // Description Input
            AppOutlinedTextField(
                value = description,
                onValueChange = { description = it },
                labelText = stringResource(R.string.label_event_description),
                placeholderText = stringResource(R.string.placeholder_event_description),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )

            // Event Type (PUBLIC / PRIVATE)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_event_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            eventType = EventType.PRIVATE
                            hasInteractedWithGuests = true
                        }
                    ) {
                        RadioButton(
                            selected = eventType == EventType.PRIVATE,
                            onClick = {
                                eventType = EventType.PRIVATE
                                hasInteractedWithGuests = true
                            })
                        Column {
                            Text(
                                stringResource(R.string.label_private),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.label_event_type_desc_private),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            eventType = EventType.PUBLIC
                            hasInteractedWithGuests = true
                        }
                    ) {
                        RadioButton(
                            selected = eventType == EventType.PUBLIC,
                            onClick = {
                                eventType = EventType.PUBLIC
                                hasInteractedWithGuests = true
                            })
                        Column {
                            Text(
                                stringResource(R.string.label_public),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.label_event_type_desc_public),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AppHorizontalDivider()

            // Date / Time Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Start
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_start) + " *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        onClick = {
                            setShowStartDatePicker(true)
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = startDateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (isStartDateInPast || !isDateTimeValid) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(0.6f)
                            .height(44.dp),
                        onClick = {
                            setShowStartTimePicker(true)
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = formatTimeForDisplay(
                                    formatTime(
                                        startTimeState.hour,
                                        startTimeState.minute
                                    ), use24Hour
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (isStartTimeInPast || !isDateTimeValid) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (hasInteractedWithStart && startDatePickerState.selectedDateMillis == null) {
                    Text(
                        text = stringResource(R.string.error_start_date_mandatory),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // End
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_end) + " *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        onClick = {
                            setShowEndDatePicker(true)
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = endDateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (!isDateTimeValid) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(0.6f)
                            .height(44.dp),
                        onClick = {
                            setShowEndTimePicker(true)
                        }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = formatTimeForDisplay(
                                    formatTime(
                                        endTimeState.hour,
                                        endTimeState.minute
                                    ), use24Hour
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (!isDateTimeValid) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (hasInteractedWithEnd && endDatePickerState.selectedDateMillis == null) {
                    Text(
                        text = stringResource(R.string.error_end_date_mandatory),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                if (isStartDateInPast || isStartTimeInPast) {
                    Text(
                        text = stringResource(R.string.error_event_past_date_time),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                if (!isDateTimeValid && startDatePickerState.selectedDateMillis != null && endDatePickerState.selectedDateMillis != null) {
                    Text(
                        text = stringResource(R.string.error_invalid_datetime),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Timezone Picker Button
            AppOutlinedButton(
                onClick = { showTimezonePicker = true },
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val rawOffset = TimeZone.getTimeZone(timezone).rawOffset
                val hours = abs(rawOffset) / 3600000
                val mins = (abs(rawOffset) / 60000) % 60
                val sign = if (rawOffset >= 0) "+" else "-"
                Text(
                    "GMT$sign${
                        String.format(
                            locale,
                            "%02d:%02d",
                            hours,
                            mins
                        )
                    } - ${timezone.replace("_", " ")}"
                )
            }

            AppHorizontalDivider()

            // Location picker with suggestions
            Column(modifier = Modifier.fillMaxWidth()) {
                AppOutlinedTextField(
                    value = locationName,
                    onValueChange = {
                        locationName = it
                        showLocationSuggestions = true
                    },
                    labelText = stringResource(R.string.label_location) + " *",
                    placeholderText = stringResource(R.string.placeholder_location),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    trailingIcon = {
                        if (locationName.isNotBlank()) {
                            IconButton(onClick = {
                                locationName = ""
                                latitude = null
                                longitude = null
                                hasInteractedWithLocation = true
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(locationBringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            isLocationFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                hasInteractedWithLocation = true
                            }
                        },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Suggestions popup
                AnimatedVisibility(visible = showLocationSuggestions && locationSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            locationSuggestions.take(3).forEach { loc ->
                                val label = loc.getAddressLine(0) ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            locationName = label
                                            latitude = loc.latitude
                                            longitude = loc.longitude
                                            showLocationSuggestions = false
                                            focusManager.clearFocus()
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                if (hasInteractedWithLocation && !isLocationFocused && (locationName.isBlank() || latitude == null || longitude == null)) {
                    Text(
                        text = stringResource(R.string.error_location_mandatory),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            AppHorizontalDivider()

            // Guests Picker
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_event_guests_selection) + if (eventType == EventType.PRIVATE) " *" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FriendSelector(
                    friends = friends,
                    circles = circles,
                    selectedFriendIds = selectedGuestsMap.keys.toList(),
                    onToggleFriend = { id ->
                        hasInteractedWithGuests = true
                        selectedGuestsMap = if (id in selectedGuestsMap) {
                            selectedGuestsMap - id
                        } else {
                            val status = originalEvent?.guests?.get(id) ?: GuestStatus.PENDING.name
                            selectedGuestsMap + (id to status)
                        }
                    },
                    onAddFriends = { ids ->
                        hasInteractedWithGuests = true
                        val additions =
                            ids.filter { it !in selectedGuestsMap }.associateWith { id ->
                                originalEvent?.guests?.get(id) ?: GuestStatus.PENDING.name
                            }
                        selectedGuestsMap = selectedGuestsMap + additions
                    },
                    onRemoveFriends = { ids ->
                        hasInteractedWithGuests = true
                        selectedGuestsMap = selectedGuestsMap - ids.toSet()
                    },
                    onSelectAll = {
                        hasInteractedWithGuests = true
                        val all = friends.associate { friend ->
                            val status = selectedGuestsMap[friend.uid]
                                ?: originalEvent?.guests?.get(friend.uid)
                                ?: GuestStatus.PENDING.name
                            friend.uid to status
                        }
                        selectedGuestsMap = all
                    },
                    onUnselectAll = {
                        hasInteractedWithGuests = true
                        selectedGuestsMap = emptyMap()
                    }
                )

                if (hasInteractedWithGuests && eventType == EventType.PRIVATE && selectedGuestsMap.isEmpty()) {
                    Text(
                        text = stringResource(R.string.error_guests_mandatory_private),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            AppHorizontalDivider()

            // Useful Links Card
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_useful_links),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        linkTitle = ""
                        linkUrl = ""
                        showLinkDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.label_add_link))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    usefulLinksList.forEach { link ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.5f
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        link.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        link.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { usefulLinksList -= link }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editingEventId != null && hasChanges) {
                    TextButton(
                        onClick = {
                            val event = currentEventState
                            if (event != null) {
                                title = event.title
                                description = event.description
                                eventType = event.type
                                timezone = event.timezone
                                locationName = event.locationName
                                latitude = event.latitude
                                longitude = event.longitude
                                selectedGuestsMap = event.guests
                                usefulLinksList = event.usefulLinks

                                val startMillis = parseDateToMillis(event.startDate)
                                val endMillis = parseDateToMillis(event.endDate)
                                startMillis?.let { startDatePickerState.selectedDateMillis = it }
                                endMillis?.let { endDatePickerState.selectedDateMillis = it }

                                val startParts = unformatTime(event.startTime)
                                val endParts = unformatTime(event.endTime)
                                startTimeState.hour = startParts.first
                                startTimeState.minute = startParts.second
                                endTimeState.hour = endParts.first
                                endTimeState.minute = endParts.second

                                hasInteractedWithLocation = false
                                isLocationFocused = false
                                hasInteractedWithGuests = false
                                hasInteractedWithTitle = false
                                isTitleFocused = false
                                hasInteractedWithStart = false
                                hasInteractedWithEnd = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.label_discard_changes),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val startMillis = startDatePickerState.selectedDateMillis
                    val endMillis = endDatePickerState.selectedDateMillis
                    if (startMillis != null && endMillis != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val startD = sdf.format(Date(startMillis))
                        val endD = sdf.format(Date(endMillis))
                        val startT = formatTime(startTimeState.hour, startTimeState.minute)
                        val endT = formatTime(endTimeState.hour, endTimeState.minute)

                        if (editingEventId == null) {
                            viewModel.saveEvent(
                                title = title,
                                description = description,
                                type = eventType,
                                startDate = startD,
                                startTime = startT,
                                endDate = endD,
                                endTime = endT,
                                timezone = timezone,
                                locationName = locationName,
                                latitude = latitude,
                                longitude = longitude,
                                guests = selectedGuestsMap,
                                usefulLinks = usefulLinksList,
                                onSuccess = {
                                    Toast.makeText(context, eventCreatedMsg, Toast.LENGTH_SHORT)
                                        .show()
                                    onBack()
                                },
                                onError = { err ->
                                    Toast.makeText(
                                        context,
                                        err.asString(context),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        } else {
                            viewModel.updateEvent(
                                eventId = editingEventId,
                                title = title,
                                description = description,
                                type = eventType,
                                startDate = startD,
                                startTime = startT,
                                endDate = endD,
                                endTime = endT,
                                timezone = timezone,
                                locationName = locationName,
                                latitude = latitude,
                                longitude = longitude,
                                guests = selectedGuestsMap,
                                usefulLinks = usefulLinksList,
                                onSuccess = {
                                    Toast.makeText(context, eventUpdatedMsg, Toast.LENGTH_SHORT)
                                        .show()
                                    onBack()
                                },
                                onError = { err ->
                                    Toast.makeText(
                                        context,
                                        err.asString(context),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(56.dp),
                enabled = isFormValid && (editingEventId == null || hasChanges),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text =
                        if (editingEventId == null) stringResource(R.string.label_confirm)
                        else stringResource(R.string.label_update),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // Timezone selection dialog
    if (showTimezonePicker) {
        BaseDialog(onDismissRequest = { showTimezonePicker = false }) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_timezone),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                AppOutlinedTextField(
                    value = timezoneSearchQuery,
                    onValueChange = { timezoneSearchQuery = it },
                    placeholderText = stringResource(R.string.label_timezone_search),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredTzs) { tz ->
                        val rawOffset = tz.rawOffset
                        val hours = abs(rawOffset) / 3600000
                        val mins = (abs(rawOffset) / 60000) % 60
                        val sign = if (rawOffset >= 0) "+" else "-"
                        val offsetStr = "GMT$sign${String.format(locale, "%02d:%02d", hours, mins)}"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    timezone = tz.id
                                    showTimezonePicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$offsetStr - ${tz.id.replace("_", " ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Link insertion dialog
    if (showLinkDialog) {
        BaseDialog(onDismissRequest = { showLinkDialog = false }) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_add_link),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                AppOutlinedTextField(
                    value = linkTitle,
                    onValueChange = { linkTitle = it },
                    labelText = stringResource(R.string.label_link_title),
                    placeholderText = stringResource(R.string.placeholder_link_title),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                val formattedLinkUrl = remember(linkUrl) {
                    val trimmed = linkUrl.trim()
                    if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith(
                            "https://"
                        )
                    ) {
                        "https://$trimmed"
                    } else {
                        trimmed
                    }
                }

                val isUrlFormatValid = remember(linkUrl) {
                    linkUrl.isBlank() || isUrlValid(linkUrl)
                }

                val isDuplicate = remember(formattedLinkUrl, usefulLinksList) {
                    formattedLinkUrl.isNotBlank() && usefulLinksList.any { it.url == formattedLinkUrl }
                }

                AppOutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    labelText = stringResource(R.string.label_link_url),
                    placeholderText = stringResource(R.string.placeholder_link_url),
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isUrlFormatValid || isDuplicate,
                    supportingText = {
                        if (!isUrlFormatValid) {
                            Text(stringResource(R.string.error_invalid_url))
                        } else if (isDuplicate) {
                            Text(stringResource(R.string.error_duplicate_link))
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        showLinkDialog = false
                    }) { Text(stringResource(R.string.label_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            usefulLinksList =
                                usefulLinksList + EventLink(linkTitle.trim(), formattedLinkUrl)
                            showLinkDialog = false
                        },
                        enabled = linkTitle.isNotBlank() && linkUrl.isNotBlank() && isUrlFormatValid && !isDuplicate
                    ) {
                        Text(stringResource(R.string.label_add))
                    }
                }
            }
        }
    }

    // Date/Time selection dialog components (Reusing plan logic)
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                setShowStartDatePicker(false)
                hasInteractedWithStart = true
            },
            confirmButton = {
                TextButton(onClick = {
                    setShowStartDatePicker(false)
                    hasInteractedWithStart = true
                }) { Text(stringResource(R.string.label_ok)) }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                setShowEndDatePicker(false)
                hasInteractedWithEnd = true
            },
            confirmButton = {
                TextButton(onClick = {
                    setShowEndDatePicker(false)
                    hasInteractedWithEnd = true
                }) { Text(stringResource(R.string.label_ok)) }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    if (showStartTimePicker || showEndTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = if (showStartTimePicker) startTimeState.hour else endTimeState.hour,
            initialMinute = if (showStartTimePicker) startTimeState.minute else endTimeState.minute,
            is24Hour = use24Hour
        )
        BaseDialog(onDismissRequest = {
            if (showStartTimePicker) {
                hasInteractedWithStart = true
            }
            if (showEndTimePicker) {
                hasInteractedWithEnd = true
            }
            setShowStartTimePicker(false)
            setShowEndTimePicker(false)
        }) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (showStartTimePicker) stringResource(R.string.label_select_start_time) else stringResource(
                        R.string.label_select_end_time
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TimePicker(state = pickerState)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        if (showStartTimePicker) {
                            startTimeState.hour = pickerState.hour
                            startTimeState.minute = pickerState.minute
                            hasInteractedWithStart = true
                        } else {
                            endTimeState.hour = pickerState.hour
                            endTimeState.minute = pickerState.minute
                            hasInteractedWithEnd = true
                        }
                        setShowStartTimePicker(false)
                        setShowEndTimePicker(false)
                    }) { Text(stringResource(R.string.label_ok)) }
                }
            }
        }
    }

    if (showDiscardDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.label_discard_changes),
            text = stringResource(R.string.text_discard_changes_confirmation),
            confirmButtonText = stringResource(R.string.label_discard_changes),
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismiss = { showDiscardDialog = false },
            isDestructive = true
        )
    }
}
