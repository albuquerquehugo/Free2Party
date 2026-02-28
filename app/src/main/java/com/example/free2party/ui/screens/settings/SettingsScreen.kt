package com.example.free2party.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.free2party.data.model.DatePattern
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.model.User
import com.example.free2party.ui.components.TopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
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
        currentThemeMode = viewModel.themeMode,
        onBack = onBack,
        onSetThemeMode = { viewModel.updateThemeMode(it) },
        onUpdateSettings = { viewModel.updateSettings(it) }
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    currentThemeMode: ThemeMode,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onUpdateSettings: (User) -> Unit
) {
    Scaffold(
        topBar = {
            TopBar("Your Settings", onBack = onBack, enabled = uiState !is SettingsUiState.Loading)
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
                    currentThemeMode = currentThemeMode,
                    isSaving = uiState.isSaving,
                    onSetThemeMode = onSetThemeMode,
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
    currentThemeMode: ThemeMode,
    isSaving: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onUpdateSettings: (User) -> Unit
) {
    var use24HourFormat by remember(user.settings.use24HourFormat) {
        mutableStateOf(user.settings.use24HourFormat)
    }
    var datePattern by remember(user.settings.datePattern) {
        mutableStateOf(user.settings.datePattern)
    }

    val hasChanges = remember(user, use24HourFormat, datePattern) {
        use24HourFormat != user.settings.use24HourFormat ||
                datePattern != user.settings.datePattern
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "App Theme:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .selectableGroup(),
                    horizontalAlignment = Alignment.Start
                ) {
                    ThemeMode.entries.forEach { mode ->
                        SettingsOption(
                            label = mode.label,
                            selected = currentThemeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            enabled = true
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Time Format:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsOption(
                        label = "24-hour",
                        selected = use24HourFormat,
                        onClick = { use24HourFormat = true },
                        enabled = !isSaving
                    )
                    SettingsOption(
                        label = "AM/PM",
                        selected = !use24HourFormat,
                        onClick = { use24HourFormat = false },
                        enabled = !isSaving
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date Format:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .selectableGroup(),
                    horizontalAlignment = Alignment.Start
                ) {
                    DatePattern.entries.forEach { pattern ->
                        SettingsOption(
                            label = pattern.label,
                            selected = datePattern == pattern,
                            onClick = { datePattern = pattern },
                            enabled = !isSaving
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onUpdateSettings(
                    user.copy(
                        settings = user.settings.copy(
                            use24HourFormat = use24HourFormat,
                            datePattern = datePattern
                        )
                    )
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
fun SettingsOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .height(40.dp)
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
            onClick = null,
            enabled = enabled
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
