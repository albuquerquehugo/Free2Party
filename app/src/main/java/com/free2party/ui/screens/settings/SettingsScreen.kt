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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.data.model.DatePattern
import com.free2party.data.model.DistanceUnit
import com.free2party.data.model.User
import com.free2party.data.model.Circle
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.BirthdayVisibility
import com.free2party.data.model.BirthdayShowType
import com.free2party.data.model.PlanVisibility
import com.free2party.R
import com.free2party.ui.components.AppSettingsOption
import com.free2party.ui.components.AppSettingsCard
import com.free2party.ui.components.FriendSelector
import com.free2party.ui.components.TopBar
import com.free2party.ui.components.basic.AppFilledButton
import com.free2party.ui.components.basic.AppHorizontalDivider
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit
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
        circles = circles,
        onNavigateToOnboarding = onNavigateToOnboarding
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    gradientBackground: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: (User) -> Unit,
    friends: List<FriendInfo>,
    circles: List<Circle>,
    onNavigateToOnboarding: () -> Unit
) {
    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.label_settings),
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
                    isSaving = uiState.isSaving,
                    onUpdateSettings = onUpdateSettings,
                    friends = friends,
                    circles = circles,
                    onNavigateToOnboarding = onNavigateToOnboarding
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
    onUpdateSettings: (User) -> Unit,
    friends: List<FriendInfo>,
    circles: List<Circle>,
    onNavigateToOnboarding: () -> Unit
) {
    var use24HourFormat by remember(user.settings.use24HourFormat) {
        mutableStateOf(user.settings.use24HourFormat)
    }
    var datePattern by remember(user.settings.datePattern) {
        mutableStateOf(user.settings.datePattern)
    }
    var distanceUnit by remember(user.settings.distanceUnit) {
        mutableStateOf(user.settings.distanceUnit)
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

    val isPreferencesChanged = remember(user, use24HourFormat, datePattern, distanceUnit) {
        use24HourFormat != user.settings.use24HourFormat ||
                datePattern != user.settings.datePattern ||
                distanceUnit != user.settings.distanceUnit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
            .consumeWindowInsets(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section 1: Privacy (Requires Save)
            Text(
                text = stringResource(R.string.label_privacy),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            // Status Privacy
            AppSettingsCard {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.text_visibility_manual_free_status),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.label_visibility_discretion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppSettingsOption(
                    label = stringResource(R.string.label_everyone),
                    selected = manualStatusVisibility == PlanVisibility.EVERYONE,
                    onClick = { manualStatusVisibility = PlanVisibility.EVERYONE },
                    enabled = !isSaving
                )
                AppSettingsOption(
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
                            manualStatusFriendsSelection =
                                if (id in manualStatusFriendsSelection) {
                                    manualStatusFriendsSelection - id
                                } else manualStatusFriendsSelection + id
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
                AppSettingsOption(
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
                            manualStatusFriendsSelection =
                                if (id in manualStatusFriendsSelection) {
                                    manualStatusFriendsSelection - id
                                } else manualStatusFriendsSelection + id
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

            Spacer(modifier = Modifier.height(16.dp))

            // Birthday Privacy
            AppSettingsCard {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.text_visibility_birthday),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.label_visibility_discretion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppSettingsOption(
                    label = stringResource(R.string.label_everyone),
                    selected = birthdayVisibility == BirthdayVisibility.EVERYONE,
                    onClick = { birthdayVisibility = BirthdayVisibility.EVERYONE },
                    enabled = !isSaving
                )
                AppSettingsOption(
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
                AppSettingsOption(
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
                AppSettingsOption(
                    label = stringResource(R.string.label_nobody),
                    selected = birthdayVisibility == BirthdayVisibility.NOBODY,
                    onClick = { birthdayVisibility = BirthdayVisibility.NOBODY },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("birthday_visibility_nobody")
                )

                AnimatedVisibility(visible = birthdayVisibility != BirthdayVisibility.NOBODY) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        AppHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = stringResource(R.string.label_birthday_display_format),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        AppSettingsOption(
                            label = stringResource(R.string.option_birthday_full),
                            selected = birthdayShowType == BirthdayShowType.FULL,
                            onClick = { birthdayShowType = BirthdayShowType.FULL },
                            enabled = !isSaving
                        )
                        AppSettingsOption(
                            label = stringResource(R.string.option_birthday_day_month),
                            selected = birthdayShowType == BirthdayShowType.DAY_MONTH,
                            onClick = { birthdayShowType = BirthdayShowType.DAY_MONTH },
                            enabled = !isSaving
                        )
                    }
                }
            }

            // Section 2: Preferences (Requires Save)
            Text(
                text = stringResource(R.string.label_preferences),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            AppSettingsCard {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_time_format_colon),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppSettingsOption(
                            label = stringResource(R.string.option_time_twenty_four_hour),
                            selected = use24HourFormat,
                            onClick = { use24HourFormat = true },
                            enabled = !isSaving
                        )
                        AppSettingsOption(
                            label = stringResource(R.string.option_time_am_pm),
                            selected = !use24HourFormat,
                            onClick = { use24HourFormat = false },
                            enabled = !isSaving
                        )
                    }
                }

                AppHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_date_format_colon),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp)
                            .selectableGroup(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        DatePattern.entries.forEach { pattern ->
                            AppSettingsOption(
                                label = stringResource(pattern.labelResId),
                                selected = datePattern == pattern,
                                onClick = { datePattern = pattern },
                                enabled = !isSaving
                            )
                        }
                    }
                }

                AppHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_distance_unit_colon),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppSettingsOption(
                            label = stringResource(R.string.option_distance_unit_kilometers),
                            selected = distanceUnit == DistanceUnit.KILOMETERS,
                            onClick = { distanceUnit = DistanceUnit.KILOMETERS },
                            enabled = !isSaving
                        )
                        AppSettingsOption(
                            label = stringResource(R.string.option_distance_unit_miles),
                            selected = distanceUnit == DistanceUnit.MILES,
                            onClick = { distanceUnit = DistanceUnit.MILES },
                            enabled = !isSaving
                        )
                    }
                }
            }

            // Section 3: Tutorial
            Text(
                text = stringResource(R.string.label_onboarding),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            AppSettingsCard(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.text_replay_tutorial),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppFilledButton(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.label_replay_tutorial),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasChanges) {
                TextButton(
                    onClick = {
                        use24HourFormat = user.settings.use24HourFormat
                        datePattern = user.settings.datePattern
                        distanceUnit = user.settings.distanceUnit
                        manualStatusVisibility = user.manualStatusVisibility
                        manualStatusFriendsSelection = user.manualStatusFriendsSelection
                        birthdayVisibility = user.birthdayVisibility
                        birthdayFriendsSelection = user.birthdayFriendsSelection
                        birthdayShowType = user.birthdayShowType
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("discard_button"),
                    enabled = !isSaving
                ) {
                    Text(
                        text = stringResource(R.string.label_discard_changes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            AppFilledButton(
                onClick = {
                    val oldPattern = user.settings.datePattern
                    val newPattern = datePattern

                    val updatedBirthday =
                        if (oldPattern != newPattern && user.birthday.length == 8) {
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
                                distanceUnit = distanceUnit
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
                    .height(56.dp)
                    .testTag("save_button"),
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
}
