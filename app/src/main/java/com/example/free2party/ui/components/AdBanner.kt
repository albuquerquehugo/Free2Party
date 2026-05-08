package com.example.free2party.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.free2party.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(
    modifier: Modifier = Modifier
) {
    // Google Test Ad Unit ID for Banners
    val testAdUnitId = "ca-app-pub-3940256099942544/6300978111"

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp) // Standard banner height
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = testAdUnitId
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )

            // "AD" indicator in the corner (only visible if ad fails to load or for debugging)
            Text(
                text = stringResource(R.string.label_ad),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 6.dp, top = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
