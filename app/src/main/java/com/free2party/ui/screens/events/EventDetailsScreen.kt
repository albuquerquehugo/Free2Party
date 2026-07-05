package com.free2party.ui.screens.events

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.free2party.R
import com.free2party.data.model.*
import com.free2party.ui.components.dialogs.BaseDialog
import com.free2party.ui.components.dialogs.ConfirmationDialog
import com.free2party.ui.components.dialogs.PublicProfileDialog
import com.free2party.ui.components.TopBar
import com.free2party.ui.components.basic.AppOutlinedTextField
import com.free2party.ui.theme.available
import com.free2party.ui.theme.busy
import com.free2party.util.formatPlanDateInFull
import com.free2party.util.formatTimeAgo
import com.free2party.util.formatTimeForDisplay
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    viewModel: EventsViewModel,
    eventId: String,
    scrollToComments: Boolean = false,
    onNavigateToEditEvent: (String) -> Unit,
    onNavigateToEvents: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val gradientBackground = viewModel.gradientBackground
    val currentUserId = viewModel.currentUserId
    val use24Hour = viewModel.use24HourFormat

    val commentInteractionSource = remember { MutableInteractionSource() }
    val isCommentFocused by commentInteractionSource.collectIsFocusedAsState()
    val commentBringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val commentsBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(scrollToComments, eventId) {
        if (scrollToComments) {
            delay(300.milliseconds)
            commentsBringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(isCommentFocused) {
        if (isCommentFocused) {
            delay(100.milliseconds)
            keyboardController?.show()
            delay(100.milliseconds)
            commentBringIntoViewRequester.bringIntoView(rect = Rect(0f, 0f, 0f, 200f))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.eventNotFoundEvent.collectLatest {
            Toast.makeText(context, R.string.error_event_not_found, Toast.LENGTH_SHORT).show()
            onNavigateToEvents()
        }
    }

    val commentDeletedMsg = stringResource(R.string.toast_comment_deleted)
    val eventDeletedMsg = stringResource(R.string.toast_event_deleted)
    val noMapAvailableMsg = stringResource(R.string.error_no_app_available)
    val photoUploadedMsg = stringResource(R.string.toast_photo_uploaded)
    val photoDeletedMsg = stringResource(R.string.toast_photo_deleted)
    val editDisabledMsg = stringResource(R.string.error_event_edit_current_past)

    val event by viewModel.currentEvent.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val guestProfiles by viewModel.eventGuests.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var editingComment by remember { mutableStateOf<EventComment?>(null) }
    var activeMenuCommentId by remember { mutableStateOf<String?>(null) }
    var selectedFriendForProfile by remember { mutableStateOf<FriendInfo?>(null) }
    var selectedPhotoForView by remember { mutableStateOf<EventPhoto?>(null) }
    var selectedStatuses by remember {
        mutableStateOf(setOf(GuestStatus.ACCEPTED, GuestStatus.PENDING, GuestStatus.DECLINED))
    }

    var showDeleteEventDialog by remember { mutableStateOf(false) }
    var showDeleteCommentDialog by remember { mutableStateOf<EventComment?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadPhoto(
                eventId = eventId,
                uri = it,
                onSuccess = {
                    Toast.makeText(context, photoUploadedMsg, Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, err.asString(context), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    LaunchedEffect(eventId) {
        viewModel.selectEvent(eventId)
    }

    val ev = event
    if (ev == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isHost = ev.hostId == currentUserId
    val myStatus = remember(ev.guests, currentUserId) {
        ev.guests[currentUserId]?.let { GuestStatus.valueOf(it) } ?: GuestStatus.PENDING
    }
    val isAccepted = isHost || myStatus == GuestStatus.ACCEPTED

    // Timezone translation details
    val tzMatch = TimeZone.getTimeZone(ev.timezone).hasSameRules(TimeZone.getDefault())
    val timeFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(ev.timezone)
        }
    }
    val eventDateTime = remember(ev.startDate, ev.startTime) {
        runCatching { timeFormatter.parse("${ev.startDate} ${ev.startTime}") }.getOrNull()
    }
    val eventEndDateTime = remember(ev.endDate, ev.endTime) {
        runCatching { timeFormatter.parse("${ev.endDate} ${ev.endTime}") }.getOrNull()
    }

    val durationData = remember(eventDateTime, eventEndDateTime) {
        if (eventDateTime == null || eventEndDateTime == null) return@remember null
        val diffMillis = eventEndDateTime.time - eventDateTime.time
        if (diffMillis <= 0) return@remember null

        val diffMin = diffMillis / 60000L
        val d = (diffMin / (24 * 60)).toInt()
        val h = ((diffMin % (24 * 60)) / 60).toInt()
        val m = (diffMin % 60).toInt()
        Triple(d, h, m)
    }

    val durationText = durationData?.let { (d, h, m) ->
        val parts = mutableListOf<String>()
        if (d > 0) parts.add(stringResource(R.string.duration_days, d))
        if (h > 0) parts.add(stringResource(R.string.duration_hours, h))
        if (m > 0 || parts.isEmpty()) parts.add(stringResource(R.string.duration_minutes, m))
        parts.joinToString(" ")
    }

    val localDateText = remember(eventDateTime) {
        eventDateTime?.let {
            val localSdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
            localSdf.format(it)
        } ?: formatPlanDateInFull(ev.startDate)
    }
    val localTimeText = remember(eventDateTime) {
        eventDateTime?.let {
            val localSdf =
                SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
            localSdf.format(it)
        } ?: formatTimeForDisplay(ev.startTime, use24Hour)
    }

    val isPastStartTime = remember(eventDateTime) {
        eventDateTime?.let { System.currentTimeMillis() > it.time } ?: false
    }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.label_event_details),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack,
                action = {
                    if (isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isPastStartTime) {
                                        Toast.makeText(
                                            context,
                                            editDisabledMsg,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onNavigateToEditEvent(eventId)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.label_edit),
                                    tint = if (isPastStartTime) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            IconButton(onClick = { showDeleteEventDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.label_delete_event),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title & Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = ev.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Host Info Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            if (ev.hostProfilePic.isNotBlank()) {
                                AsyncImage(
                                    model = ev.hostProfilePic,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.label_hosted_by,
                                if (isHost) stringResource(R.string.label_you) else ev.hostName
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (ev.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = ev.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Date / Time Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_date_and_time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "$localDateText @ $localTimeText",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!tzMatch) {
                                Text(
                                    text = stringResource(
                                        R.string.label_your_local_time,
                                        formatPlanDateInFull(ev.startDate),
                                        formatTimeForDisplay(ev.startTime, use24Hour),
                                        ev.timezone
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = ev.timezone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (durationText != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.label_duration, durationText),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Location Card
            if (ev.locationName.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_location),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ev.locationName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (ev.latitude != null && ev.longitude != null) {
                                Button(
                                    onClick = {
                                        val gmmIntentUri = "geo:${ev.latitude},${ev.longitude}?q=${
                                            Uri.encode(ev.locationName)
                                        }".toUri()
                                        val mapIntent =
                                            Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                                setPackage("com.google.android.apps.maps")
                                            }
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                context,
                                                noMapAvailableMsg,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(stringResource(R.string.label_open_in_maps))
                                }
                            }
                        }
                    }
                }
            }

            // RSVP Response (if not host)
            if (!isHost) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_rsvp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.respondToEvent(
                                        eventId = eventId,
                                        status = GuestStatus.ACCEPTED,
                                        onSuccess = {},
                                        onError = { err ->
                                            Toast.makeText(
                                                context,
                                                err.asString(context),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (myStatus == GuestStatus.ACCEPTED) {
                                        MaterialTheme.colorScheme.available
                                    } else {
                                        MaterialTheme.colorScheme.available.copy(alpha = 0.12f)
                                    },
                                    contentColor = if (myStatus == GuestStatus.ACCEPTED) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.available
                                    }
                                ),
                                border = if (myStatus == GuestStatus.ACCEPTED) {
                                    null
                                } else {
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.available.copy(alpha = 0.5f)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.label_going_self))
                            }
                            Button(
                                onClick = {
                                    viewModel.respondToEvent(
                                        eventId = eventId,
                                        status = GuestStatus.DECLINED,
                                        onSuccess = {},
                                        onError = { err ->
                                            Toast.makeText(
                                                context,
                                                err.asString(context),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (myStatus == GuestStatus.DECLINED) {
                                        MaterialTheme.colorScheme.busy
                                    } else {
                                        MaterialTheme.colorScheme.busy.copy(alpha = 0.12f)
                                    },
                                    contentColor = if (myStatus == GuestStatus.DECLINED) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.busy
                                    }
                                ),
                                border = if (myStatus == GuestStatus.DECLINED) {
                                    null
                                } else {
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.busy.copy(alpha = 0.5f)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.label_not_going_self))
                            }
                        }
                    }
                }
            }

            // Guests list Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val visibleGuests =
                        remember(ev.guests, ev.invitedGuestIds, ev.guestIds, ev.type) {
                            val isPublic = ev.type == EventType.PUBLIC
                            ev.guests.filter { entry ->
                                if (isPublic) {
                                    val invitedIds = ev.invitedGuestIds ?: ev.guestIds
                                    val isInvited = invitedIds.contains(entry.key)
                                    isInvited || entry.value == GuestStatus.ACCEPTED.name
                                } else {
                                    true
                                }
                            }
                        }

                    Text(
                        text = stringResource(R.string.label_guests, ev.guests.size),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isHost && visibleGuests.isNotEmpty()) {
                        val acceptedCount = remember(visibleGuests) {
                            visibleGuests.values.count { it == GuestStatus.ACCEPTED.name }
                        }
                        val pendingCount = remember(visibleGuests) {
                            visibleGuests.values.count { it == GuestStatus.PENDING.name }
                        }
                        val declinedCount = remember(visibleGuests) {
                            visibleGuests.values.count { it == GuestStatus.DECLINED.name }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusSummaryItem(
                                label = stringResource(R.string.label_going),
                                count = acceptedCount,
                                color = MaterialTheme.colorScheme.available,
                                isChecked = GuestStatus.ACCEPTED in selectedStatuses,
                                onClick = {
                                    selectedStatuses =
                                        if (GuestStatus.ACCEPTED in selectedStatuses) {
                                            selectedStatuses - GuestStatus.ACCEPTED
                                        } else {
                                            selectedStatuses + GuestStatus.ACCEPTED
                                        }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StatusSummaryItem(
                                label = stringResource(R.string.label_pending),
                                count = pendingCount,
                                color = MaterialTheme.colorScheme.primary,
                                isChecked = GuestStatus.PENDING in selectedStatuses,
                                onClick = {
                                    selectedStatuses =
                                        if (GuestStatus.PENDING in selectedStatuses) {
                                            selectedStatuses - GuestStatus.PENDING
                                        } else {
                                            selectedStatuses + GuestStatus.PENDING
                                        }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StatusSummaryItem(
                                label = stringResource(R.string.label_not_going),
                                count = declinedCount,
                                color = MaterialTheme.colorScheme.busy,
                                isChecked = GuestStatus.DECLINED in selectedStatuses,
                                onClick = {
                                    selectedStatuses =
                                        if (GuestStatus.DECLINED in selectedStatuses) {
                                            selectedStatuses - GuestStatus.DECLINED
                                        } else {
                                            selectedStatuses + GuestStatus.DECLINED
                                        }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (visibleGuests.isEmpty()) {
                        Text(
                            text = stringResource(R.string.label_no_guests_pending),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val sortedGuests =
                            remember(visibleGuests, currentUserId, selectedStatuses) {
                                val list = visibleGuests.entries.toList()
                                val currentUserEntry = list.find { it.key == currentUserId }
                                val remainingGuests = list.filter { it.key != currentUserId }
                                    .sortedWith { o1, o2 ->
                                        val s1 = GuestStatus.valueOf(o1.value)
                                        val s2 = GuestStatus.valueOf(o2.value)
                                        val p1 = when (s1) {
                                            GuestStatus.ACCEPTED -> 1
                                            GuestStatus.PENDING -> 2
                                            GuestStatus.DECLINED -> 3
                                        }
                                        val p2 = when (s2) {
                                            GuestStatus.ACCEPTED -> 1
                                            GuestStatus.PENDING -> 2
                                            GuestStatus.DECLINED -> 3
                                        }
                                        p1.compareTo(p2)
                                    }
                                val combined = if (currentUserEntry != null) {
                                    listOf(currentUserEntry) + remainingGuests
                                } else {
                                    remainingGuests
                                }
                                if (isHost) {
                                    combined.filter { entry ->
                                        val status = GuestStatus.valueOf(entry.value)
                                        status in selectedStatuses
                                    }
                                } else {
                                    combined
                                }
                            }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sortedGuests) { entry ->
                                val uid = entry.key
                                val status = GuestStatus.valueOf(entry.value)
                                val guestUser = guestProfiles[uid]
                                val guestName =
                                    guestUser?.fullName ?: stringResource(R.string.label_pending)
                                val displayName =
                                    if (uid == currentUserId) stringResource(R.string.label_you) else guestName
                                val guestPic = guestUser?.profilePicUrl ?: ""
                                val statusColor = when (status) {
                                    GuestStatus.ACCEPTED -> MaterialTheme.colorScheme.available
                                    GuestStatus.DECLINED -> MaterialTheme.colorScheme.busy
                                    GuestStatus.PENDING -> MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            selectedFriendForProfile = FriendInfo(
                                                uid = uid,
                                                name = guestName,
                                                profilePicUrl = guestPic,
                                                isFreeNow = status == GuestStatus.ACCEPTED
                                            )
                                        }
                                        .width(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, statusColor, CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(2.dp)
                                        ) {
                                            if (guestPic.isNotBlank()) {
                                                AsyncImage(
                                                    model = guestPic,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy((-6).dp)
                                        ) {
                                            Text(
                                                text = displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            val statusText = when (status) {
                                                GuestStatus.ACCEPTED -> {
                                                    if (uid == currentUserId) stringResource(R.string.label_going_self)
                                                    else stringResource(R.string.label_going)
                                                }

                                                GuestStatus.DECLINED -> {
                                                    if (uid == currentUserId) stringResource(R.string.label_not_going_self)
                                                    else stringResource(R.string.label_not_going)
                                                }

                                                GuestStatus.PENDING -> stringResource(R.string.label_pending)
                                            }
                                            Text(
                                                text = statusText,
                                                fontSize = 9.sp,
                                                color = statusColor,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Useful Links Card
            if (ev.usefulLinks.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_useful_links),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ev.usefulLinks.forEach { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                                        context.startActivity(intent)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        link.title,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        link.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Photo Album Section (Host & Accepted users only)
            if (isAccepted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_photo_album),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.label_upload_photo))
                            }
                        }

                        if (photos.isEmpty()) {
                            Text(
                                text = stringResource(R.string.label_photos_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // 3 column grid of photos (max height in details to prevent full page take)
                            Box(modifier = Modifier.heightIn(max = 240.dp)) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(photos) { photo ->
                                        AsyncImage(
                                            model = photo.url,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedPhotoForView = photo },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Comment Section (All pending users)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(commentsBringIntoViewRequester),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_comments),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (comments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.label_comments_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            comments.forEach { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val userProfile: @Composable () -> Unit = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            if (comment.userProfilePic.isNotBlank()) {
                                                AsyncImage(
                                                    model = comment.userProfilePic,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                    val commentBubble: @Composable (Modifier) -> Unit =
                                        { modifier ->
                                            val isBeingEdited = editingComment?.id == comment.id
                                            Card(
                                                modifier = modifier
                                                    .then(
                                                        if (isBeingEdited) Modifier.border(
                                                            BorderStroke(
                                                                1.dp,
                                                                MaterialTheme.colorScheme.primary
                                                            ),
                                                            shape = CardDefaults.shape
                                                        ) else Modifier
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = when {
                                                        isBeingEdited -> MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.2f
                                                        )

                                                        comment.userId == currentUserId -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.surface
                                                    },
                                                    contentColor = when {
                                                        isBeingEdited -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        comment.userId == currentUserId -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        horizontal = 12.dp,
                                                        vertical = 8.dp
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text =
                                                                if (comment.userId == currentUserId) stringResource(
                                                                    R.string.label_you
                                                                )
                                                                else comment.userName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                4.dp
                                                            )
                                                        ) {
                                                            if (comment.edited) {
                                                                Text(
                                                                    text = stringResource(R.string.label_comment_edited),
                                                                    fontSize = 9.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                        alpha = 0.6f
                                                                    )
                                                                )
                                                            }
                                                            Text(
                                                                text = formatTimeAgo(comment.createdAt).asString(),
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = comment.text,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    val dropdownMenu: @Composable () -> Unit = {
                                        if (comment.userId == currentUserId || isHost) {
                                            Box {
                                                IconButton(
                                                    onClick = { activeMenuCommentId = comment.id },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.MoreVert,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = activeMenuCommentId == comment.id,
                                                    onDismissRequest = {
                                                        activeMenuCommentId = null
                                                    }
                                                ) {
                                                    if (comment.userId == currentUserId) {
                                                        DropdownMenuItem(
                                                            text = { Text(stringResource(R.string.label_edit)) },
                                                            onClick = {
                                                                activeMenuCommentId = null
                                                                editingComment = comment
                                                                commentText = comment.text
                                                            },
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Default.Edit,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.label_delete)) },
                                                        onClick = {
                                                            activeMenuCommentId = null
                                                            showDeleteCommentDialog = comment
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(16.dp),
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val isCurrentUserComment = comment.userId == currentUserId
                                    if (isCurrentUserComment) {
                                        dropdownMenu()
                                        commentBubble(Modifier.weight(1f))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        userProfile()
                                    } else {
                                        userProfile()
                                        Spacer(modifier = Modifier.width(10.dp))
                                        commentBubble(Modifier.weight(1f))
                                        dropdownMenu()
                                    }
                                }
                            }
                        }
                    }

                    // Input to write comment
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (editingComment != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_editing_comment),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        editingComment = null
                                        commentText = ""
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_discard),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppOutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholderText = stringResource(R.string.placeholder_comment),
                                modifier = Modifier
                                    .weight(1f)
                                    .bringIntoViewRequester(commentBringIntoViewRequester),
                                interactionSource = commentInteractionSource,
                                maxLines = 5,
                                shape = RoundedCornerShape(24.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    keyboardType = KeyboardType.Text
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        val editCommentObj = editingComment
                                        if (editCommentObj != null) {
                                            viewModel.editComment(
                                                eventId = eventId,
                                                commentId = editCommentObj.id,
                                                text = commentText.trim(),
                                                onSuccess = {
                                                    editingComment = null
                                                    commentText = ""
                                                },
                                                onError = { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.asString(context),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        } else {
                                            viewModel.addComment(
                                                eventId = eventId,
                                                text = commentText.trim(),
                                                onSuccess = { commentText = "" },
                                                onError = { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.asString(context),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    }
                                },
                                enabled = commentText.isNotBlank()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.label_post_comment),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Event Dialog confirmation
    if (showDeleteEventDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.label_delete_event),
            text = stringResource(R.string.label_delete_event_confirm),
            confirmButtonText = stringResource(R.string.label_delete),
            onConfirm = {
                showDeleteEventDialog = false
                viewModel.deleteEvent(
                    eventId = eventId,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            eventDeletedMsg,
                            Toast.LENGTH_SHORT
                        ).show()
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
            },
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismiss = { showDeleteEventDialog = false },
            isDestructive = true
        )
    }

    // Delete Comment confirmation
    showDeleteCommentDialog?.let { comment ->
        ConfirmationDialog(
            title = stringResource(R.string.label_delete_comment),
            text = stringResource(R.string.label_delete_comment_confirm),
            confirmButtonText = stringResource(R.string.label_delete),
            onConfirm = {
                showDeleteCommentDialog = null
                viewModel.deleteComment(
                    eventId = eventId,
                    commentId = comment.id,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            commentDeletedMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { err ->
                        Toast.makeText(
                            context,
                            err.asString(context),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismiss = { showDeleteCommentDialog = null },
            isDestructive = true
        )
    }

    // Full screen image viewer
    selectedPhotoForView?.let { photo ->
        BaseDialog(onDismissRequest = { selectedPhotoForView = null }) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (photo.uploadedBy == currentUserId || isHost) {
                        Button(
                            onClick = {
                                selectedPhotoForView = null
                                viewModel.deletePhoto(
                                    eventId = eventId,
                                    photoId = photo.id,
                                    storageUrl = photo.url,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            photoDeletedMsg,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(
                                            context,
                                            err.asString(context),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.label_delete))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    TextButton(onClick = { selectedPhotoForView = null }) {
                        Text(stringResource(R.string.label_close))
                    }
                }
            }
        }
    }

    // Guest Profile Dialogue view
    selectedFriendForProfile?.let { friend ->
        PublicProfileDialog(
            friend = friend,
            onDismiss = { selectedFriendForProfile = null },
            onViewCalendar = {}
        )
    }
}

@Composable
private fun StatusSummaryItem(
    label: String,
    count: Int,
    color: Color,
    isChecked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) color.copy(alpha = 0.15f) else color.copy(alpha = 0.02f),
        border = BorderStroke(
            width = if (isChecked) 1.5.dp else 1.dp,
            color = if (isChecked) color.copy(alpha = 0.6f) else color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isChecked) color else color.copy(alpha = 0.4f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isChecked) color else color.copy(alpha = 0.4f)
            )
        }
    }
}
