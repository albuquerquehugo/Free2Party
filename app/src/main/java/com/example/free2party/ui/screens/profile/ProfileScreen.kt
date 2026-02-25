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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.data.model.User
import com.example.free2party.data.model.UserSocials
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
        onUpdateProfile = { viewModel.updateProfile(it) },
        onUploadImage = { viewModel.uploadProfilePicture(it) }
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onUpdateProfile: (User) -> Unit,
    onUploadImage: (Uri) -> Unit
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
                    user = uiState.user,
                    isSaving = uiState.isSaving,
                    isUploadingImage = uiState.isUploadingImage,
                    onUpdateProfile = onUpdateProfile,
                    onUploadImage = onUploadImage
                )
            }
        }
    }
}

@Composable
fun ProfileScreenContent(
    paddingValues: PaddingValues,
    user: User,
    isSaving: Boolean,
    isUploadingImage: Boolean,
    onUpdateProfile: (User) -> Unit,
    onUploadImage: (Uri) -> Unit
) {
    var editedFirstName by remember(user.firstName) { mutableStateOf(user.firstName) }
    var editedLastName by remember(user.lastName) { mutableStateOf(user.lastName) }
    var editedPhoneNumber by remember(user.phoneNumber) { mutableStateOf(user.phoneNumber) }
    var editedBirthday by remember(user.birthday) { mutableStateOf(user.birthday) }
    var editedBio by remember(user.bio) { mutableStateOf(user.bio) }
    var editedFacebookUsername by remember(user.socials.facebookUsername) { mutableStateOf(user.socials.facebookUsername) }
    var editedInstagramUsername by remember(user.socials.instagramUsername) { mutableStateOf(user.socials.instagramUsername) }
    var editedTiktokUsername by remember(user.socials.tiktokUsername) { mutableStateOf(user.socials.tiktokUsername) }
    var editedXUsername by remember(user.socials.xUsername) { mutableStateOf(user.socials.xUsername) }

    val hasChanges = remember(
        user,
        editedFirstName,
        editedLastName,
        editedPhoneNumber,
        editedBirthday,
        editedBio,
        editedFacebookUsername,
        editedInstagramUsername,
        editedTiktokUsername,
        editedXUsername
    ) {
        editedFirstName != user.firstName ||
                editedLastName != user.lastName ||
                editedPhoneNumber != user.phoneNumber ||
                editedBirthday != user.birthday ||
                editedBio != user.bio ||
                editedFacebookUsername != user.socials.facebookUsername ||
                editedInstagramUsername != user.socials.instagramUsername ||
                editedTiktokUsername != user.socials.tiktokUsername ||
                editedXUsername != user.socials.xUsername
    }

    Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
        ProfileContent(
            isLoading = isSaving || isUploadingImage,
            profilePicture = user.profilePicUrl,
            onProfilePicChange = { onUploadImage(it) },
            firstName = editedFirstName,
            onFirstNameChange = { editedFirstName = it },
            lastName = editedLastName,
            onLastNameChange = { editedLastName = it },
            isEmailEnabled = false,
            email = user.email,
            onEmailChange = {},
            phoneNumber = editedPhoneNumber,
            onPhoneNumberChange = { editedPhoneNumber = it },
            birthday = editedBirthday,
            onBirthdayChange = { editedBirthday = it },
            datePattern = user.settings.datePattern,
            bio = editedBio,
            onBioChange = { editedBio = it },
            facebookUsername = editedFacebookUsername,
            onFacebookUsernameChange = { editedFacebookUsername = it },
            instagramUsername = editedInstagramUsername,
            onInstagramUsernameChange = { editedInstagramUsername = it },
            tiktokUsername = editedTiktokUsername,
            onTiktokUsernameChange = { editedTiktokUsername = it },
            xUsername = editedXUsername,
            onXUsernameChange = { editedXUsername = it },
            confirmButtons = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasChanges) {
                        TextButton(
                            onClick = {
                                editedFirstName = user.firstName
                                editedLastName = user.lastName
                                editedPhoneNumber = user.phoneNumber
                                editedBirthday = user.birthday
                                editedFacebookUsername = user.socials.facebookUsername
                                editedInstagramUsername = user.socials.instagramUsername
                                editedTiktokUsername = user.socials.tiktokUsername
                                editedXUsername = user.socials.xUsername
                                editedBio = user.bio
                            },
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
                        onClick = {
                            onUpdateProfile(
                                user.copy(
                                    firstName = editedFirstName,
                                    lastName = editedLastName,
                                    phoneNumber = editedPhoneNumber,
                                    birthday = editedBirthday,
                                    socials = UserSocials(
                                        facebookUsername = editedFacebookUsername,
                                        instagramUsername = editedInstagramUsername,
                                        tiktokUsername = editedTiktokUsername,
                                        xUsername = editedXUsername
                                    ),
                                    bio = editedBio
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_button"),
                        enabled = hasChanges && !isSaving && editedFirstName.isNotBlank() && editedLastName.isNotBlank()
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
