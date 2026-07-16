package com.free2party.ui.screens.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.free2party.data.model.Membership
import com.free2party.data.model.ThemeMode
import com.free2party.ui.components.dialogs.AppColorPickerDialog
import com.free2party.ui.components.dialogs.AppEmojiPickerDialog
import com.free2party.ui.components.dialogs.PremiumDialog
import com.free2party.ui.components.ProfileAvatar
import com.free2party.ui.components.ProfileAvatarSize
import com.free2party.ui.theme.*
import com.free2party.R
import com.free2party.ui.components.AppSettingsCard
import com.free2party.ui.components.AppSettingsOption
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.components.basic.AppOutlinedButton
import com.free2party.ui.components.TopBar

@Composable
fun AppearanceRoute(
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val viewModel: AppearanceViewModel = hiltViewModel()

    AppearanceScreen(
        currentThemeMode = viewModel.themeMode,
        gradientBackground = viewModel.isGradientBackground,
        currentGradientTheme = viewModel.gradientTheme,
        currentMembership = viewModel.membership,
        statusEmoji = viewModel.statusEmoji,
        statusColor = viewModel.statusColor,
        profilePicUrl = viewModel.profilePicUrl,
        onStatusEmojiChange = { viewModel.updateStatusEmoji(it) },
        onStatusColorChange = { viewModel.updateStatusColor(it) },
        onBack = onBack,
        onSetThemeMode = { viewModel.updateThemeMode(it) },
        onSetGradientBackground = { viewModel.updateGradientBackground(it) },
        onSetGradientTheme = { viewModel.updateGradientTheme(it) },
        onNavigateToPremium = onNavigateToPremium
    )
}

