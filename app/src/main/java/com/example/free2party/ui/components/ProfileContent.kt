package com.example.free2party.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.free2party.R
import com.example.free2party.data.model.Countries
import com.example.free2party.data.model.DatePattern
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
    var showCountryMenu by remember { mutableStateOf(false) }
    var showWhatsappCountryMenu by remember { mutableStateOf(false) }
    
    val selectedCountry = Countries.find { it.code == countryCode }
    val selectedWhatsappCountry = Countries.find { it.code == whatsappCountryCode }
    
    val phoneFocusRequester = remember { FocusRequester() }
    val whatsappFocusRequester = remember { FocusRequester() }

    val isBirthdayError = remember(birthday, datePattern) {
        birthday.isNotEmpty() && birthday.length == 8 && !isValidDateDigits(birthday, datePattern)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                            contentDescription = "Profile Picture",
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
                        contentDescription = "Edit Photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            InputTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = "First Name *",
                icon = Icons.Default.AccountCircle,
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
                value = lastName,
                onValueChange = onLastNameChange,
                label = "Last Name *",
                icon = Icons.Default.AccountCircle,
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
                value = email,
                onValueChange = onEmailChange,
                label = "Email *",
                icon = Icons.Default.Email,
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

            // TODO: Insert a search field on country selection
            InputTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    if (selectedCountry != null && newValue.length <= selectedCountry.digitsCount) {
                        onPhoneNumberChange(newValue.filter { it.isDigit() })
                    } else if (selectedCountry == null) {
                        onPhoneNumberChange(newValue.filter { it.isDigit() })
                    }
                },
                label = "Phone Number",
                placeholder = selectedCountry?.phoneMask ?: "Please select a country",
                icon = Icons.Default.Phone,
                focusRequester = phoneFocusRequester,
                leadingIconExtra = {
                    Box(modifier = Modifier.padding(start = 16.dp)) {
                        Row(
                            modifier = Modifier.clickable { showCountryMenu = true },
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
                                    contentDescription = "Select Country",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showCountryMenu,
                            onDismissRequest = { showCountryMenu = false }
                        ) {
                            Countries.forEach { country ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                country.flag,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(country.name)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "(${country.phoneCode})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                        }
                                    },
                                    onClick = {
                                        onCountryCodeChange(country.code)
                                        onPhoneNumberChange("") // Reset number when country changes
                                        showCountryMenu = false
                                        phoneFocusRequester.requestFocus()
                                    }
                                )
                            }
                        }
                    }
                },
                visualTransformation = PhoneVisualTransformation(selectedCountry?.phoneMask ?: ""),
                onClear = if (selectedCountry != null || phoneNumber.isNotEmpty()) {
                    {
                        onCountryCodeChange("")
                        onPhoneNumberChange("")
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
                label = "Birthday",
                placeholder = datePattern.label,
                icon = Icons.Default.Cake,
                isError = isBirthdayError,
                supportingText = if (isBirthdayError) {
                    { Text("Invalid date", color = MaterialTheme.colorScheme.error) }
                } else null,
                visualTransformation = DateVisualTransformation(datePattern),
                trailingIcon = {
                    IconButton(onClick = { setShowDatePicker(true) }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Birthday"
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
                label = "Bio",
                placeholder = "Write about yourself...",
                icon = Icons.AutoMirrored.Filled.Notes,
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

            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.CenterStart) {
                Text(text = "Socials", style = MaterialTheme.typography.titleSmall)
            }

            // WhatsApp Number Field
            InputTextField(
                value = whatsappNumber,
                onValueChange = { newValue ->
                    if (selectedWhatsappCountry != null && newValue.length <= selectedWhatsappCountry.digitsCount) {
                        onWhatsappNumberChange(newValue.filter { it.isDigit() })
                    } else if (selectedWhatsappCountry == null) {
                        onWhatsappNumberChange(newValue.filter { it.isDigit() })
                    }
                },
                label = "WhatsApp Number",
                placeholder = selectedWhatsappCountry?.phoneMask ?: "Please select a country",
                painter = painterResource(id = R.drawable.whatsapp),
                focusRequester = whatsappFocusRequester,
                leadingIconExtra = {
                    Box(modifier = Modifier.padding(start = 16.dp)) {
                        Row(
                            modifier = Modifier.clickable { showWhatsappCountryMenu = true },
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
                                    contentDescription = "Select Country",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showWhatsappCountryMenu,
                            onDismissRequest = { showWhatsappCountryMenu = false }
                        ) {
                            Countries.forEach { country ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                country.flag,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(country.name)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "(${country.phoneCode})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                        }
                                    },
                                    onClick = {
                                        onWhatsappCountryCodeChange(country.code)
                                        onWhatsappNumberChange("")
                                        showWhatsappCountryMenu = false
                                        whatsappFocusRequester.requestFocus()
                                    }
                                )
                            }
                        }
                    }
                },
                visualTransformation = PhoneVisualTransformation(selectedWhatsappCountry?.phoneMask ?: ""),
                onClear = if (selectedWhatsappCountry != null || whatsappNumber.isNotEmpty()) {
                    {
                        onWhatsappCountryCodeChange("")
                        onWhatsappNumberChange("")
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
                value = telegramUsername,
                onValueChange = onTelegramUsernameChange,
                label = "Telegram Username",
                painter = painterResource(id = R.drawable.telegram),
                prefix = { Text("@") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            InputTextField(
                value = facebookUsername,
                onValueChange = onFacebookUsernameChange,
                label = "Facebook Username",
                icon = Icons.Default.Facebook,
                prefix = { Text("@") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            InputTextField(
                value = instagramUsername,
                onValueChange = onInstagramUsernameChange,
                label = "Instagram Username",
                painter = painterResource(id = R.drawable.instagram),
                prefix = { Text("@") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            InputTextField(
                value = tiktokUsername,
                onValueChange = onTiktokUsernameChange,
                label = "TikTok Username",
                painter = painterResource(id = R.drawable.tiktok),
                prefix = { Text("@") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            InputTextField(
                value = xUsername,
                onValueChange = onXUsernameChange,
                label = "X Username",
                painter = painterResource(id = R.drawable.x),
                prefix = { Text("@") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
        }

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
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { setShowDatePicker(false) },
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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowDatePicker(false) }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}
