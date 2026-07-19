package com.free2party.ui.screens.login

import android.app.Activity
import com.free2party.MainActivity
import android.content.ContextWrapper
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.free2party.R
import com.free2party.ui.components.dialogs.EmailDialog
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.components.basic.AppOutlinedTextField
import com.free2party.ui.components.basic.AppOutlinedButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.runtime.rememberUpdatedState
import com.free2party.ui.components.basic.AppFilledButton
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    val handleLegacyGoogleSignInResult = { resultCode: Int, data: android.content.Intent? ->
        if (resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.onGoogleSignIn(
                        credential = firebaseCredential,
                        onSuccess = onLoginSuccess
                    )
                } else {
                    viewModel.onLegacyGoogleSignInNullToken()
                }
            } catch (e: ApiException) {
                viewModel.onLegacyGoogleSignInApiException(e, isResultOk = true)
            }
        } else {
            // Result is not RESULT_OK. If there is intent data, check if there is an error status code.
            if (data != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (idToken != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        viewModel.onGoogleSignIn(
                            credential = firebaseCredential,
                            onSuccess = onLoginSuccess
                        )
                    } else {
                        viewModel.resetState()
                    }
                } catch (e: ApiException) {
                    viewModel.onLegacyGoogleSignInApiException(e, isResultOk = false)
                } catch (e: Exception) {
                    viewModel.onLegacyGoogleSignInUnexpectedException(e)
                }
            } else {
                viewModel.onLegacyGoogleSignInNullData(resultCode)
            }
        }
    }

    val legacyGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleLegacyGoogleSignInResult(result.resultCode, result.data)
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
                viewModel.onLegacyGoogleSignOutFailed(e)
            }

            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                mainActivity.launchGoogleSignIn(googleSignInClient.signInIntent) { data, resultCode ->
                    handleLegacyGoogleSignInResult(resultCode, data)
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
                    val result = credentialManager.getCredential(
                        context = targetContext,
                        request = request
                    )

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
                            viewModel.onGoogleSignIn(
                                credential = firebaseCredential,
                                onSuccess = onLoginSuccess
                            )
                        } catch (e: GoogleIdTokenParsingException) {
                            viewModel.onGoogleSignInError(e)
                        }
                    } else {
                        viewModel.onGoogleSignInUnexpectedCredentialType(credential.type)
                    }
                } catch (_: GetCredentialCancellationException) {
                    viewModel.resetState()
                } catch (e: Exception) {
                    viewModel.onGoogleSignInError(e)
                    currentLaunchLegacyGoogleSignIn()
                }
            }
        }
    }

    LoginScreen(
        uiState = viewModel.uiState,
        email = viewModel.email,
        password = viewModel.password,
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
        }
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    email: String,
    password: String,
    isFormValid: Boolean,
    showForgotPasswordDialog: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onResendVerificationClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onForgotPasswordConfirm: (String) -> Unit,
    onDismissForgotPassword: () -> Unit,
    onResetState: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Image(
                painter = painterResource(id = R.drawable.free2party_full_foreground_color),
                contentDescription = stringResource(R.string.description_logo_content),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(50.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(64.dp))

            AppOutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                labelText = stringResource(R.string.label_email),
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

            AppOutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                labelText = stringResource(R.string.label_password),
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

            if (uiState is LoginUiState.Error) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.message.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
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

            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 8.dp),
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

            if (uiState is LoginUiState.Loading && !showForgotPasswordDialog) {
                CircularProgressIndicator()
            } else {
                AppFilledButton(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid && uiState !is LoginUiState.Loading
                ) {
                    Text(stringResource(R.string.label_login))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppHorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.label_or),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                    AppHorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppOutlinedButton(
                    onClick = onGoogleSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState !is LoginUiState.Loading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
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
                            stringResource(R.string.label_sign_in_with_google),
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
            title = stringResource(R.string.label_reset_password),
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
            confirmButtonLabel = stringResource(R.string.label_send_link)
        )
    }
}