@Composable
fun AppearanceScreen(
    currentThemeMode: ThemeMode,
    gradientBackground: Boolean,
    currentGradientTheme: String,
    currentMembership: Membership,
    statusEmoji: String,
    statusColor: String,
    profilePicUrl: String,
    onStatusEmojiChange: (String) -> Unit,
    onStatusColorChange: (String) -> Unit,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetGradientBackground: (Boolean) -> Unit,
    onSetGradientTheme: (String) -> Unit,
    onNavigateToPremium: () -> Unit
) {
    var showPremiumPrompt by remember { mutableStateOf(false) }

    if (showPremiumPrompt) {
        PremiumDialog(
            title = stringResource(R.string.label_premium_appearance_title),
            text = stringResource(R.string.text_premium_appearance_message),
            onConfirm = {
                showPremiumPrompt = false
                onNavigateToPremium()
            },
            onDismiss = {
                showPremiumPrompt = false
            }
        )
    }

    var showStatusPremiumPrompt by remember { mutableStateOf(false) }

    if (showStatusPremiumPrompt) {
        PremiumDialog(
            title = stringResource(R.string.label_premium_status_title),
            text = stringResource(R.string.text_premium_status_message),
            onConfirm = {
                showStatusPremiumPrompt = false
                onNavigateToPremium()
            },
            onDismiss = {
                showStatusPremiumPrompt = false
            }
        )
    }

    val isDark = when (currentThemeMode) {
        ThemeMode.AUTOMATIC -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.label_appearance),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Theme Mode
            AppSettingsCard {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.label_theme_mode_colon),
                    style = MaterialTheme.typography.titleMedium,
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
                        AppSettingsOption(
                            label = stringResource(mode.labelResId),
                            selected = currentThemeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            enabled = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Background
            AppSettingsCard {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.label_background_colon),
                    style = MaterialTheme.typography.titleMedium,
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
                    AppSettingsOption(
                        label = stringResource(R.string.option_background_gradient),
                        selected = gradientBackground,
                        onClick = { onSetGradientBackground(true) },
                        enabled = true
                    )
                    AppSettingsOption(
                        label = stringResource(R.string.option_background_solid),
                        selected = !gradientBackground,
                        onClick = { onSetGradientBackground(false) },
                        enabled = true
                    )
                }

                AnimatedVisibility(visible = gradientBackground) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        AppHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = stringResource(R.string.label_gradient_theme_colon),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val themes = listOf(
                            "DEFAULT" to R.string.option_gradient_theme_default,
                            "SUNSET" to R.string.option_gradient_theme_sunset,
                            "OCEAN" to R.string.option_gradient_theme_ocean,
                            "FOREST" to R.string.option_gradient_theme_forest,
                            "NEON" to R.string.option_gradient_theme_neon,
                            "MIDNIGHT" to R.string.option_gradient_theme_midnight
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            themes.forEach { (themeId, labelRes) ->
                                val isSelected = currentGradientTheme == themeId
                                val isPremium = themeId != "DEFAULT"
                                val isLocked = isPremium && currentMembership == Membership.FREE

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isLocked) {
                                                showPremiumPrompt = true
                                            } else {
                                                onSetGradientTheme(themeId)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.2f
                                            )
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val themeColors =
                                                GradientThemes.getThemeColors(themeId)
                                            val previewBaseColor =
                                                if (isDark) Color.Black else MaterialTheme.colorScheme.surface
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(previewBaseColor)
                                                    .drawBehind {
                                                        val slopeStop =
                                                            if (isDark) 0.4f else 0.8f
                                                        val glowAlpha =
                                                            if (isDark) 0.3f else 0.6f
                                                        val color1 = themeColors[0]
                                                        val color2 = themeColors[1]
                                                        val color3 = themeColors[2]
                                                        val color4 = themeColors[3]

                                                        // Top Left
                                                        drawCircle(
                                                            brush = Brush.radialGradient(
                                                                0.0f to color1.copy(alpha = glowAlpha),
                                                                slopeStop to Color.Transparent,
                                                                center = Offset(
                                                                    size.width * 0.2f,
                                                                    size.height * 0.3f
                                                                ),
                                                                radius = size.width * 1.4f
                                                            ),
                                                            radius = size.width * 1.4f,
                                                            center = Offset(
                                                                size.width * 0.2f,
                                                                size.height * 0.3f
                                                            )
                                                        )

                                                        // Top Right
                                                        drawCircle(
                                                            brush = Brush.radialGradient(
                                                                0.0f to color2.copy(alpha = glowAlpha),
                                                                slopeStop to Color.Transparent,
                                                                center = Offset(
                                                                    size.width * 0.9f,
                                                                    size.height * 0.1f
                                                                ),
                                                                radius = size.width * 0.8f
                                                            ),
                                                            radius = size.width * 0.8f,
                                                            center = Offset(
                                                                size.width * 0.9f,
                                                                size.height * 0.1f
                                                            )
                                                        )

                                                        // Bottom Left
                                                        drawCircle(
                                                            brush = Brush.radialGradient(
                                                                0.0f to color3.copy(alpha = glowAlpha),
                                                                slopeStop to Color.Transparent,
                                                                center = Offset(
                                                                    size.width * 0.1f,
                                                                    size.height * 0.9f
                                                                ),
                                                                radius = size.width * 1.4f
                                                            ),
                                                            radius = size.width * 1.4f,
                                                            center = Offset(
                                                                size.width * 0.1f,
                                                                size.height * 0.9f
                                                            )
                                                        )

                                                        // Bottom Right
                                                        drawCircle(
                                                            brush = Brush.radialGradient(
                                                                0.0f to color4.copy(alpha = glowAlpha),
                                                                slopeStop to Color.Transparent,
                                                                center = Offset(
                                                                    size.width * 0.8f,
                                                                    size.height * 0.6f
                                                                ),
                                                                radius = size.width * 0.8f
                                                            ),
                                                            radius = size.width * 0.8f,
                                                            center = Offset(
                                                                size.width * 0.8f,
                                                                size.height * 0.6f
                                                            )
                                                        )
                                                    }
                                                    .border(
                                                        0.5.dp,
                                                        MaterialTheme.colorScheme.outline.copy(
                                                            alpha = 0.3f
                                                        ),
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = stringResource(labelRes),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (isLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = stringResource(R.string.description_premium_only),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Preview
            AppSettingsCard(contentPadding = Modifier) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = stringResource(R.string.label_status_preview_colon),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Preview
                        val previewStatusColor =
                            if (statusColor.isNotBlank() && statusColor.startsWith("#")) {
                                try {
                                    Color(statusColor.toColorInt())
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.available
                                }
                            } else {
                                MaterialTheme.colorScheme.available
                            }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileAvatar(
                                size = ProfileAvatarSize.MEDIUM,
                                profilePicUrl = profilePicUrl,
                                statusColor = previewStatusColor,
                                statusColorHex = "",
                                statusEmoji = statusEmoji
                            )
                            Text(
                                text = stringResource(R.string.text_status_preview_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AppHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Emoji section
                        Text(
                            text = stringResource(R.string.label_status_emoji_colon),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = stringResource(R.string.text_status_emoji),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Custom Emoji picker state and dialog
                        val isCustomEmojiSelected = statusEmoji.isNotEmpty()
                        var showEmojiPicker by remember { mutableStateOf(false) }

                        if (showEmojiPicker) {
                            AppEmojiPickerDialog(
                                onEmojiSelected = { emoji ->
                                    showEmojiPicker = false
                                    onStatusEmojiChange(emoji)
                                },
                                onDismiss = { showEmojiPicker = false }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isDefaultEmojiSelected = statusEmoji.isEmpty()
                            // No emoji
                            AppOutlinedButton(
                                onClick = { onStatusEmojiChange("") },
                                border = BorderStroke(
                                    width = if (isDefaultEmojiSelected) 2.dp else 1.dp,
                                    color =
                                        if (isDefaultEmojiSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        )
                                ),
                                colors =
                                    if (isDefaultEmojiSelected) {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_none),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight =
                                            if (isDefaultEmojiSelected) FontWeight.Bold
                                            else FontWeight.Normal,
                                        color =
                                            if (isDefaultEmojiSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(
                                                width = 1.dp,
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.description_no_emoji),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Premium button
                            AppOutlinedButton(
                                onClick = {
                                    if (currentMembership == Membership.FREE) {
                                        showStatusPremiumPrompt = true
                                    } else showEmojiPicker = true
                                },
                                border = BorderStroke(
                                    width = if (!isDefaultEmojiSelected) 2.dp else 1.dp,
                                    color =
                                        if (!isDefaultEmojiSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        )
                                ),
                                colors = if (!isDefaultEmojiSelected) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_emoji),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight =
                                            if (!isDefaultEmojiSelected) FontWeight.Bold
                                            else FontWeight.Normal,
                                        color =
                                            if (!isDefaultEmojiSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color = MaterialTheme.colorScheme.surface)
                                            .border(
                                                width = if (isCustomEmojiSelected) 2.dp else 1.dp,
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = statusEmoji, fontSize = 18.sp)
                                    }
                                    if (currentMembership == Membership.FREE) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.description_premium_only),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AppHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Color section
                        Text(
                            text = stringResource(R.string.label_status_color_colon),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = stringResource(R.string.text_status_color),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Custom Color picker state and dialog
                        val isDefaultColorSelected = statusColor.isEmpty()
                        val customColorPreview =
                            if (!isDefaultColorSelected && statusColor.startsWith("#")) {
                                try {
                                    Color(statusColor.toColorInt())
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                            } else {
                                MaterialTheme.colorScheme.primary
                            }

                        var showColorPicker by remember { mutableStateOf(false) }

                        if (showColorPicker) {
                            AppColorPickerDialog(
                                initialColorHex = statusColor,
                                onColorSelected = { colorHex ->
                                    showColorPicker = false
                                    onStatusColorChange(colorHex)
                                },
                                onDismiss = { showColorPicker = false }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Default color
                            AppOutlinedButton(
                                onClick = { onStatusColorChange("") },
                                border = BorderStroke(
                                    width = if (isDefaultColorSelected) 2.dp else 1.dp,
                                    color =
                                        if (isDefaultColorSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        )
                                ),
                                colors =
                                    if (isDefaultColorSelected) {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Default color (Available Green)
                                    Text(
                                        text = stringResource(R.string.label_default),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight =
                                            if (isDefaultColorSelected) FontWeight.Bold
                                            else FontWeight.Normal,
                                        color =
                                            if (isDefaultColorSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.available)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            // Premium color button
                            AppOutlinedButton(
                                onClick = {
                                    if (currentMembership == Membership.FREE) {
                                        showStatusPremiumPrompt = true
                                    } else showColorPicker = true
                                },
                                border = BorderStroke(
                                    width = if (!isDefaultColorSelected) 2.dp else 1.dp,
                                    color =
                                        if (!isDefaultColorSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        )
                                ),
                                colors =
                                    if (!isDefaultColorSelected) {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_premium),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight =
                                            if (!isDefaultColorSelected) FontWeight.Bold
                                            else FontWeight.Normal,
                                        color =
                                            if (!isDefaultColorSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (!isDefaultColorSelected) customColorPreview else Color.Transparent)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                    )
                                    if (currentMembership == Membership.FREE) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.description_premium_only),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
