package com.free2party.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.free2party.R
import com.free2party.data.model.Countries
import com.free2party.data.model.FriendInfo
import com.free2party.data.model.User
import com.free2party.data.model.BirthdayVisibility
import com.free2party.data.repository.UserRepository
import com.free2party.ui.theme.TelegramColor
import com.free2party.ui.theme.WhatsAppColor
import com.free2party.ui.theme.available
import com.free2party.ui.theme.busy
import com.free2party.util.SocialPlatform
import com.free2party.util.openEmail
import com.free2party.util.openSMS
import com.free2party.util.openDialer
import com.free2party.util.openSocialMessage
import com.free2party.util.formatBirthday
import com.free2party.util.formatPhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _userId = MutableStateFlow<String?>(null)

    val currentUserId: String get() = userRepository.currentUserId

    fun setUserId(uid: String) {
        _userId.value = uid
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val userFlow: StateFlow<User?> = _userId.flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else userRepository.observeUser(uid)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}

@Composable
fun PublicProfileDialog(
    friend: FriendInfo,
    onDismiss: () -> Unit,
    onViewCalendar: () -> Unit
) {
    val viewModel: PublicProfileViewModel = hiltViewModel(
        key = "public_profile_${friend.uid}"
    )

    LaunchedEffect(friend.uid) {
        viewModel.setUserId(friend.uid)
    }

    val userState by viewModel.userFlow.collectAsState()
    val currentUserId = viewModel.currentUserId
    val context = LocalContext.current
    val email = userState?.email ?: friend.email
    val phoneNum = userState?.phoneNumber ?: friend.phoneNumber

    val showBirthday = remember(userState, currentUserId) {
        val user = userState ?: return@remember false
        if (user.birthday.isBlank()) return@remember false

        when (user.birthdayVisibility) {
            BirthdayVisibility.EVERYONE -> true
            BirthdayVisibility.EXCEPT -> currentUserId !in user.birthdayFriendsSelection
            BirthdayVisibility.ONLY -> currentUserId in user.birthdayFriendsSelection
            BirthdayVisibility.NOBODY -> false
        }
    }

    BaseDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Full Name
            val fullName = userState?.fullName ?: friend.name
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Avatar
            val statusColor = if (friend.isFreeNow) MaterialTheme.colorScheme.available
            else MaterialTheme.colorScheme.busy

            val picUrl = userState?.profilePicUrl ?: friend.profilePicUrl

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .border(3.dp, statusColor, CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (picUrl.isNotBlank()) {
                    AsyncImage(
                        model = picUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = statusColor.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Pill
            val statusText = if (friend.isFreeNow) {
                stringResource(R.string.label_status_free)
            } else {
                val baseRes = R.string.label_status_busy
                val genderedRes = userState?.gender?.getStringRes(baseRes) ?: baseRes
                stringResource(genderedRes)
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, statusColor.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onDismiss()
                    onViewCalendar()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.description_view_calendar),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.label_view_calendar),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Bio Card (if present)
            val bio = userState?.bio ?: ""
            if (bio.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.label_bio),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Birthday
                if (showBirthday && userState != null) {
                    val birthdayText =
                        formatBirthday(userState!!.birthday, userState!!.birthdayShowType)
                    ProfileDetailRow(
                        icon = Icons.Default.Cake,
                        text = birthdayText
                    )
                }

                // Phone (Call)
                val countryCode = userState?.countryCode ?: friend.phoneCode
                if (phoneNum.isNotBlank()) {
                    val country = Countries.find { it.code == countryCode }
                    val phoneText = if (country != null) {
                        "${country.flag}   ${formatPhoneNumber(phoneNum, country.phoneMask)}"
                    } else {
                        phoneNum
                    }
                    val rawDialNumber = if (country != null) {
                        "${country.phoneCode.filter { it.isDigit() || it == '+' }}$phoneNum"
                    } else {
                        phoneNum
                    }
                    ProfileDetailRow(
                        icon = Icons.Default.Phone,
                        text = phoneText,
                        contentDescription = "${stringResource(R.string.description_phone_number)}: $rawDialNumber",
                        onClick = { openDialer(context, rawDialNumber) }
                    )
                }
            }

            // Socials Row
            val socials = userState?.socials ?: friend.socials

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.label_send_message),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(
                    12.dp,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hangOutMessage = stringResource(R.string.text_hang_out_message)

                if (email.isNotBlank()) {
                    SocialIconButton(
                        painter = rememberVectorPainter(Icons.Default.Email),
                        contentDescription = stringResource(R.string.description_send_email),
                        tint = MaterialTheme.colorScheme.primary
                    ) {
                        openEmail(context, email)
                    }
                }
                if (phoneNum.isNotBlank()) {
                    val countryCode = userState?.countryCode ?: friend.phoneCode
                    val country = Countries.find { it.code == countryCode }
                    val rawSmsNumber = if (country != null) {
                        "${country.phoneCode.filter { it.isDigit() || it == '+' }}$phoneNum"
                    } else {
                        phoneNum
                    }
                    SocialIconButton(
                        painter = rememberVectorPainter(Icons.Default.Sms),
                        contentDescription = stringResource(R.string.description_send_sms),
                        tint = MaterialTheme.colorScheme.primary
                    ) {
                        openSMS(context, rawSmsNumber)
                    }
                }
                if (socials.whatsappFullNumber.isNotBlank()) {
                    SocialIconButton(
                        painter = painterResource(id = R.drawable.whatsapp),
                        contentDescription = stringResource(R.string.description_send_whatsapp_message),
                        tint = WhatsAppColor
                    ) {
                        openSocialMessage(
                            context,
                            SocialPlatform.WHATSAPP,
                            socials.whatsappFullNumber,
                            message = hangOutMessage
                        )
                    }
                }
                if (socials.telegramUsername.isNotBlank()) {
                    SocialIconButton(
                        painter = painterResource(id = R.drawable.telegram),
                        contentDescription = stringResource(R.string.description_send_telegram_message),
                        tint = TelegramColor
                    ) {
                        openSocialMessage(
                            context,
                            SocialPlatform.TELEGRAM,
                            socials.telegramUsername
                        )
                    }
                }
                if (socials.facebookUsername.isNotBlank()) {
                    SocialIconButton(
                        painter = painterResource(id = R.drawable.messenger_color),
                        contentDescription = stringResource(R.string.description_send_messenger_message)
                    ) {
                        openSocialMessage(
                            context,
                            SocialPlatform.MESSENGER,
                            socials.facebookUsername
                        )
                    }
                }
                if (socials.instagramUsername.isNotBlank()) {
                    SocialIconButton(
                        painter = painterResource(id = R.drawable.instagram_color),
                        contentDescription = stringResource(R.string.description_send_instagram_message)
                    ) {
                        openSocialMessage(
                            context,
                            SocialPlatform.INSTAGRAM,
                            socials.instagramUsername
                        )
                    }
                }
                if (socials.tiktokUsername.isNotBlank()) {
                    SocialIconButton(
                        painter = painterResource(id = R.drawable.tiktok_color),
                        contentDescription = stringResource(R.string.description_send_tiktok_message)
                    ) {
                        openSocialMessage(
                            context,
                            SocialPlatform.TIKTOK,
                            socials.tiktokUsername
                        )
                    }
                }
                if (socials.xUsername.isNotBlank()) {
                    SocialIconButton(
                        painter = painterResource(id = R.drawable.x),
                        contentDescription = stringResource(R.string.description_send_x_message)
                    ) {
                        openSocialMessage(context, SocialPlatform.X, socials.xUsername)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions row (Close button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val closeText = stringResource(R.string.label_close)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = closeText
                    }
                ) {
                    Text(closeText)
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    text: String,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SocialIconButton(
    painter: Painter,
    contentDescription: String,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(4.dp)
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
