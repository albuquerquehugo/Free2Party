package com.example.free2party.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.R
import com.example.free2party.data.model.User
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SettingsUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsScreen(
        uiState = viewModel.uiState,
        onBack = onBack,
        onUpdateSettings = { viewModel.updateSettings(it) }
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onUpdateSettings: (User) -> Unit
) {
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.logo_light_full_transparent),
                    contentDescription = "Free2Party Logo",
                    modifier = Modifier.height(20.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is SettingsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is SettingsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is SettingsUiState.Success -> {
                SettingsScreenContent(
                    paddingValues = paddingValues,
                    user = uiState.user,
                    isSaving = uiState.isSaving,
                    onUpdateSettings = onUpdateSettings
                )
            }
        }
    }
}

@Composable
fun SettingsScreenContent(
    paddingValues: PaddingValues,
    user: User,
    isSaving: Boolean,
    onUpdateSettings: (User) -> Unit
) {
    var use24HourFormat by remember(user.settings.use24HourFormat) {
        mutableStateOf(user.settings.use24HourFormat)
    }

    val hasChanges = remember(user, use24HourFormat) {
        use24HourFormat != user.settings.use24HourFormat
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Your Settings", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Time Format",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeFormatOption(
                        label = "24-hour",
                        selected = use24HourFormat,
                        onClick = { use24HourFormat = true },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    )
                    TimeFormatOption(
                        label = "AM/PM",
                        selected = !use24HourFormat,
                        onClick = { use24HourFormat = false },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                onUpdateSettings(
                    user.copy(settings = user.settings.copy(use24HourFormat = use24HourFormat))
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = hasChanges && !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Settings", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun TimeFormatOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
                enabled = enabled
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // null recommended for accessibility with selectable modifier
            enabled = enabled
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
