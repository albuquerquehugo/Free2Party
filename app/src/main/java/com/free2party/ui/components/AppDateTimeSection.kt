package com.free2party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.components.basic.AppOutlinedCard

@Composable
fun AppDateTimeSection(
    startDateText: String,
    startTimeText: String,
    endDateText: String,
    endTimeText: String,
    isStartDateSelected: Boolean,
    isStartTimeSelected: Boolean,
    isEndDateSelected: Boolean,
    isEndTimeSelected: Boolean,
    isStartDateInPast: Boolean,
    isStartTimeInPast: Boolean,
    isDateTimeValid: Boolean,
    onStartDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClearEndClick: (() -> Unit)? = null,
    labelStyle: TextStyle = MaterialTheme.typography.titleMedium,
    cardTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    isEndDateTimeRequired: Boolean = false,
    pastErrorResId: Int = R.string.error_past_plan,
    hasInteractedWithStart: Boolean = false,
    cardTextColorNormal: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Start Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = stringResource(R.string.label_start_colon) + stringResource(R.string.label_required_field),
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(60.dp)
                    .padding(top = 12.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        onClick = onStartDateClick
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = startDateText,
                                style = cardTextStyle,
                                color = if (isStartDateInPast || !isDateTimeValid || (isStartTimeSelected && !isStartDateSelected)) MaterialTheme.colorScheme.error
                                else cardTextColorNormal
                            )
                        }
                    }
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(44.dp),
                        onClick = onStartTimeClick
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = startTimeText,
                                style = cardTextStyle,
                                color = if (isStartTimeInPast || !isDateTimeValid) MaterialTheme.colorScheme.error
                                else cardTextColorNormal
                            )
                        }
                    }
                }
                if (isStartTimeSelected && !isStartDateSelected) {
                    Text(
                        text = stringResource(R.string.error_start_date_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        textAlign = TextAlign.Start
                    )
                } else if (hasInteractedWithStart && !isStartDateSelected) {
                    Text(
                        text = stringResource(R.string.error_start_date_time_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        textAlign = TextAlign.Start
                    )
                } else if (isStartDateInPast || isStartTimeInPast) {
                    Text(
                        text = stringResource(pastErrorResId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // End Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = stringResource(R.string.label_end_colon) +
                        if (isEndDateTimeRequired) stringResource(R.string.label_required_field)
                        else "",
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(60.dp)
                    .padding(top = 12.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        onClick = onEndDateClick
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = endDateText,
                                style = cardTextStyle,
                                color = if (!isDateTimeValid || (isEndTimeSelected && !isEndDateSelected)) MaterialTheme.colorScheme.error
                                else cardTextColorNormal
                            )
                        }
                    }
                    AppOutlinedCard(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(44.dp),
                        onClick = onEndTimeClick
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = endTimeText,
                                style = cardTextStyle,
                                color = if (!isDateTimeValid) MaterialTheme.colorScheme.error
                                else cardTextColorNormal
                            )
                        }
                    }
                    if (onClearEndClick != null && isEndDateSelected) {
                        IconButton(
                            onClick = onClearEndClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear End Date",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (isEndTimeSelected && !isEndDateSelected) {
                    Text(
                        text = stringResource(R.string.error_end_date_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        textAlign = TextAlign.Start
                    )
                } else if (!isDateTimeValid && isStartDateSelected && isEndDateSelected) {
                    Text(
                        text = stringResource(R.string.error_invalid_datetime),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
