package com.example.free2party.ui.components.dialogs

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.free2party.data.model.Country
import com.example.free2party.data.model.Countries
import java.util.Locale

@Composable
fun CountryPickerDialog(
    onDismissRequest: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    val context = LocalContext.current
    val detectedCountryCode = remember {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val countryIso = telephonyManager?.networkCountryIso?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: telephonyManager?.simCountryIso?.uppercase()
                ?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country.uppercase()
        countryIso
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery, detectedCountryCode) {
        val baseList = Countries.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneCode.contains(searchQuery) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
        
        if (searchQuery.isEmpty()) {
            val detected = baseList.find { it.code == detectedCountryCode }
            if (detected != null) {
                listOf(detected) + (baseList - detected)
            } else {
                baseList
            }
        } else {
            baseList
        }
    }

    BaseDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(min = 400.dp, max = 600.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Select Country",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 8.dp)
            )

            OutlinedTextField(
                value = searchQuery.trim(),
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                placeholder = { Text("Search country or code...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredCountries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No results found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val suggestedCountry = if (searchQuery.isEmpty()) {
                        filteredCountries.firstOrNull()?.takeIf { it.code == detectedCountryCode }
                    } else null

                    if (suggestedCountry != null) {
                        item {
                            Text(
                                "Suggested",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        item {
                            CountryItem(suggestedCountry, onCountrySelected)
                        }
                        item {
                            Text(
                                "All Countries",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(filteredCountries.drop(1)) { country ->
                            CountryItem(country, onCountrySelected)
                        }
                    } else {
                        items(filteredCountries) { country ->
                            CountryItem(country, onCountrySelected)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: Country,
    onCountrySelected: (Country) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCountrySelected(country) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = country.flag, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = country.phoneCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
