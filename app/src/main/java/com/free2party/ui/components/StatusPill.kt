package com.free2party.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.theme.availableContainer
import com.free2party.ui.theme.busyContainer
import com.free2party.ui.theme.onAvailableContainer
import com.free2party.ui.theme.onBusyContainer

@Composable
fun StatusPill(
    isUserFree: Boolean,
    text: String? = null
) {
    val pillContainerColor =
        if (isUserFree) MaterialTheme.colorScheme.availableContainer
        else MaterialTheme.colorScheme.busyContainer
    val pillContentColor =
        if (isUserFree) MaterialTheme.colorScheme.onAvailableContainer
        else MaterialTheme.colorScheme.onBusyContainer
    val pillText = text
        ?: stringResource(if (isUserFree) R.string.label_status_free else R.string.label_status_busy)

    Card(
        colors = CardDefaults.cardColors(containerColor = pillContainerColor),
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(
            text = pillText,
            color = pillContentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}
