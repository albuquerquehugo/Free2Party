package com.free2party.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.theme.available
import com.free2party.ui.theme.availableContainer
import com.free2party.ui.theme.busy
import com.free2party.ui.theme.busyContainer
import com.free2party.ui.theme.onAvailableContainer
import com.free2party.ui.theme.onBusyContainer

@Composable
fun StatusToggleButton(
    isUserFree: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActionLoading: Boolean = false,
    enabled: Boolean = true
) {
    val glowColor =
        if (isUserFree) MaterialTheme.colorScheme.busy
        else MaterialTheme.colorScheme.available
    val containerColor =
        if (isUserFree) MaterialTheme.colorScheme.busyContainer
        else MaterialTheme.colorScheme.availableContainer
    val contentColor =
        if (isUserFree) MaterialTheme.colorScheme.onBusyContainer
        else MaterialTheme.colorScheme.onAvailableContainer

    ElevatedButton(
        onClick = onClick,
        enabled = enabled && !isActionLoading,
        modifier = modifier
            .height(64.dp)
            .wrapContentWidth()
            .shadow(
                elevation = if (isUserFree) 16.dp else 32.dp,
                spotColor = glowColor,
                shape = CircleShape,
                ambientColor = glowColor,
                clip = false
            ),
        shape = CircleShape,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = if (isActionLoading) 0f else 1f
                }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.free2party_full_foreground_color),
                    contentDescription = null,
                    modifier = Modifier.height(32.dp),
                    contentScale = ContentScale.Fit
                )
            }

            if (isActionLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            }
        }
    }
}
