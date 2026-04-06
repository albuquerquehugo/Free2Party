package com.example.free2party.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.Countries
import com.example.free2party.data.model.DatePattern
import com.example.free2party.ui.components.dialogs.CountryPickerDialog
import com.example.free2party.util.DateVisualTransformation
import com.example.free2party.util.PhoneVisualTransformation
import com.example.free2party.util.isValidDateDigits
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    isLoading: Boolean,
    profilePicture: Any?,
    onProfilePicChange: (Uri) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    isEmailEnabled: Boolean = true,
    email: String,
    onEmailChange: (String) -> Unit,
    passwordField: @Composable (() -> Unit)? = null,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    whatsappCountryCode: String,
    onWhatsappCountryCodeChange: (String) -> Unit,
    whatsappNumber: String,
    onWhatsappNumberChange: (String) -> Unit,
    isWhatsappSameAsPhone: Boolean,
    onWhatsappSameAsPhoneChange: (Boolean) -> Unit,
    birthday: String,
    onBirthdayChange: (String) -> Unit,
    datePattern: DatePattern = DatePattern.YYYY_MM_DD,
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
    telegramUsername: String,
    onTelegramUsernameChange: (String) -> Unit,
    confirmButtons: @Composable (() -> Unit)? = null
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onProfilePicChange(it) }
    }
    val focusManager = LocalFocusManager.current
    val (showDatePicker, setShowDatePicker) = remember { mutableStateOf(false) }

    val (showCountryDialog, setShowCountryDialog) = remember { mutableStateOf(false) }
    val (showWhatsappCountryDialog, setShowWhatsappCountryDialog) = remember { mutableStateOf(false) }

    val selectedCountry = Countries.find { it.code == countryCode }
    val selectedWhatsappCountry = Countries.find { it.code == whatsappCountryCode }

    val phoneFocusRequester = remember { FocusRequester() }
    val whatsappFocusRequester = remember { FocusRequester() }

    val phoneInteractionSource = remember { MutableInteractionSource() }
    val isPhoneFocused by phoneInteractionSource.collectIsFocusedAsState()

    val whatsappInteractionSource = remember { MutableInteractionSource() }
    val isWhatsappFocused by whatsappInteractionSource.collectIsFocusedAsState()

    var wasPhoneFocused by remember { mutableStateOf(false) }
    var wasWhatsappFocused by remember { mutableStateOf(false) }

    val isPhoneValidInternal = remember(phoneNumber, countryCode) {
        val country = Countries.find { it.code == countryCode }
        phoneNumber.isEmpty() || (country != null && phoneNumber.length == country.digitsCount)
    }

    val isWhatsappValidInternal = remember(whatsappNumber, whatsappCountryCode) {
        val country = Countries.find { it.code == whatsappCountryCode }
        whatsappNumber.isEmpty() || (country != null && whatsappNumber.length == country.digitsCount)
    }

    LaunchedEffect(isPhoneFocused, isPhoneValidInternal) {
        if (isPhoneFocused) {
            wasPhoneFocused = true
        } else if (isPhoneValidInternal) {
            wasPhoneFocused = false
        }
    }

    LaunchedEffect(isWhatsappFocused, isWhatsappValidInternal) {
        if (isWhatsappFocused) {
            wasWhatsappFocused = true
        } else if (isWhatsappValidInternal) {
            wasWhatsappFocused = false
        }
    }

    LaunchedEffect(isWhatsappSameAsPhone) {
        if (isWhatsappSameAsPhone) {
            wasWhatsappFocused = false
        }
    }

    LaunchedEffect(phoneInteractionSource, selectedCountry) {
        phoneInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release && selectedCountry == null) {
                setShowCountryDialog(true)
            }
        }
    }

    LaunchedEffect(whatsappInteractionSource, selectedWhatsappCountry) {
        whatsappInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release && selectedWhatsappCountry == null) {
                setShowWhatsappCountryDialog(true)
            }
        }
    }

    LaunchedEffect(phoneNumber, countryCode, whatsappNumber, whatsappCountryCode) {
        val isSame = phoneNumber.isNotEmpty() &&
                phoneNumber == whatsappNumber &&
                countryCode == whatsappCountryCode
        if (isSame && !isWhatsappSameAsPhone) {
            onWhatsappSameAsPhoneChange(true)
        }
        
        if (phoneNumber.isEmpty() && countryCode.isEmpty() && isWhatsappSameAsPhone) {
            onWhatsappSameAsPhoneChange(false)
        }
    }

    val isBirthdayError = remember(birthday, datePattern) {
        birthday.isNotEmpty() && birthday.length == 8 && !isValidDateDigits(birthday, datePattern)
    }

    val isPhoneError = remember(phoneNumber, countryCode, isPhoneFocused, wasPhoneFocused) {
        if (isPhoneFocused || !wasPhoneFocused || phoneNumber.isEmpty()) false
        else {
            val country = Countries.find { it.code == countryCode }
            country != null && phoneNumber.length != country.digitsCount
        }
    }

    val isWhatsappError = remember(whatsappNumber, whatsappCountryCode, isWhatsappFocused, wasWhatsappFocused, isWhatsappSameAsPhone) {
        if (isWhatsappSameAsPhone || isWhatsappFocused || !wasWhatsappFocused || whatsappNumber.isEmpty()) false
        else {
            val country = Countries.find { it.code == whatsappCountryCode }
            country != null && whatsappNumber.length != country.digitsCount
        }
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .padding(top = 16.dp)
                .clickable(enabled = !isLoading) { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val hasImage = when (profilePicture) {
                    is String -> profilePicture.isNotBlank()
                    is Uri -> true
                    else -> false
                }
                if (hasImage) {
                    AsyncImage(
                        model = profilePicture,
                        contentDescription = stringResource(R.string.profile_picture_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.edit_photo_content_description),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        InputTextField(
            value = firstName.trim(),
            onValueChange = onFirstNameChange,
            label = stringResource(R.string.first_name_required),
            icon = Icons.Default.AccountCircle,
            modifier = Modifier.testTag("first_name_field"),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = lastName.trim(),
            onValueChange = onLastNameChange,
            label = stringResource(R.string.last_name_required),
            icon = Icons.Default.AccountCircle,
            modifier = Modifier.testTag("last_name_field"),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = email.trim(),
            onValueChange = onEmailChange,
            label = stringResource(R.string.email_required),
            icon = Icons.Default.Email,
            modifier = Modifier.testTag("email_field"),
            enabled = !isLoading && isEmailEnabled,
            showClearIcon = isEmailEnabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        passwordField?.invoke()

        val phoneLabelColor = if (phoneNumber.isEmpty() && !isPhoneFocused) {
            if (selectedCountry == null) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.error
            }
        } else {
            Color.Unspecified
        }

        InputTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                if (selectedCountry != null) {
                    if (newValue.length <= selectedCountry.digitsCount) {
                        val filtered = newValue.filter { it.isDigit() }
                        onPhoneNumberChange(filtered)
                        if (isWhatsappSameAsPhone) {
                            onWhatsappNumberChange(filtered)
                        }
                    }
                }
            },
            label = stringResource(R.string.phone_number),
            isError = isPhoneError,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedLabelColor = phoneLabelColor
            ),
            placeholder = selectedCountry?.phoneMask ?: stringResource(R.string.phone_mask_placeholder),
            placeholderColor =
                if (selectedCountry == null) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            icon = Icons.Default.Phone,
            modifier = Modifier.testTag("phone_field"),
            focusRequester = phoneFocusRequester,
            interactionSource = phoneInteractionSource,
            leadingIconExtra = {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    Row(
                        modifier = Modifier.clickable { setShowCountryDialog(true) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCountry != null) {
                            Text(
                                text = selectedCountry.flag,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedCountry.phoneCode,
                                style = MaterialTheme.typography.bodyMedium,
                                softWrap = false,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = stringResource(R.string.select_country),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            visualTransformation = PhoneVisualTransformation(selectedCountry?.phoneMask ?: ""),
            onClear = if (selectedCountry != null) {
                {
                    onCountryCodeChange("")
                    onPhoneNumberChange("")
                    if (isWhatsappSameAsPhone) {
                        onWhatsappSameAsPhoneChange(false)
                        onWhatsappCountryCodeChange("")
                        onWhatsappNumberChange("")
                    }
                }
            } else null,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = birthday.filter { it.isDigit() },
            onValueChange = { newValue ->
                if (newValue.length <= 8) {
                    onBirthdayChange(newValue)
                }
            },
            label = stringResource(R.string.birthday),
            placeholder = datePattern.label,
            icon = Icons.Default.Cake,
            modifier = Modifier.testTag("birthday_field"),
            isError = isBirthdayError,
            supportingText = if (isBirthdayError) {
                { Text(stringResource(R.string.error_invalid_date), color = MaterialTheme.colorScheme.error) }
            } else null,
            visualTransformation = DateVisualTransformation(datePattern),
            trailingIcon = {
                IconButton(onClick = { setShowDatePicker(true) }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.select_birthday)
                    )
                }
            },
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = bio,
            onValueChange = onBioChange,
            label = stringResource(R.string.bio),
            placeholder = stringResource(R.string.bio_placeholder),
            icon = Icons.AutoMirrored.Filled.Notes,
            modifier = Modifier.testTag("bio_field"),
            minLines = 1,
            maxLines = 5,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = stringResource(R.string.socials), style = MaterialTheme.typography.titleSmall)
        }

        val whatsappLabelColor = if (whatsappNumber.isEmpty() && !isWhatsappFocused) {
            if (selectedWhatsappCountry == null || isWhatsappSameAsPhone) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.error
            }
        } else {
            Color.Unspecified
        }

        InputTextField(
            value = whatsappNumber,
            onValueChange = { newValue ->
                if (selectedWhatsappCountry != null) {
                    if (newValue.length <= selectedWhatsappCountry.digitsCount) {
                        onWhatsappNumberChange(newValue.filter { it.isDigit() })
                    }
                }
            },
            label = stringResource(R.string.whatsapp_number),
            isError = isWhatsappError,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedLabelColor = whatsappLabelColor
            ),
            placeholder = selectedWhatsappCountry?.phoneMask ?: stringResource(R.string.phone_mask_placeholder),
            placeholderColor =
                if (selectedWhatsappCountry == null) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            painter = painterResource(id = R.drawable.whatsapp),
            modifier = Modifier.testTag("whatsapp_field"),
            focusRequester = whatsappFocusRequester,
            interactionSource = whatsappInteractionSource,
            leadingIconExtra = {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    Row(
                        modifier = Modifier.clickable(enabled = !isWhatsappSameAsPhone) { setShowWhatsappCountryDialog(true) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedWhatsappCountry != null) {
                            Text(
                                text = selectedWhatsappCountry.flag,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedWhatsappCountry.phoneCode,
                                style = MaterialTheme.typography.bodyMedium,
                                softWrap = false,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = stringResource(R.string.select_country),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (!isWhatsappSameAsPhone) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            supportingText = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isWhatsappSameAsPhone,
                            enabled = phoneNumber.isNotEmpty() && !isLoading,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                onWhatsappSameAsPhoneChange(checked)
                                if (checked) {
                                    onWhatsappCountryCodeChange(countryCode)
                                    onWhatsappNumberChange(phoneNumber)
                                } else {
                                    onWhatsappCountryCodeChange("")
                                    onWhatsappNumberChange("")
                                }
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isWhatsappSameAsPhone,
                        onCheckedChange = null,
                        enabled = phoneNumber.isNotEmpty() && !isLoading,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.whatsapp_same_as_phone_number),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (phoneNumber.isNotEmpty()) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            },
            visualTransformation = PhoneVisualTransformation(
                selectedWhatsappCountry?.phoneMask ?: ""
            ),
            onClear = if (selectedWhatsappCountry != null && !isWhatsappSameAsPhone) {
                {
                    onWhatsappCountryCodeChange("")
                    onWhatsappNumberChange("")
                }
            } else null,
            enabled = !isLoading && !isWhatsappSameAsPhone,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = telegramUsername.trim(),
            onValueChange = onTelegramUsernameChange,
            label = stringResource(R.string.telegram_username),
            painter = painterResource(id = R.drawable.telegram),
            modifier = Modifier.testTag("telegram_field"),
            prefix = { Text("@") },
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = facebookUsername.trim(),
            onValueChange = onFacebookUsernameChange,
            label = stringResource(R.string.facebook_username),
            icon = Icons.Default.Facebook,
            modifier = Modifier.testTag("facebook_field"),
            prefix = { Text("@") },
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = instagramUsername.trim(),
            onValueChange = onInstagramUsernameChange,
            label = stringResource(R.string.instagram_username),
            painter = painterResource(id = R.drawable.instagram),
            modifier = Modifier.testTag("instagram_field"),
            prefix = { Text("@") },
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = tiktokUsername.trim(),
            onValueChange = onTiktokUsernameChange,
            label = stringResource(R.string.tiktok_username),
            painter = painterResource(id = R.drawable.tiktok),
            modifier = Modifier.testTag("tiktok_field"),
            prefix = { Text("@") },
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        InputTextField(
            value = xUsername.trim(),
            onValueChange = onXUsernameChange,
            label = stringResource(R.string.x_username),
            painter = painterResource(id = R.drawable.x),
            modifier = Modifier.testTag("x_field"),
            prefix = { Text("@") },
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        if (confirmButtons != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                confirmButtons()
            }
        }

        Spacer(modifier = Modifier.imePadding())
    }

    if (showCountryDialog) {
        CountryPickerDialog(
            onDismissRequest = { setShowCountryDialog(false) },
            onCountrySelected = { country ->
                onCountryCodeChange(country.code)
                onPhoneNumberChange("")
                if (isWhatsappSameAsPhone) {
                    onWhatsappCountryCodeChange(country.code)
                    onWhatsappNumberChange("")
                }
                setShowCountryDialog(false)
                phoneFocusRequester.requestFocus()
            }
        )
    }

    if (showWhatsappCountryDialog) {
        CountryPickerDialog(
            onDismissRequest = { setShowWhatsappCountryDialog(false) },
            onCountrySelected = { country ->
                onWhatsappCountryCodeChange(country.code)
                onWhatsappNumberChange("")
                setShowWhatsappCountryDialog(false)
                whatsappFocusRequester.requestFocus()
            }
        )
    }

    if (showDatePicker) {
        val initialDateMillis = if (birthday.length == 8) {
            val sdf =
                SimpleDateFormat(datePattern.pattern.replace("-", ""), Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            runCatching { sdf.parse(birthday)?.time }.getOrNull()
        } else null

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        val dialogColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

        DatePickerDialog(
            onDismissRequest = { setShowDatePicker(false) },
            colors = DatePickerDefaults.colors(containerColor = dialogColor),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onBirthdayChange(sdf.format(Date(millis)))
                    }
                    setShowDatePicker(false)
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowDatePicker(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(containerColor = dialogColor),
                title = {
                    Text(
                        text = stringResource(R.string.select_birthday),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    }
}
