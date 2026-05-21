package com.free2party.ui.screens.blocked

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.free2party.data.model.BlockedUser
import com.free2party.R
import com.free2party.ui.components.TopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BlockedUsersRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: BlockedUsersViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is BlockedUsersUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    BlockedUsersScreen(
        uiState = viewModel.uiState,
        gradientBackground = viewModel.gradientBackground,
        onBack = onBack,
        onUnblockUser = { viewModel.unblockUser(it) },
        onReportUser = { userId, reason -> viewModel.reportUser(userId, reason) }
    )
}

@Composable
fun BlockedUsersScreen(
    uiState: BlockedUsersUiState,
    gradientBackground: Boolean,
    onBack: () -> Unit,
    onUnblockUser: (String) -> Unit,
    onReportUser: (String, String) -> Unit
) {
    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_blocked_users),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack,
                enabled = uiState !is BlockedUsersUiState.Loading
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is BlockedUsersUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is BlockedUsersUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                }
            }

            is BlockedUsersUiState.Success -> {
                BlockedUsersContent(
                    paddingValues = paddingValues,
                    blockedUsers = uiState.blockedUsers,
                    onUnblockUser = onUnblockUser,
                    onReportUser = onReportUser
                )
            }
        }
    }
}

@Composable
private fun BlockedUsersContent(
    paddingValues: PaddingValues,
    blockedUsers: List<BlockedUser>,
    onUnblockUser: (String) -> Unit,
    onReportUser: (String, String) -> Unit
) {
    if (blockedUsers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.text_no_results_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(blockedUsers, key = { it.uid }) { user ->
                BlockedUserItem(
                    user = user,
                    onUnblock = { onUnblockUser(user.uid) },
                    onReport = { reason -> onReportUser(user.uid, reason) }
                )
            }
        }
    }
}

@Composable
private fun BlockedUserItem(
    user: BlockedUser,
    onUnblock: () -> Unit,
    onReport: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (showReportDialog, setShowReportDialog) = remember { mutableStateOf(false) }

    if (showReportDialog) {
        ReportUserDialog(
            onDismiss = { setShowReportDialog(false) },
            onReport = { reason ->
                onReport(reason)
                setShowReportDialog(false)
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (user.profilePicUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.profilePicUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (user.email.isNotBlank()) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.description_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_unblock)) },
                    onClick = {
                        onUnblock()
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_report)) },
                    onClick = {
                        setShowReportDialog(true)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReportUserDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val options = listOf(
        stringResource(R.string.label_report_reason_spam),
        stringResource(R.string.label_report_reason_harassment),
        stringResource(R.string.label_report_reason_inappropriate_content),
        stringResource(R.string.label_report_reason_impersonation),
        stringResource(R.string.label_report_reason_other)
    )
    var selectedOption by remember { mutableStateOf(options[0]) }
    var otherReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.title_report_user)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.text_report_user_reason),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                options.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (text == selectedOption),
                                onClick = { selectedOption = text },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedOption),
                            onClick = null // Selected by Row's selectable
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                if (selectedOption == options.last()) {
                    OutlinedTextField(
                        value = otherReason,
                        onValueChange = { otherReason = it },
                        placeholder = { Text(stringResource(R.string.placeholder_report_reason_other)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
            }
        },
        confirmButton = {
            val isOtherSelected = selectedOption == options.last()
            val isEnabled = !isOtherSelected || otherReason.isNotBlank()

            TextButton(
                onClick = {
                    val finalReason = if (isOtherSelected) otherReason else selectedOption
                    onReport(finalReason)
                },
                enabled = isEnabled
            ) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}
