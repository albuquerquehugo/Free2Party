package com.example.free2party.ui.screens.register

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.free2party.ui.components.InputTextField
import com.example.free2party.ui.components.ProfileContent
import com.example.free2party.ui.components.TopBar

@Composable
fun RegisterRoute(
    viewModel: RegisterViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            Toast.makeText(
                context,
                "Account created successfully for user ${viewModel.email}!",
                Toast.LENGTH_SHORT
            ).show()
            onRegisterSuccess()
        }
    }

    RegisterScreen(
        uiState = uiState,
        firstName = viewModel.firstName,
        lastName = viewModel.lastName,
        email = viewModel.email,
        password = viewModel.password,
        phoneNumber = viewModel.phoneNumber,
        birthday = viewModel.birthday,
        bio = viewModel.bio,
        facebookUsername = viewModel.facebookUsername,
        instagramUsername = viewModel.instagramUsername,
        tiktokUsername = viewModel.tiktokUsername,
        xUsername = viewModel.xUsername,
        profilePicUri = viewModel.profilePicUri,
        isFormValid = viewModel.isFormValid,
        onFirstNameChange = { viewModel.firstName = it },
        onLastNameChange = { viewModel.lastName = it },
        onEmailChange = { viewModel.email = it },
        onPasswordChange = { viewModel.password = it },
        onPhoneNumberChange = { viewModel.phoneNumber = it },
        onBirthdayChange = { viewModel.birthday = it },
        onBioChange = { viewModel.bio = it },
        onFacebookUsernameChange = { viewModel.facebookUsername = it },
        onInstagramUsernameChange = { viewModel.instagramUsername = it },
        onTiktokUsernameChange = { viewModel.tiktokUsername = it },
        onXUsernameChange = { viewModel.xUsername = it },
        onProfilePicChange = { viewModel.profilePicUri = it },
        onRegisterClick = { viewModel.onRegisterClick() },
        onBackToLogin = {
            viewModel.resetFields()
            onBackToLogin()
        }
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    phoneNumber: String,
    birthday: String,
    bio: String,
    facebookUsername: String,
    instagramUsername: String,
    tiktokUsername: String,
    xUsername: String,
    profilePicUri: Uri?,
    isFormValid: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onFacebookUsernameChange: (String) -> Unit,
    onInstagramUsernameChange: (String) -> Unit,
    onTiktokUsernameChange: (String) -> Unit,
    onXUsernameChange: (String) -> Unit,
    onProfilePicChange: (Uri) -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = "Create Account",
                onBack = onBackToLogin,
                enabled = uiState !is RegisterUiState.Loading
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            ProfileContent(
                isLoading = uiState is RegisterUiState.Loading,
                profilePicture = profilePicUri,
                onProfilePicChange = onProfilePicChange,
                firstName = firstName,
                onFirstNameChange = onFirstNameChange,
                lastName = lastName,
                onLastNameChange = onLastNameChange,
                email = email,
                onEmailChange = onEmailChange,
                passwordField = {
                    InputTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = "Password *",
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        changeVisibility = { passwordVisible = !passwordVisible },
                        icon = Icons.Default.Lock,
                        enabled = uiState !is RegisterUiState.Loading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        )
                    )
                },
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
                confirmButtons = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState is RegisterUiState.Error) {
                            Text(
                                text = uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (uiState is RegisterUiState.Loading) {
                            CircularProgressIndicator()
                        } else {
                            Button(
                                onClick = onRegisterClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = isFormValid
                            ) {
                                Text("Register", style = MaterialTheme.typography.titleMedium)
                            }

                            TextButton(onClick = onBackToLogin) {
                                Text("Already have an account? Login")
                            }
                        }
                    }
                }
            )
        }
    }
}
