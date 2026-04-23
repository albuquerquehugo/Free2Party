package com.example.free2party.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.R
import com.example.free2party.data.model.BlockedUser
import com.example.free2party.ui.components.ProfileContent
import com.example.free2party.ui.components.dialogs.ConfirmationDialog
import com.example.free2party.ui.components.TopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(context)
    )
    val deleteMsg = stringResource(R.string.account_deleted)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ProfileUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT)
                        .show()
                    if (event.navigateBack) {
                        onBack()
                    }
                }

                ProfileUiEvent.AccountDeleted -> {
                    Toast.makeText(context, deleteMsg, Toast.LENGTH_LONG).show()
                    onLogout()
                }
            }
        }
    }

    ProfileScreen(
        uiState = viewModel.uiState,
        gradientBackground = viewModel.gradientBackground,
        onBack = onBack,
        onUploadImage = { viewModel.uploadProfilePicture(it) },
        firstName = viewModel.firstName,
        onFirstNameChange = { viewModel.firstName = it },
        lastName = viewModel.lastName,
        onLastNameChange = { viewModel.lastName = it },
        countryCode = viewModel.countryCode,
        onCountryCodeChange = { viewModel.countryCode = it },
        phoneNumber = viewModel.phoneNumber,
        onPhoneNumberChange = { viewModel.phoneNumber = it },
        birthday = viewModel.birthday,
        onBirthdayChange = { viewModel.birthday = it },
        bio = viewModel.bio,
        onBioChange = { viewModel.bio = it },
        whatsappCountryCode = viewModel.whatsappCountryCode,
        onWhatsappCountryCodeChange = { viewModel.whatsappCountryCode = it },
        whatsappNumber = viewModel.whatsappNumber,
        onWhatsappNumberChange = { viewModel.whatsappNumber = it },
        isWhatsappSameAsPhone = viewModel.isWhatsappSameAsPhone,
        onWhatsappSameAsPhoneChange = { viewModel.onWhatsappSameAsPhoneChange(it) },
        telegramUsername = viewModel.telegramUsername,
        onTelegramUsernameChange = { viewModel.telegramUsername = it },
        facebookUsername = viewModel.facebookUsername,
        onFacebookUsernameChange = { viewModel.facebookUsername = it },
        instagramUsername = viewModel.instagramUsername,
        onInstagramUsernameChange = { viewModel.instagramUsername = it },
        tiktokUsername = viewModel.tiktokUsername,
        onTiktokUsernameChange = { viewModel.tiktokUsername = it },
        xUsername = viewModel.xUsername,
        onXUsernameChange = { viewModel.xUsername = it },
        onUnblockUser = { viewModel.unblockUser(it) },
        blockedUsers = (viewModel.uiState as? ProfileUiState.Success)?.blockedUsers ?: emptyList(),
        hasChanges = viewModel.hasChanges,
        isFormValid = viewModel.isFormValid(context),
        onDiscardChanges = { viewModel.discardChanges() },
        onUpdateProfile = { viewModel.updateProfile(context) },
        onDeleteAccount = { viewModel.deleteAccount() }
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    gradientBackground: Boolean,
    onBack: () -> Unit,
    onUploadImage: (Uri) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    birthday: String,
    onBirthdayChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    whatsappCountryCode: String,
    onWhatsappCountryCodeChange: (String) -> Unit,
    whatsappNumber: String,
    onWhatsappNumberChange: (String) -> Unit,
    isWhatsappSameAsPhone: Boolean,
    onWhatsappSameAsPhoneChange: (Boolean) -> Unit,
    telegramUsername: String,
    onTelegramUsernameChange: (String) -> Unit,
    facebookUsername: String,
    onFacebookUsernameChange: (String) -> Unit,
    instagramUsername: String,
    onInstagramUsernameChange: (String) -> Unit,
    tiktokUsername: String,
    onTiktokUsernameChange: (String) -> Unit,
    xUsername: String,
    onXUsernameChange: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    blockedUsers: List<BlockedUser>,
    hasChanges: Boolean,
    isFormValid: Boolean,
    onDiscardChanges: () -> Unit,
    onUpdateProfile: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_your_profile),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack,
                enabled = uiState !is ProfileUiState.Loading
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                }
            }

            is ProfileUiState.Success -> {
                ProfileScreenContent(
                    paddingValues = paddingValues,
                    uiState = uiState,
                    onUploadImage = onUploadImage,
                    firstName = firstName,
                    onFirstNameChange = onFirstNameChange,
                    lastName = lastName,
                    onLastNameChange = onLastNameChange,
                    countryCode = countryCode,
                    onCountryCodeChange = onCountryCodeChange,
                    phoneNumber = phoneNumber,
                    onPhoneNumberChange = onPhoneNumberChange,
                    birthday = birthday,
                    onBirthdayChange = onBirthdayChange,
                    bio = bio,
                    onBioChange = onBioChange,
                    whatsappCountryCode = whatsappCountryCode,
                    onWhatsappCountryCodeChange = onWhatsappCountryCodeChange,
                    whatsappNumber = whatsappNumber,
                    onWhatsappNumberChange = onWhatsappNumberChange,
                    isWhatsappSameAsPhone = isWhatsappSameAsPhone,
                    onWhatsappSameAsPhoneChange = onWhatsappSameAsPhoneChange,
                    telegramUsername = telegramUsername,
                    onTelegramUsernameChange = onTelegramUsernameChange,
                    facebookUsername = facebookUsername,
                    onFacebookUsernameChange = onFacebookUsernameChange,
                    instagramUsername = instagramUsername,
                    onInstagramUsernameChange = onInstagramUsernameChange,
                    tiktokUsername = tiktokUsername,
                    onTiktokUsernameChange = onTiktokUsernameChange,
                    xUsername = xUsername,
                    onXUsernameChange = onXUsernameChange,
                    onUnblockUser = onUnblockUser,
                    blockedUsers = blockedUsers,
                    hasChanges = hasChanges,
                    isFormValid = isFormValid,
                    onDiscardChanges = onDiscardChanges,
                    onUpdateProfile = onUpdateProfile,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }
    }
}

