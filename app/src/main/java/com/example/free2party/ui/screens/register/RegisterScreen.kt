package com.example.free2party.ui.screens.register

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.free2party.R
import com.example.free2party.data.model.Gender
import com.example.free2party.ui.components.InputTextField
import com.example.free2party.ui.components.ProfileContent
import com.example.free2party.ui.components.TopBar

@Composable
fun RegisterRoute(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    val accountCreatedTemplate = stringResource(R.string.success_account_created)


    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            Toast.makeText(
                context,
                accountCreatedTemplate.format(viewModel.email),
                Toast.LENGTH_LONG
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
        confirmPassword = viewModel.confirmPassword,
        countryCode = viewModel.countryCode,
        phoneNumber = viewModel.phoneNumber,
        birthday = viewModel.birthday,
        bio = viewModel.bio,
        gender = viewModel.gender,
        whatsappCountryCode = viewModel.whatsappCountryCode,
        whatsappNumber = viewModel.whatsappNumber,
        isWhatsappSameAsPhone = viewModel.isWhatsappSameAsPhone,
        telegramUsername = viewModel.telegramUsername,
        facebookUsername = viewModel.facebookUsername,
        instagramUsername = viewModel.instagramUsername,
        tiktokUsername = viewModel.tiktokUsername,
        xUsername = viewModel.xUsername,
        profilePicUri = viewModel.profilePicUri,
        isFormValid = viewModel.isFormValid(context),
        onFirstNameChange = { viewModel.firstName = it },
        onLastNameChange = { viewModel.lastName = it },
        onEmailChange = { viewModel.email = it },
        onPasswordChange = { viewModel.password = it },
        onConfirmPasswordChange = { viewModel.confirmPassword = it },
        onCountryCodeChange = { viewModel.countryCode = it },
        onPhoneNumberChange = { viewModel.phoneNumber = it },
        onBirthdayChange = { viewModel.birthday = it },
        onBioChange = { viewModel.bio = it },
        onWhatsappCountryCodeChange = { viewModel.whatsappCountryCode = it },
        onWhatsappNumberChange = { viewModel.whatsappNumber = it },
        onWhatsappSameAsPhoneChange = { viewModel.isWhatsappSameAsPhone = it },
        onTelegramUsernameChange = { viewModel.telegramUsername = it },
        onFacebookUsernameChange = { viewModel.facebookUsername = it },
        onInstagramUsernameChange = { viewModel.instagramUsername = it },
        onTiktokUsernameChange = { viewModel.tiktokUsername = it },
        onXUsernameChange = { viewModel.xUsername = it },
        onGenderChange = { viewModel.gender = it },
        onProfilePicChange = { viewModel.profilePicUri = it },
        onRegisterClick = { viewModel.onRegisterClick(context) },
        onBackToLogin = {
            viewModel.resetFields()
            onBackToLogin()
        },
        gradientBackground = viewModel.gradientBackground
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    confirmPassword: String,
    countryCode: String,
    phoneNumber: String,
    birthday: String,
    bio: String,
    gender: Gender,
    onGenderChange: (Gender) -> Unit,
    whatsappCountryCode: String,
    whatsappNumber: String,
    isWhatsappSameAsPhone: Boolean,
    telegramUsername: String,
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
    onConfirmPasswordChange: (String) -> Unit,
    onCountryCodeChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onWhatsappCountryCodeChange: (String) -> Unit,
    onWhatsappNumberChange: (String) -> Unit,
    onWhatsappSameAsPhoneChange: (Boolean) -> Unit,
    onTelegramUsernameChange: (String) -> Unit,
    onFacebookUsernameChange: (String) -> Unit,
    onInstagramUsernameChange: (String) -> Unit,
    onTiktokUsernameChange: (String) -> Unit,
    onXUsernameChange: (String) -> Unit,
    onProfilePicChange: (Uri) -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLogin: () -> Unit,
    gradientBackground: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val confirmPasswordInteractionSource = remember { MutableInteractionSource() }
    val isConfirmPasswordFocused by confirmPasswordInteractionSource.collectIsFocusedAsState()
    var wasConfirmPasswordFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isConfirmPasswordFocused) {
        if (isConfirmPasswordFocused) wasConfirmPasswordFocused = true
    }

    val isConfirmPasswordError =
        remember(password, confirmPassword, isConfirmPasswordFocused, wasConfirmPasswordFocused) {
            !isConfirmPasswordFocused && wasConfirmPasswordFocused && password != confirmPassword && confirmPassword.isNotEmpty()
        }

    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.title_create_account),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBackToLogin,
                enabled = uiState !is RegisterUiState.Loading
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .consumeWindowInsets(paddingValues)
        ) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InputTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = stringResource(R.string.password_required),
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
                        InputTextField(
                            value = confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            label = stringResource(R.string.confirm_password_required),
                            isPassword = true,
                            isError = isConfirmPasswordError,
                            passwordVisible = confirmPasswordVisible,
                            changeVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                            icon = Icons.Default.Lock,
                            interactionSource = confirmPasswordInteractionSource,
                            enabled = uiState !is RegisterUiState.Loading,
                            supportingText = if (isConfirmPasswordError) {
                                {
                                    Text(
                                        stringResource(R.string.error_passwords_not_match),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                },
                countryCode = countryCode,
                onCountryCodeChange = onCountryCodeChange,
                phoneNumber = phoneNumber,
                onPhoneNumberChange = onPhoneNumberChange,
                birthday = birthday,
                onBirthdayChange = onBirthdayChange,
                gender = gender,
                onGenderChange = onGenderChange,
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
                confirmButtons = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState is RegisterUiState.Error) {
                            Text(
                                text = uiState.message.asString(),
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
                                enabled = isFormValid,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (gradientBackground) MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.7f
                                        )
                                        else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    stringResource(R.string.button_register),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            TextButton(onClick = onBackToLogin) {
                                Text(
                                    stringResource(R.string.label_already_have_account_log_in),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            )
        }
    }
}
