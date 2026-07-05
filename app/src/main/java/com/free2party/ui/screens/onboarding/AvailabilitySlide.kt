package com.free2party.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.components.StatusToggleButton
import com.free2party.ui.components.StatusPill
import com.free2party.ui.components.ProfileAvatar
import com.free2party.ui.theme.available
import com.free2party.ui.theme.busy

@Composable
fun LiveAvailabilityTogglePreview() {
    var isUserFree by remember { mutableStateOf(false) }
    val statusColor =
        if (isUserFree) MaterialTheme.colorScheme.available else MaterialTheme.colorScheme.busy

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dynamic Instruction Text
                Text(
                    text = stringResource(
                        if (isUserFree) R.string.onboarding_text_make_busy
                        else R.string.onboarding_text_make_free
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Main Interactive Status Toggle Button (identical to home screen)
                StatusToggleButton(
                    isUserFree = isUserFree,
                    onClick = { isUserFree = !isUserFree },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // User Profile avatar at the bottom with dynamically changing status ring color
                ProfileAvatar(statusColor = statusColor)
                StatusPill(isUserFree = isUserFree)
            }
        }

        // Tutorial note text below the Card
        Text(
            text = stringResource(R.string.onboarding_text_availability_tutorial_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(top = 4.dp)
        )
    }
}
