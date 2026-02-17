package com.example.free2party.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.free2party.ui.screens.calendar.CalendarViewModel
import com.example.free2party.ui.theme.inactive
import com.example.free2party.ui.theme.onInactiveContainer
import com.example.free2party.util.isDateTimeInPast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun MonthCalendar(
    viewModel: CalendarViewModel,
    plannedDays: Set<Int>,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, viewModel.displayedYear)
        set(Calendar.MONTH, viewModel.displayedMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(calendar.time)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month navigation header
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { viewModel.moveToPreviousMonth() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                Text(text = monthName, style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = { viewModel.goToToday() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = { viewModel.moveToNextMonth() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next"
                )
            }
        }

        // Day of the week headers (S, M, T, W, T, F, S)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onInactiveContainer
                )
            }
        }

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.wrapContentHeight(),
            userScrollEnabled = false
        ) {
            // Add empty boxes for days before the 1st day of the month
            items(firstDayOfWeek) { Spacer(modifier = Modifier.fillMaxSize()) }

            items(daysInMonth) { index ->
                val day = index + 1
                val isPlanned = plannedDays.contains(day)

                val dateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.YEAR, viewModel.displayedYear)
                    set(Calendar.MONTH, viewModel.displayedMonth)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // Check if this specific cell is actually "Today"
                val today = Calendar.getInstance()
                val isToday = day == today.get(Calendar.DAY_OF_MONTH) &&
                        viewModel.displayedMonth == today.get(Calendar.MONTH) &&
                        viewModel.displayedYear == today.get(Calendar.YEAR)

                val isSelected = viewModel.selectedDateMillis?.let {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = it
                    cal.get(Calendar.DAY_OF_MONTH) == day &&
                            cal.get(Calendar.MONTH) == viewModel.displayedMonth &&
                            cal.get(Calendar.YEAR) == viewModel.displayedYear
                } ?: false

                Box(
                    modifier = Modifier
                        .aspectRatio(1.8f)
                        .padding(1.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.selectDate(day) },
                    contentAlignment = Alignment.Center
                ) {
                    // Highlight selection
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else if (isPlanned) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                        )
                    }

                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }

                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isPlanned -> MaterialTheme.colorScheme.onPrimaryContainer
                            isDateTimeInPast(dateMillis) && !isToday ->
                                MaterialTheme.colorScheme.inactive

                            else -> Color.Unspecified
                        }
                    )
                }
            }
        }
    }
}
