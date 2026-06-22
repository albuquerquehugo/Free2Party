package com.free2party.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.free2party.R
import androidx.compose.foundation.layout.BoxScope

@Composable
fun TopBar(
    title: String? = null,
    color: Color = Color.Unspecified,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    enabled: Boolean = true,
    action: @Composable (BoxScope.() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showBackButton) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = onBack,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = color,
                        contentDescription = stringResource(R.string.label_back)
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.free2party_full_foreground_color),
                contentDescription = stringResource(R.string.description_logo_content),
                modifier = Modifier.fillMaxHeight(0.5f),
                contentScale = ContentScale.Fit
            )

            if (action != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    action()
                }
            }
        }
        if (!title.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