@Composable
fun ProfileScreenContent(
    paddingValues: PaddingValues,
    uiState: ProfileUiState.Success,
    onUploadImage: (Uri) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    birthday: String,
    onBirthdayChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    whatsappCountryCode: String,
    onWhatsappCountryCodeChange: (String) -> Unit,
    whatsappNumber: String,
    onWhatsappNumberChange: (String) -> Unit,
    isWhatsappSameAsPhone: Boolean,
    onWhatsappSameAsPhoneChange: (Boolean) -> Unit,
    telegramUsername: String,
    onTelegramUsernameChange: (String) -> Unit,
    facebookUsername: String,
    onFacebookUsernameChange: (String) -> Unit,
    instagramUsername: String,
    onInstagramUsernameChange: (String) -> Unit,
    tiktokUsername: String,
    onTiktokUsernameChange: (String) -> Unit,
    xUsername: String,
    onXUsernameChange: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    blockedUsers: List<BlockedUser>,
    hasChanges: Boolean,
    isFormValid: Boolean,
    onDiscardChanges: () -> Unit,
    onUpdateProfile: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val user = uiState.user
    val isSaving = uiState.isSaving
    val isUploadingImage = uiState.isUploadingImage
    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_account),
            text = stringResource(R.string.delete_account_confirmation_message),
            confirmButtonText = stringResource(R.string.text_delete),
            onConfirm = {
                setShowDeleteDialog(false)
                onDeleteAccount()
            },
            onDismiss = { setShowDeleteDialog(false) },
            isDestructive = true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
            .consumeWindowInsets(paddingValues)
    ) {
        ProfileContent(
            isLoading = isSaving || isUploadingImage,
            profilePicture = user.profilePicUrl,
            onProfilePicChange = { onUploadImage(it) },
            firstName = firstName,
            onFirstNameChange = onFirstNameChange,
            lastName = lastName,
            onLastNameChange = onLastNameChange,
            isEmailEnabled = false,
            email = user.email,
            onEmailChange = {},
            countryCode = countryCode,
            onCountryCodeChange = onCountryCodeChange,
            phoneNumber = phoneNumber,
            onPhoneNumberChange = onPhoneNumberChange,
            birthday = birthday,
            onBirthdayChange = onBirthdayChange,
            datePattern = user.settings.datePattern,
            bio = bio,
            onBioChange = onBioChange,
            whatsappCountryCode = whatsappCountryCode,
            onWhatsappCountryCodeChange = onWhatsappCountryCodeChange,
            whatsappNumber = whatsappNumber,
            onWhatsappNumberChange = onWhatsappNumberChange,
            isWhatsappSameAsPhone = isWhatsappSameAsPhone,
            onWhatsappSameAsPhoneChange = onWhatsappSameAsPhoneChange,
            telegramUsername = telegramUsername,
            onTelegramUsernameChange = onTelegramUsernameChange,
            facebookUsername = facebookUsername,
            onFacebookUsernameChange = onFacebookUsernameChange,
            instagramUsername = instagramUsername,
            onInstagramUsernameChange = onInstagramUsernameChange,
            tiktokUsername = tiktokUsername,
            onTiktokUsernameChange = onTiktokUsernameChange,
            xUsername = xUsername,
            onXUsernameChange = onXUsernameChange,
            blockedUsers = blockedUsers,
            onUnblockUser = onUnblockUser,
            confirmButtons = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (hasChanges) {
                        TextButton(
                            onClick = onDiscardChanges,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("discard_button"),
                            enabled = !isSaving
                        ) {
                            Text(
                                text = stringResource(R.string.discard_changes),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onUpdateProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_button"),
                        enabled = hasChanges && !isSaving && isFormValid
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                stringResource(R.string.save_changes),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { setShowDeleteDialog(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = stringResource(R.string.delete_account),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        )
    }
}
