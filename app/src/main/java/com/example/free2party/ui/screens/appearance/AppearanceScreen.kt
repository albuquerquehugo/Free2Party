package com.example.free2party.ui.screens.appearance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.free2party.R
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.ui.components.TopBar
import com.example.free2party.ui.screens.settings.SettingsOption

@Composable
fun AppearanceRoute(
    onBack: () -> Unit
) {
    val viewModel: AppearanceViewModel = hiltViewModel()

    AppearanceScreen(
        currentThemeMode = viewModel.themeMode,
        gradientBackground = viewModel.gradientBackground,
        onBack = onBack,
        onSetThemeMode = { viewModel.updateThemeMode(it) },
        onSetGradientBackground = { viewModel.updateGradientBackground(it) }
    )
}

@Composable
fun AppearanceScreen(
    currentThemeMode: ThemeMode,
    gradientBackground: Boolean,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetGradientBackground: (Boolean) -> Unit
) {
    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_appearance),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.label_theme_mode),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text(
                        text = stringResource(R.string.label_background),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SettingsOption(
                            label = stringResource(R.string.option_gradient),
                            selected = gradientBackground,
                            onClick = { onSetGradientBackground(true) },
                            enabled = true
                        )
                        SettingsOption(
                            label = stringResource(R.string.option_solid),
                            selected = !gradientBackground,
                            onClick = { onSetGradientBackground(false) },
                            enabled = true
                        )
                    }
                }
            }
        }
    }
}
