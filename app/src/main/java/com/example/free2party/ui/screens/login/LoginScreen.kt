package com.example.free2party.ui.screens.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.free2party.R
import com.example.free2party.data.model.ThemeMode
import com.example.free2party.ui.components.InputTextField
import com.example.free2party.ui.components.dialogs.ForgotPasswordDialog
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val (showForgotPasswordDialog, setShowForgotPasswordDialog) = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is LoginUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    setShowForgotPasswordDialog(false)
                }
            }
        }
    }

    LoginScreen(
        uiState = viewModel.uiState,
        email = viewModel.email,
        password = viewModel.password,
        themeMode = viewModel.themeMode,
        isFormValid = viewModel.isFormValid,
        showForgotPasswordDialog = showForgotPasswordDialog,
        onEmailChange = {
            viewModel.email = it
            if (viewModel.uiState is LoginUiState.Error) viewModel.resetState()
        },
        onPasswordChange = {
            viewModel.password = it
            if (viewModel.uiState is LoginUiState.Error) viewModel.resetState()
        },
        onSetThemeMode = { viewModel.updateThemeMode(it) },
        onLoginClick = { viewModel.onLoginClick(onLoginSuccess) },
        onForgotPasswordClick = { setShowForgotPasswordDialog(true) },
        onForgotPasswordConfirm = { email -> viewModel.onForgotPasswordConfirm(email) },
        onDismissForgotPassword = {
            viewModel.resetState()
            setShowForgotPasswordDialog(false)
        },
        onResetState = { viewModel.resetState() },
        onNavigateToRegister = {
            viewModel.resetFields()
            onNavigateToRegister()
        }
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    email: String,
    password: String,
    themeMode: ThemeMode,
    isFormValid: Boolean,
    showForgotPasswordDialog: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onForgotPasswordConfirm: (String) -> Unit,
    onDismissForgotPassword: () -> Unit,
    onResetState: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = { showThemeMenu = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Contrast,
                contentDescription = "Theme Settings"
            )

            DropdownMenu(
                expanded = showThemeMenu,
                onDismissRequest = { showThemeMenu = false }
            ) {
                Text(
                    text = "Theme",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider()

                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label) },
                        onClick = {
                            onSetThemeMode(mode)
                            showThemeMenu = false
                        },
                        trailingIcon = {
                            if (themeMode == mode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.free2party_full_transparent_light),
                contentDescription = "Free2Party Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            InputTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Email",
                icon = Icons.Default.Email,
                enabled = uiState !is LoginUiState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InputTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                isPassword = true,
                passwordVisible = passwordVisible,
                changeVisibility = { passwordVisible = !passwordVisible },
                icon = Icons.Default.Lock,
                enabled = uiState !is LoginUiState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (isFormValid) onLoginClick() }
                )
            )

            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End),
                enabled = uiState !is LoginUiState.Loading
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState is LoginUiState.Error) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is LoginUiState.Loading && !showForgotPasswordDialog) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = isFormValid && uiState !is LoginUiState.Loading
                ) {
                    Text("Login")
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = uiState !is LoginUiState.Loading
                ) {
                    Text("Don't have an account? Register")
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            uiState = uiState,
            onDismiss = onDismissForgotPassword,
            onConfirm = onForgotPasswordConfirm,
            onResetState = onResetState
        )
    }
}
