package com.free2party.ui.screens.events

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.free2party.R
import com.free2party.data.model.*
import com.free2party.ui.components.TopBar
import com.free2party.ui.components.dialogs.BaseDialog
import com.free2party.ui.components.dialogs.ConfirmationDialog
import com.free2party.ui.components.dialogs.PublicProfileDialog
import com.free2party.ui.theme.available
import com.free2party.ui.theme.busy
import com.free2party.util.formatPlanDateInFull
import com.free2party.util.formatTimeAgo
import com.free2party.util.formatTimeForDisplay
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    viewModel: EventsViewModel,
    eventId: String,
    onNavigateToEditEvent: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gradientBackground = viewModel.gradientBackground
    val currentUserId = viewModel.currentUserId
    val use24Hour = viewModel.use24HourFormat

    val photoUploadedMsg = stringResource(R.string.toast_photo_uploaded)
    val noMapAvailableMsg = stringResource(R.string.error_no_app_available)
    val eventDeletedMsg = stringResource(R.string.toast_event_deleted)

    val event by viewModel.currentEvent.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val photos by viewModel.photos.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var selectedFriendForProfile by remember { mutableStateOf<FriendInfo?>(null) }
    var selectedPhotoForView by remember { mutableStateOf<EventPhoto?>(null) }

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
        ev.guests[currentUserId]?.let { GuestStatus.valueOf(it) } ?: GuestStatus.INVITED
    }
    val isAccepted = isHost || myStatus == GuestStatus.ACCEPTED

    // Timezone translation details
    val tzMatch = ev.timezone == TimeZone.getDefault().id
    val timeFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(ev.timezone)
        }
    }
    val eventDateTime = remember(ev.startDate, ev.startTime) {
        runCatching { timeFormatter.parse("${ev.startDate} ${ev.startTime}") }.getOrNull()
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

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_event_details),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack,
                action = {
                    if (isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onNavigateToEditEvent(eventId) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.label_edit),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                        style = MaterialTheme.typography.headlineSmall,
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
                            text = stringResource(R.string.label_hosted_by, ev.hostName),
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!tzMatch) {
                                Text(
                                    text = "Your local time (Translated from original: ${
                                        formatPlanDateInFull(
                                            ev.startDate
                                        )
                                    } @ ${
                                        formatTimeForDisplay(
                                            ev.startTime,
                                            use24Hour
                                        )
                                    } ${ev.timezone})",
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
                }
            }

            // Location Card
            if (ev.locationName.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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
                            Column {
                                Text(
                                    text = stringResource(R.string.label_location),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = ev.locationName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (ev.latitude != null && ev.longitude != null) {
                            Button(
                                onClick = {
                                    val gmmIntentUri = "geo:${ev.latitude},${ev.longitude}?q=${
                                        Uri.encode(ev.locationName)
                                    }".toUri()
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
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

            // Useful Links Card
            if (ev.usefulLinks.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_useful_links),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        link.title,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
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

            // RSVP Response (if not host)
            if (!isHost) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_rsvp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
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
                                    containerColor = if (myStatus == GuestStatus.ACCEPTED) MaterialTheme.colorScheme.available else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (myStatus == GuestStatus.ACCEPTED) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.label_accept))
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
                                    containerColor = if (myStatus == GuestStatus.DECLINED) MaterialTheme.colorScheme.busy else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (myStatus == GuestStatus.DECLINED) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.label_decline))
                            }
                        }
                    }
                }
            }

            // Guests list Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_guests),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (ev.guests.isEmpty()) {
                        Text(
                            text = "No guests invited yet",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ev.guests.entries.toList()) { entry ->
                                val uid = entry.key
                                val status = GuestStatus.valueOf(entry.value)
                                val statusColor = when (status) {
                                    GuestStatus.ACCEPTED -> MaterialTheme.colorScheme.available
                                    GuestStatus.DECLINED -> MaterialTheme.colorScheme.busy
                                    GuestStatus.INVITED -> MaterialTheme.colorScheme.outline
                                }
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            selectedFriendForProfile = FriendInfo(
                                                uid = uid,
                                                name = "Guest Profile", // Will load real name via dialog viewmodel
                                                isFreeNow = status == GuestStatus.ACCEPTED
                                            )
                                        }
                                        .width(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .border(2.dp, statusColor, CircleShape)
                                                .padding(2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = status.name.lowercase().replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase(LocalLocale.current.platformLocale)
                                                else it.toString()
                                            },
                                            fontSize = 10.sp,
                                            color = statusColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_photo_album),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
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
                                modifier = Modifier.fillMaxWidth(),
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
                    }
                }
            }

            // Comment Section (All invited users)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_comments),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (comments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.label_comments_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            comments.forEach { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // User profile
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
                                    Spacer(modifier = Modifier.width(10.dp))
                                    // Comment bubble
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    comment.userName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    formatTimeAgo(comment.createdAt).asString(),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(comment.text, fontSize = 13.sp)
                                        }
                                    }
                                    // Delete comment if owner or host
                                    if (comment.userId == currentUserId || isHost) {
                                        IconButton(
                                            onClick = { showDeleteCommentDialog = comment },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input to write comment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text(stringResource(R.string.placeholder_comment)) },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
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

            Spacer(modifier = Modifier.height(32.dp))
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
            title = "Delete Comment",
            text = "Are you sure you want to delete this comment?",
            confirmButtonText = stringResource(R.string.label_delete),
            onConfirm = {
                showDeleteCommentDialog = null
                viewModel.deleteComment(
                    eventId = eventId,
                    commentId = comment.id,
                    onSuccess = {
                        Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
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
                                            "Photo deleted",
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
                            Text("Delete")
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
