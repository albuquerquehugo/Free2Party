package com.free2party.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.data.model.DatePattern
import com.free2party.data.model.User
import com.free2party.data.model.Circle
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.BirthdayVisibility
import com.free2party.data.model.BirthdayShowType
import com.free2party.data.model.PlanVisibility
import com.free2party.R
import com.free2party.ui.components.dialogs.FriendSelector
import com.free2party.ui.components.TopBar
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
    val friends by viewModel.friendsList.collectAsState()
    val circles by viewModel.circles.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SettingsUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    SettingsScreen(
        uiState = viewModel.uiState,
        gradientBackground = viewModel.gradientBackground,
        onBack = onBack,
        onUpdateSettings = { viewModel.updateSettings(it) },
        friends = friends,
        circles = circles
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    gradientBackground: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: (User) -> Unit,
    friends: List<FriendInfo>,
    circles: List<Circle>
) {
    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_settings),
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
                    gradientBackground = gradientBackground,
                    isSaving = uiState.isSaving,
                    onUpdateSettings = onUpdateSettings,
                    friends = friends,
                    circles = circles
                )
            }
        }
    }
}

@Composable
fun SettingsScreenContent(
    paddingValues: PaddingValues,
    user: User,
    gradientBackground: Boolean,
    isSaving: Boolean,
    onUpdateSettings: (User) -> Unit,
    friends: List<FriendInfo>,
    circles: List<Circle>
) {
    var use24HourFormat by remember(user.settings.use24HourFormat) {
        mutableStateOf(user.settings.use24HourFormat)
    }
    var datePattern by remember(user.settings.datePattern) {
        mutableStateOf(user.settings.datePattern)
    }
    var manualStatusVisibility by remember(user.manualStatusVisibility) {
        mutableStateOf(user.manualStatusVisibility)
    }
    var manualStatusFriendsSelection by remember(user.manualStatusFriendsSelection) {
        mutableStateOf(user.manualStatusFriendsSelection)
    }
    var birthdayVisibility by remember(user.birthdayVisibility) {
        mutableStateOf(user.birthdayVisibility)
    }
    var birthdayFriendsSelection by remember(user.birthdayFriendsSelection) {
        mutableStateOf(user.birthdayFriendsSelection)
    }
    var birthdayShowType by remember(user.birthdayShowType) {
        mutableStateOf(user.birthdayShowType)
    }

    LaunchedEffect(friends) {
        val currentFriendIds = friends.map { it.uid }.toSet()
        if (manualStatusFriendsSelection.any { it !in currentFriendIds }) {
            manualStatusFriendsSelection =
                manualStatusFriendsSelection.filter { it in currentFriendIds }
        }
        if (birthdayFriendsSelection.any { it !in currentFriendIds }) {
            birthdayFriendsSelection =
                birthdayFriendsSelection.filter { it in currentFriendIds }
        }
    }

    val oldPatternString = stringResource(user.settings.datePattern.patternResId).replace("-", "")
    val newPatternString = stringResource(datePattern.patternResId).replace("-", "")

    val isPrivacyChanged = remember(
        user,
        manualStatusVisibility,
        manualStatusFriendsSelection,
        birthdayVisibility,
        birthdayFriendsSelection,
        birthdayShowType
    ) {
        val manualStatusChanged = if (manualStatusVisibility != user.manualStatusVisibility) {
            true
        } else {
            when (manualStatusVisibility) {
                PlanVisibility.EVERYONE -> false
                PlanVisibility.EXCEPT, PlanVisibility.ONLY -> {
                    manualStatusFriendsSelection.toSet() != user.manualStatusFriendsSelection.toSet()
                }
            }
        }
        val birthdayChanged = if (birthdayVisibility != user.birthdayVisibility) {
            true
        } else if (birthdayShowType != user.birthdayShowType) {
            true
        } else {
            when (birthdayVisibility) {
                BirthdayVisibility.EVERYONE, BirthdayVisibility.NOBODY -> false
                BirthdayVisibility.EXCEPT, BirthdayVisibility.ONLY -> {
                    birthdayFriendsSelection.toSet() != user.birthdayFriendsSelection.toSet()
                }
            }
        }
        manualStatusChanged || birthdayChanged
    }

    val isPreferencesChanged = remember(user, use24HourFormat, datePattern) {
        use24HourFormat != user.settings.use24HourFormat || datePattern != user.settings.datePattern
    }

    val hasChanges = isPrivacyChanged || isPreferencesChanged

    val isVisibilityValid = remember(
        manualStatusVisibility,
        manualStatusFriendsSelection,
        birthdayVisibility,
        birthdayFriendsSelection
    ) {
        val isManualStatusValid = when (manualStatusVisibility) {
            PlanVisibility.EVERYONE -> true
            PlanVisibility.EXCEPT, PlanVisibility.ONLY -> manualStatusFriendsSelection.isNotEmpty()
        }
        val isBirthdayValid = when (birthdayVisibility) {
            BirthdayVisibility.EVERYONE, BirthdayVisibility.NOBODY -> true
            BirthdayVisibility.EXCEPT, BirthdayVisibility.ONLY -> birthdayFriendsSelection.isNotEmpty()
        }
        isManualStatusValid && isBirthdayValid
    }

    val cardColors = CardDefaults.cardColors(
        containerColor = if (gradientBackground) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
        // Section 1: Privacy (Requires Save)
        Text(
            text = stringResource(R.string.title_privacy),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.text_visibility_manual_free_status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.label_visibility_discretion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsOption(
                    label = stringResource(R.string.label_everyone),
                    selected = manualStatusVisibility == PlanVisibility.EVERYONE,
                    onClick = { manualStatusVisibility = PlanVisibility.EVERYONE },
                    enabled = !isSaving
                )
                SettingsOption(
                    label = stringResource(R.string.label_everyone_except_label),
                    selected = manualStatusVisibility == PlanVisibility.EXCEPT,
                    onClick = { manualStatusVisibility = PlanVisibility.EXCEPT },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("visibility_except")
                )
                AnimatedVisibility(visible = manualStatusVisibility == PlanVisibility.EXCEPT) {
                    FriendSelector(
                        friends = friends,
                        circles = circles,
                        selectedFriendIds = manualStatusFriendsSelection,
                        onToggleFriend = { id ->
                            manualStatusFriendsSelection = if (id in manualStatusFriendsSelection)
                                manualStatusFriendsSelection - id else manualStatusFriendsSelection + id
                        },
                        onAddFriends = { ids ->
                            manualStatusFriendsSelection =
                                (manualStatusFriendsSelection + ids).distinct()
                        },
                        onRemoveFriends = { ids ->
                            manualStatusFriendsSelection =
                                manualStatusFriendsSelection - ids.toSet()
                        },
                        onSelectAll = { manualStatusFriendsSelection = friends.map { it.uid } },
                        onUnselectAll = { manualStatusFriendsSelection = emptyList() }
                    )
                }
                SettingsOption(
                    label = stringResource(R.string.label_only_selected_people_label),
                    selected = manualStatusVisibility == PlanVisibility.ONLY,
                    onClick = { manualStatusVisibility = PlanVisibility.ONLY },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("visibility_only")
                )
                AnimatedVisibility(visible = manualStatusVisibility == PlanVisibility.ONLY) {
                    FriendSelector(
                        friends = friends,
                        circles = circles,
                        selectedFriendIds = manualStatusFriendsSelection,
                        onToggleFriend = { id ->
                            manualStatusFriendsSelection = if (id in manualStatusFriendsSelection)
                                manualStatusFriendsSelection - id else manualStatusFriendsSelection + id
                        },
                        onAddFriends = { ids ->
                            manualStatusFriendsSelection =
                                (manualStatusFriendsSelection + ids).distinct()
                        },
                        onRemoveFriends = { ids ->
                            manualStatusFriendsSelection =
                                manualStatusFriendsSelection - ids.toSet()
                        },
                        onSelectAll = { manualStatusFriendsSelection = friends.map { it.uid } },
                        onUnselectAll = { manualStatusFriendsSelection = emptyList() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.text_visibility_birthday),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.label_visibility_discretion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsOption(
                    label = stringResource(R.string.label_everyone),
                    selected = birthdayVisibility == BirthdayVisibility.EVERYONE,
                    onClick = { birthdayVisibility = BirthdayVisibility.EVERYONE },
                    enabled = !isSaving
                )
                SettingsOption(
                    label = stringResource(R.string.label_everyone_except_label),
                    selected = birthdayVisibility == BirthdayVisibility.EXCEPT,
                    onClick = { birthdayVisibility = BirthdayVisibility.EXCEPT },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("birthday_visibility_except")
                )
                AnimatedVisibility(visible = birthdayVisibility == BirthdayVisibility.EXCEPT) {
                    FriendSelector(
                        friends = friends,
                        circles = circles,
                        selectedFriendIds = birthdayFriendsSelection,
                        onToggleFriend = { id ->
                            birthdayFriendsSelection = if (id in birthdayFriendsSelection)
                                birthdayFriendsSelection - id else birthdayFriendsSelection + id
                        },
                        onAddFriends = { ids ->
                            birthdayFriendsSelection =
                                (birthdayFriendsSelection + ids).distinct()
                        },
                        onRemoveFriends = { ids ->
                            birthdayFriendsSelection =
                                birthdayFriendsSelection - ids.toSet()
                        },
                        onSelectAll = { birthdayFriendsSelection = friends.map { it.uid } },
                        onUnselectAll = { birthdayFriendsSelection = emptyList() }
                    )
                }
                SettingsOption(
                    label = stringResource(R.string.label_only_selected_people_label),
                    selected = birthdayVisibility == BirthdayVisibility.ONLY,
                    onClick = { birthdayVisibility = BirthdayVisibility.ONLY },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("birthday_visibility_only")
                )
                AnimatedVisibility(visible = birthdayVisibility == BirthdayVisibility.ONLY) {
                    FriendSelector(
                        friends = friends,
                        circles = circles,
                        selectedFriendIds = birthdayFriendsSelection,
                        onToggleFriend = { id ->
                            birthdayFriendsSelection = if (id in birthdayFriendsSelection)
                                birthdayFriendsSelection - id else birthdayFriendsSelection + id
                        },
                        onAddFriends = { ids ->
                            birthdayFriendsSelection =
                                (birthdayFriendsSelection + ids).distinct()
                        },
                        onRemoveFriends = { ids ->
                            birthdayFriendsSelection =
                                birthdayFriendsSelection - ids.toSet()
                        },
                        onSelectAll = { birthdayFriendsSelection = friends.map { it.uid } },
                        onUnselectAll = { birthdayFriendsSelection = emptyList() }
                    )
                }
                SettingsOption(
                    label = stringResource(R.string.label_nobody),
                    selected = birthdayVisibility == BirthdayVisibility.NOBODY,
                    onClick = { birthdayVisibility = BirthdayVisibility.NOBODY },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("birthday_visibility_nobody")
                )

                AnimatedVisibility(visible = birthdayVisibility != BirthdayVisibility.NOBODY) {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Text(
                            text = stringResource(R.string.label_birthday_display_format),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsOption(
                            label = stringResource(R.string.option_birthday_full),
                            selected = birthdayShowType == BirthdayShowType.FULL,
                            onClick = { birthdayShowType = BirthdayShowType.FULL },
                            enabled = !isSaving
                        )
                        SettingsOption(
                            label = stringResource(R.string.option_birthday_day_month),
                            selected = birthdayShowType == BirthdayShowType.DAY_MONTH,
                            onClick = { birthdayShowType = BirthdayShowType.DAY_MONTH },
                            enabled = !isSaving
                        )
                    }
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
                .padding(vertical = 16.dp)
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hasChanges) {
            TextButton(
                onClick = {
                    use24HourFormat = user.settings.use24HourFormat
                    datePattern = user.settings.datePattern
                    manualStatusVisibility = user.manualStatusVisibility
                    manualStatusFriendsSelection = user.manualStatusFriendsSelection
                    birthdayVisibility = user.birthdayVisibility
                    birthdayFriendsSelection = user.birthdayFriendsSelection
                    birthdayShowType = user.birthdayShowType
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .testTag("discard_button"),
                enabled = !isSaving
            ) {
                Text(
                    text = stringResource(R.string.label_discard_changes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                            datePattern = datePattern
                        ),
                        manualStatusVisibility = manualStatusVisibility,
                        manualStatusFriendsSelection = manualStatusFriendsSelection,
                        birthdayVisibility = birthdayVisibility,
                        birthdayFriendsSelection = birthdayFriendsSelection,
                        birthdayShowType = birthdayShowType
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .height(56.dp),
            enabled = hasChanges && isVisibilityValid && !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    stringResource(R.string.label_save_changes),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun SettingsOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .height(40.dp)
            .selectable(
                selected = selected,
                onClick = {
                    focusManager.clearFocus()
                    onClick()
                },
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
