package com.free2party.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.free2party.R

enum class ProfileAvatarSize(
    val size: Dp,
    val borderWidth: Dp,
    val innerPadding: Dp
) {
    SMALL(size = 40.dp, borderWidth = 2.dp, innerPadding = 4.dp),
    MEDIUM(size = 64.dp, borderWidth = 3.dp, innerPadding = 6.dp),
    LARGE(size = 100.dp, borderWidth = 4.dp, innerPadding = 8.dp)
}

@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    size: ProfileAvatarSize = ProfileAvatarSize.MEDIUM,
    profilePicUrl: String = "",
    statusColor: Color? = null,
    fallbackIconTint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    val baseModifier = modifier
        .size(size.size)
        .let {
            if (statusColor != null) {
                it
                    .border(size.borderWidth, statusColor, CircleShape)
                    .padding(size.innerPadding)
            } else {
                it
            }
        }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        if (profilePicUrl.isNotBlank()) {
            AsyncImage(
                model = profilePicUrl,
                contentDescription = contentDescription ?: stringResource(R.string.label_profile),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = contentDescription ?: stringResource(R.string.label_profile),
                tint = fallbackIconTint,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
