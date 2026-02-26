package com.example.free2party.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.ui.components.ProfileContent
import com.example.free2party.ui.components.TopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.provideFactory()),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ProfileUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    if (event.navigateBack) {
                        onBack()
                    }
                }
            }
        }
    }

    ProfileScreen(
        uiState = viewModel.uiState,
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
        facebookUsername = viewModel.facebookUsername,
        onFacebookUsernameChange = { viewModel.facebookUsername = it },
        instagramUsername = viewModel.instagramUsername,
        onInstagramUsernameChange = { viewModel.instagramUsername = it },
        tiktokUsername = viewModel.tiktokUsername,
        onTiktokUsernameChange = { viewModel.tiktokUsername = it },
        xUsername = viewModel.xUsername,
        onXUsernameChange = { viewModel.xUsername = it },
        hasChanges = viewModel.hasChanges,
        isFormValid = viewModel.isFormValid,
        onDiscardChanges = { viewModel.discardChanges() },
        onUpdateProfile = { viewModel.updateProfile() }
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
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
    facebookUsername: String,
    onFacebookUsernameChange: (String) -> Unit,
    instagramUsername: String,
    onInstagramUsernameChange: (String) -> Unit,
    tiktokUsername: String,
    onTiktokUsernameChange: (String) -> Unit,
    xUsername: String,
    onXUsernameChange: (String) -> Unit,
    hasChanges: Boolean,
    isFormValid: Boolean,
    onDiscardChanges: () -> Unit,
    onUpdateProfile: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "Your Profile",
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
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
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
                    facebookUsername = facebookUsername,
                    onFacebookUsernameChange = onFacebookUsernameChange,
                    instagramUsername = instagramUsername,
                    onInstagramUsernameChange = onInstagramUsernameChange,
                    tiktokUsername = tiktokUsername,
                    onTiktokUsernameChange = onTiktokUsernameChange,
                    xUsername = xUsername,
                    onXUsernameChange = onXUsernameChange,
                    hasChanges = hasChanges,
                    isFormValid = isFormValid,
                    onDiscardChanges = onDiscardChanges,
                    onUpdateProfile = onUpdateProfile
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
    facebookUsername: String,
    onFacebookUsernameChange: (String) -> Unit,
    instagramUsername: String,
    onInstagramUsernameChange: (String) -> Unit,
    tiktokUsername: String,
    onTiktokUsernameChange: (String) -> Unit,
    xUsername: String,
    onXUsernameChange: (String) -> Unit,
    hasChanges: Boolean,
    isFormValid: Boolean,
    onDiscardChanges: () -> Unit,
    onUpdateProfile: () -> Unit
) {
    val user = uiState.user
    val isSaving = uiState.isSaving
    val isUploadingImage = uiState.isUploadingImage

    Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
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
            facebookUsername = facebookUsername,
            onFacebookUsernameChange = onFacebookUsernameChange,
            instagramUsername = instagramUsername,
            onInstagramUsernameChange = onInstagramUsernameChange,
            tiktokUsername = tiktokUsername,
            onTiktokUsernameChange = onTiktokUsernameChange,
            xUsername = xUsername,
            onXUsernameChange = onXUsernameChange,
            confirmButtons = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasChanges) {
                        TextButton(
                            onClick = onDiscardChanges,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("discard_button"),
                            enabled = !isSaving
                        ) {
                            Text(
                                text = "Discard Changes",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

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
                            Text("Save Changes", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        )
    }
}
