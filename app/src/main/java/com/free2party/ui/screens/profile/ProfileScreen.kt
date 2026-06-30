package com.free2party.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.free2party.R
import com.free2party.data.model.Membership
import com.free2party.ui.components.AdBanner
import com.free2party.ui.components.TopBar
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.components.dialogs.AboutDialog
import com.free2party.ui.components.dialogs.ConfirmationDialog
import com.free2party.ui.theme.available
import com.free2party.ui.theme.busy
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCircles: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToInterests: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ProfileUiEvent.Logout -> onLogout()
            }
        }
    }

    val gradientBackground =
        (viewModel.uiState as? ProfileUiState.Success)?.gradientBackground ?: true

    ProfileScreen(
        uiState = viewModel.uiState,
        gradientBackground = gradientBackground,
        onLogoutClick = { viewModel.logout(onLogout) },
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToBlockedUsers = onNavigateToBlockedUsers,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToCircles = onNavigateToCircles,
        onNavigateToAppearance = onNavigateToAppearance,
        onNavigateToInterests = onNavigateToInterests
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    gradientBackground: Boolean,
    onLogoutClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCircles: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToInterests: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (gradientBackground) Color.Transparent
                else MaterialTheme.colorScheme.surface
            )
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(
            showBackButton = false,
            color = MaterialTheme.colorScheme.onSurface,
            onBack = {}
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is ProfileUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.message.asString(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is ProfileUiState.Success -> {
                    val statusColor = if (uiState.isUserFree) {
                        MaterialTheme.colorScheme.available
                    } else {
                        MaterialTheme.colorScheme.busy
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Centered Avatar with thick status border
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                                .border(4.dp, statusColor, CircleShape)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.profilePicUrl.isNotBlank()) {
                                AsyncImage(
                                    model = uiState.profilePicUrl,
                                    contentDescription = stringResource(R.string.label_profile),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.label_profile),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Display Name
                        Text(
                            text = uiState.userFullName.ifBlank { uiState.userName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Capsule/Pill
                        val statusText = if (uiState.isUserFree) {
                            stringResource(R.string.label_status_free)
                        } else {
                            stringResource(uiState.userGender.getStringRes(R.string.label_status_busy))
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = statusColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            // Menu items grouped inside a sleek Card
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(0.dp, shape = RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        MenuItem(
                                            icon = Icons.Default.Person,
                                            title = stringResource(R.string.label_edit_profile),
                                            onClick = onNavigateToProfile
                                        )
                                        AppHorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        MenuItem(
                                            icon = Icons.Default.Favorite,
                                            title = stringResource(R.string.label_interests),
                                            onClick = onNavigateToInterests
                                        )
                                        AppHorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        MenuItem(
                                            icon = Icons.Default.Groups,
                                            title = stringResource(R.string.label_circles),
                                            onClick = onNavigateToCircles
                                        )
                                        AppHorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        MenuItem(
                                            icon = Icons.Default.Block,
                                            title = stringResource(R.string.label_blocked_users),
                                            onClick = onNavigateToBlockedUsers
                                        )
                                        AppHorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        MenuItem(
                                            icon = Icons.Default.Palette,
                                            title = stringResource(R.string.label_appearance),
                                            onClick = onNavigateToAppearance
                                        )
                                        AppHorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        MenuItem(
                                            icon = Icons.Default.Settings,
                                            title = stringResource(R.string.label_settings),
                                            onClick = onNavigateToSettings
                                        )
                                        AppHorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        MenuItem(
                                            icon = Icons.Default.Info,
                                            title = stringResource(R.string.title_about),
                                            onClick = { showAboutDialog = true }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Destructive Action: Logout
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(0.dp, shape = RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    MenuItem(
                                        icon = Icons.AutoMirrored.Filled.Logout,
                                        title = stringResource(R.string.label_logout),
                                        tint = MaterialTheme.colorScheme.error,
                                        onClick = { showLogoutDialog = true }
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Ads Banner at the bottom
                        if (uiState.membership == Membership.REGULAR) {
                            AdBanner()
                        }
                    }
                }
            }
        }

        if (showLogoutDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.label_logout),
                text = stringResource(R.string.text_logout_confirmation),
                confirmButtonText = stringResource(R.string.label_logout),
                onConfirm = {
                    showLogoutDialog = false
                    onLogoutClick()
                },
                dismissButtonText = stringResource(R.string.label_cancel),
                onDismiss = { showLogoutDialog = false },
                isDestructive = true
            )
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = tint.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}
