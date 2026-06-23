package com.free2party.ui.screens.login

import android.app.Activity
import com.free2party.MainActivity
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.free2party.R
import com.free2party.data.model.ThemeMode
import com.free2party.ui.components.InputTextField
import com.free2party.ui.components.dialogs.EmailDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return@remember currentContext
            }
            currentContext = currentContext.baseContext
        }
        null
    }
    val coroutineScope = rememberCoroutineScope()
    val (showForgotPasswordDialog, setShowForgotPasswordDialog) = remember { mutableStateOf(false) }
    val serverClientId = stringResource(R.string.default_web_client_id)
    val googleConfigError = stringResource(R.string.error_google_config)
    val googleInvalidTokenError = stringResource(R.string.error_google_invalid_token)
    val googleUnexpectedResponseError = stringResource(R.string.error_google_unexpected_response)
    val googleSignInFailedError = stringResource(R.string.error_google_failed)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is LoginUiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.message.asString(context),
                        Toast.LENGTH_LONG
                    ).show()
                    setShowForgotPasswordDialog(false)
                }
            }
        }
    }

    val legacyGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.onGoogleSignIn(firebaseCredential, onLoginSuccess)
                } else {
                    Log.e("LoginScreen", "Legacy Google Sign-In: ID Token is null")
                    Toast.makeText(context, googleSignInFailedError, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
            } catch (e: ApiException) {
                Log.e("LoginScreen", "Legacy Google Sign-In failed to parse result", e)
                Toast.makeText(context, googleSignInFailedError, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
        } else {
            viewModel.resetState()
        }
    }

    val launchLegacyGoogleSignIn = {
        coroutineScope.launch {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(serverClientId)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(activity ?: context, gso)
            try {
                googleSignInClient.signOut().await()
            } catch (e: Exception) {
                Log.w("LoginScreen", "launchLegacyGoogleSignIn: Sign-out failed (non-fatal)", e)
            }

            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                mainActivity.launchGoogleSignIn(googleSignInClient.signInIntent) { data, resultCode ->
                    if (resultCode == Activity.RESULT_OK) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                        try {
                            val account = task.getResult(ApiException::class.java)
                            val idToken = account.idToken
                            if (idToken != null) {
                                val firebaseCredential =
                                    GoogleAuthProvider.getCredential(idToken, null)
                                viewModel.onGoogleSignIn(firebaseCredential, onLoginSuccess)
                            } else {
                                Log.e(
                                    "LoginScreen",
                                    "MainActivity delegate legacy Google Sign-In: ID Token is null"
                                )
                                Toast.makeText(context, googleSignInFailedError, Toast.LENGTH_SHORT)
                                    .show()
                                viewModel.resetState()
                            }
                        } catch (e: ApiException) {
                            Log.e(
                                "LoginScreen",
                                "MainActivity delegate legacy Google Sign-In failed to parse result",
                                e
                            )
                            Toast.makeText(context, googleSignInFailedError, Toast.LENGTH_SHORT)
                                .show()
                            viewModel.resetState()
                        }
                    } else {
                        viewModel.resetState()
                    }
                }
            } else {
                legacyGoogleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    val currentLaunchLegacyGoogleSignIn by rememberUpdatedState(launchLegacyGoogleSignIn)

    val onGoogleSignInClick: () -> Unit = {
        // Modern Google Sign-In with Credential Manager
        if (serverClientId.isEmpty()) {
            Toast.makeText(
                context,
                googleConfigError,
                Toast.LENGTH_LONG
            ).show()
        } else if (viewModel.useLegacyGoogleSignIn) {
            viewModel.setGoogleSignInLoading()
            currentLaunchLegacyGoogleSignIn()
        } else {
            val credentialManager = CredentialManager.create(context)

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            viewModel.setGoogleSignInLoading()

            coroutineScope.launch {
                try {
                    val targetContext = activity ?: context
                    val result = kotlinx.coroutines.withTimeout(3000.milliseconds) {
                        credentialManager.getCredential(
                            context = targetContext,
                            request = request
                        )
                    }

                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        try {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            val firebaseCredential = GoogleAuthProvider.getCredential(
                                googleIdTokenCredential.idToken,
                                null
                            )
                            viewModel.onGoogleSignIn(firebaseCredential, onLoginSuccess)
                        } catch (e: GoogleIdTokenParsingException) {
                            Log.e("LoginScreen", "Received an invalid google id token response", e)
                            Toast.makeText(
                                context,
                                googleInvalidTokenError,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetState()
                        }
                    } else {
                        Log.e("LoginScreen", "Unexpected credential type: ${credential.type}")
                        Toast.makeText(
                            context,
                            googleUnexpectedResponseError,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetState()
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.e(
                        "LoginScreen",
                        "Google Sign-In timed out waiting for credential manager. Falling back to legacy Google Sign-In and saving preference.",
                        e
                    )
                    viewModel.saveUseLegacyGoogleSignInPreference(true)
                    currentLaunchLegacyGoogleSignIn()
                } catch (_: GetCredentialCancellationException) {
                    viewModel.resetState()
                } catch (e: NoCredentialException) {
                    Log.e(
                        "LoginScreen",
                        "No Google accounts available. Falling back to legacy Google Sign-In and saving preference.",
                        e
                    )
                    viewModel.saveUseLegacyGoogleSignInPreference(true)
                    currentLaunchLegacyGoogleSignIn()
                } catch (e: GetCredentialException) {
                    Log.e(
                        "LoginScreen",
                        "Google Sign-In failed. Falling back to legacy Google Sign-In and saving preference.",
                        e
                    )
                    viewModel.saveUseLegacyGoogleSignInPreference(true)
                    currentLaunchLegacyGoogleSignIn()
                } catch (e: Exception) {
                    Log.e(
                        "LoginScreen",
                        "An unexpected error occurred during Google Sign-In. Falling back to legacy Google Sign-In and saving preference.",
                        e
                    )
                    viewModel.saveUseLegacyGoogleSignInPreference(true)
                    currentLaunchLegacyGoogleSignIn()
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
        onResendVerificationClick = { viewModel.onResendVerificationClick() },
        onGoogleSignInClick = onGoogleSignInClick,
        onForgotPasswordClick = {
            viewModel.resetFields()
            setShowForgotPasswordDialog(true)
        },
        onForgotPasswordConfirm = { email -> viewModel.onForgotPasswordConfirm(email) },
        onDismissForgotPassword = {
            viewModel.resetFields()
            setShowForgotPasswordDialog(false)
        },
        onResetState = { viewModel.resetState() },
        onNavigateToRegister = {
            viewModel.resetFields()
            onNavigateToRegister()
        },
        gradientBackground = viewModel.gradientBackground,
        onSetGradientBackground = { viewModel.updateGradientBackground(it) }
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
    onResendVerificationClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onForgotPasswordConfirm: (String) -> Unit,
    onDismissForgotPassword: () -> Unit,
    onResetState: () -> Unit,
    onNavigateToRegister: () -> Unit,
    gradientBackground: Boolean,
    onSetGradientBackground: (Boolean) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        IconButton(
            onClick = { showThemeMenu = true },
            enabled = uiState !is LoginUiState.Loading,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Contrast,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = stringResource(R.string.title_appearance)
            )

            DropdownMenu(
                modifier = Modifier.padding(start = 8.dp),
                expanded = showThemeMenu,
                onDismissRequest = { showThemeMenu = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = stringResource(R.string.title_theme_mode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(stringResource(mode.labelResId)) },
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

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.title_background),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.option_gradient)) },
                    onClick = {
                        onSetGradientBackground(true)
                        showThemeMenu = false
                    },
                    trailingIcon = {
                        if (gradientBackground) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.option_solid)) },
                    onClick = {
                        onSetGradientBackground(false)
                        showThemeMenu = false
                    },
                    trailingIcon = {
                        if (!gradientBackground) {
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.free2party_full_foreground_color),
                contentDescription = stringResource(R.string.description_logo_content),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(50.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            InputTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.label_email),
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
                label = stringResource(R.string.label_password),
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
                    onDone = { keyboardController?.hide() }
                )
            )

            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End),
                enabled = uiState !is LoginUiState.Loading
            ) {
                Text(
                    text = stringResource(R.string.link_forgot_password),
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        if (uiState is LoginUiState.Loading) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.primary
                )
            }

            if (uiState is LoginUiState.Error) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.message.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )

                    if (uiState.isEmailNotVerified) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.error_email_not_verified_resend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.error_email_not_verified_resend_clickable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { onResendVerificationClick() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (uiState is LoginUiState.Loading && !showForgotPasswordDialog) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = isFormValid && uiState !is LoginUiState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gradientBackground) MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.7f
                        ) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.button_login))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = if (gradientBackground) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stringResource(R.string.label_or),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (gradientBackground) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = if (gradientBackground) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onGoogleSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = uiState !is LoginUiState.Loading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = if (gradientBackground) MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.7f
                        ) else Color.Transparent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.google_color),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.button_sign_in_with_google),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = uiState !is LoginUiState.Loading
                ) {
                    Text(
                        stringResource(R.string.link_sign_up),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        val (forgotPasswordEmail, setForgotPasswordEmail) = remember { mutableStateOf("") }
        EmailDialog(
            title = stringResource(R.string.title_reset_password),
            description = stringResource(R.string.description_reset_password),
            email = forgotPasswordEmail,
            onValueChange = {
                setForgotPasswordEmail(it)
                if (uiState is LoginUiState.Error) onResetState()
            },
            onDismiss = { onDismissForgotPassword() },
            onConfirm = { onForgotPasswordConfirm(forgotPasswordEmail) },
            isLoading = uiState is LoginUiState.Loading,
            errorMessage = if (uiState is LoginUiState.Error) uiState.message.asString() else null,
            confirmButtonLabel = stringResource(R.string.button_send_link)
        )
    }
}
