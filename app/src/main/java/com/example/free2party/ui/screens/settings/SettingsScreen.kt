package com.example.free2party.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.free2party.R
import com.example.free2party.data.model.DatePattern
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.data.model.User
import com.example.free2party.ui.components.TopBar
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsScreen(
        uiState = viewModel.uiState,
        currentThemeMode = viewModel.themeMode,
        gradientBackground = viewModel.gradientBackground,
        onBack = onBack,
        onSetThemeMode = { viewModel.updateThemeMode(it) },
        onSetGradientBackground = { viewModel.updateGradientBackground(it) },
        onUpdateSettings = { viewModel.updateSettings(it) }
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    currentThemeMode: ThemeMode,
    gradientBackground: Boolean,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetGradientBackground: (Boolean) -> Unit,
    onUpdateSettings: (User) -> Unit
) {
    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_your_settings),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack,
                enabled = uiState !is SettingsUiState.Loading
            )
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
                    Text(text = uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                }
            }

            is SettingsUiState.Success -> {
                SettingsScreenContent(
                    paddingValues = paddingValues,
                    user = uiState.user,
                    currentThemeMode = currentThemeMode,
                    gradientBackground = gradientBackground,
                    isSaving = uiState.isSaving,
                    onSetThemeMode = onSetThemeMode,
                    onSetGradientBackground = onSetGradientBackground,
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
    gradientBackground: Boolean,
    isSaving: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetGradientBackground: (Boolean) -> Unit,
    onUpdateSettings: (User) -> Unit
) {
    var use24HourFormat by remember(user.settings.use24HourFormat) {
        mutableStateOf(user.settings.use24HourFormat)
    }
    var datePattern by remember(user.settings.datePattern) {
        mutableStateOf(user.settings.datePattern)
    }

    val oldPatternString = stringResource(user.settings.datePattern.patternResId).replace("-", "")
    val newPatternString = stringResource(datePattern.patternResId).replace("-", "")

    val hasChanges = remember(user, use24HourFormat, datePattern) {
        use24HourFormat != user.settings.use24HourFormat ||
                datePattern != user.settings.datePattern
    }

    val cardColors = CardDefaults.cardColors(
        containerColor = if (gradientBackground) {
            MaterialTheme.colorScheme.surface.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section 1: Appearance (Auto-applied)
        Text(
            text = stringResource(R.string.title_appearance),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = cardColors
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.label_theme_mode),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            label = stringResource(mode.labelResId),
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
                    text = stringResource(R.string.label_background),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        label = stringResource(R.string.option_gradient),
                        selected = gradientBackground,
                        onClick = { onSetGradientBackground(true) },
                        enabled = !isSaving
                    )
                    SettingsOption(
                        label = stringResource(R.string.option_solid),
                        selected = !gradientBackground,
                        onClick = { onSetGradientBackground(false) },
                        enabled = !isSaving
                    )
                }
            }
        }

        // Section 2: Preferences (Requires Save)
        Text(
            text = stringResource(R.string.title_preferences),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = cardColors
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.label_time_format),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        label = stringResource(R.string.option_twenty_four_hour),
                        selected = use24HourFormat,
                        onClick = { use24HourFormat = true },
                        enabled = !isSaving
                    )
                    SettingsOption(
                        label = stringResource(R.string.option_am_pm),
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
                    text = stringResource(R.string.label_date_format),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            label = stringResource(pattern.labelResId),
                            selected = datePattern == pattern,
                            onClick = { datePattern = pattern },
                            enabled = !isSaving
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val oldPattern = user.settings.datePattern
                    val newPattern = datePattern

                    val updatedBirthday = if (oldPattern != newPattern && user.birthday.length == 8) {
                        // Convert birthday from old pattern digits to new pattern digits
                        val oldSdf = SimpleDateFormat(
                            oldPatternString,
                            Locale.getDefault()
                        ).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val newSdf = SimpleDateFormat(
                            newPatternString,
                            Locale.getDefault()
                        ).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }

                        runCatching {
                            val date = oldSdf.parse(user.birthday)
                            if (date != null) newSdf.format(date) else user.birthday
                        }.getOrDefault(user.birthday)
                    } else {
                        user.birthday
                    }

                    onUpdateSettings(
                        user.copy(
                            birthday = updatedBirthday,
                            settings = user.settings.copy(
                                use24HourFormat = use24HourFormat,
                                datePattern = datePattern,
                                themeMode = currentThemeMode,
                                gradientBackground = gradientBackground
                            )
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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
                    Text(
                        stringResource(R.string.button_save_settings),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
