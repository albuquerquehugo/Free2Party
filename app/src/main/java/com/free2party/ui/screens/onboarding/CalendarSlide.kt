package com.free2party.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.components.BadgedIconContainer
import com.free2party.ui.components.DurationBadge
import com.free2party.ui.components.NumberBadge
import com.free2party.ui.components.basic.AppHorizontalDivider
import com.free2party.ui.theme.eventContainer
import com.free2party.ui.theme.onEventContainer
import com.free2party.ui.theme.onPlanContainer
import com.free2party.ui.theme.planContainer

@Composable
fun LiveCalendarPreview() {
    var selectedDay by remember { mutableIntStateOf(8) }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Symmetrical Top bar mock (Static Month Name)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.onboarding_mock_calendar_month),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Weekday initials
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                val daysOfWeek = listOf(
                    R.string.label_day_sunday_short,
                    R.string.label_day_monday_short,
                    R.string.label_day_tuesday_short,
                    R.string.label_day_wednesday_short,
                    R.string.label_day_thursday_short,
                    R.string.label_day_friday_short,
                    R.string.label_day_saturday_short
                )
                daysOfWeek.forEach { dayResId ->
                    Text(
                        text = stringResource(dayResId),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // 1-week compact calendar days grid (July 5 - July 11)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val week1 = listOf(5, 6, 7, 8, 9, 10, 11)
                Row(modifier = Modifier.fillMaxWidth()) {
                    week1.forEach { day ->
                        val isSelected = selectedDay == day
                        val isToday = day == 8
                        val isEvent = day == 8 || day == 10
                        val isPlan = day == 9 || day == 10

                        val cellContent = @Composable {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.2f)
                                    .clickable { selectedDay = day },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                } else {
                                    // Highlight/Indicator inner boundary (24.dp)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isPlan && isEvent -> Color.Transparent // drawn by Canvas below
                                                    isPlan -> MaterialTheme.colorScheme.planContainer
                                                    isEvent -> MaterialTheme.colorScheme.eventContainer
                                                    else -> Color.Transparent
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlan && isEvent) {
                                            val planColor =
                                                MaterialTheme.colorScheme.planContainer
                                            val eventColor =
                                                MaterialTheme.colorScheme.eventContainer
                                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(
                                                    color = planColor,
                                                    startAngle = 90f,
                                                    sweepAngle = 180f,
                                                    useCenter = true
                                                )
                                                drawArc(
                                                    color = eventColor,
                                                    startAngle = 270f,
                                                    sweepAngle = 180f,
                                                    useCenter = true
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline,
                                                CircleShape
                                            )
                                    )
                                }

                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isPlan && isEvent -> MaterialTheme.colorScheme.onSurface
                                        isPlan -> MaterialTheme.colorScheme.onPlanContainer
                                        isEvent -> MaterialTheme.colorScheme.onEventContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            when (day) {
                                9 -> BadgedIconContainer(number = 1) { cellContent() }
                                10 -> BadgedIconContainer(number = 2) { cellContent() }
                                else -> cellContent()
                            }
                        }
                    }
                }
            }

            // Center + add plan button exactly like CalendarScreen
            BadgedIconContainer(
                number = 3,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Results header at center
            Text(
                text = stringResource(
                    R.string.label_results_for,
                    stringResource(R.string.onboarding_mock_calendar_date, selectedDay)
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Results Cards Column (matching portrait height, scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val showEvent = selectedDay == 8 || selectedDay == 10
                val showPlan = selectedDay == 9 || selectedDay == 10

                if (!showEvent && !showPlan) {
                    // No Activities Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.text_no_activities_for_day),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (showEvent) {
                        // Replicate EventItem exactly from CalendarItem.kt
                        val isPizzaEvent = selectedDay == 10
                        val eventTitleRes =
                            if (isPizzaEvent) R.string.onboarding_mock_event_pizza_title else R.string.onboarding_mock_event_volleyball_title
                        val eventTimeRes =
                            if (isPizzaEvent) R.string.onboarding_mock_event_pizza_time else R.string.onboarding_mock_event_volleyball_time
                        val eventDurationRes =
                            if (isPizzaEvent) R.string.onboarding_mock_event_pizza_duration else R.string.onboarding_mock_event_volleyball_duration
                        val eventHostRes =
                            if (isPizzaEvent) R.string.label_you else R.string.onboarding_mock_event_host1
                        val eventLocationRes =
                            if (isPizzaEvent) R.string.onboarding_mock_event_pizza_location else R.string.onboarding_mock_event_volleyball_location
                        val eventDescriptionRes =
                            if (isPizzaEvent) R.string.onboarding_mock_event_pizza_description else R.string.onboarding_mock_event_volleyball_description

                        val eventColor = MaterialTheme.colorScheme.eventContainer
                        val baseContentColor = MaterialTheme.colorScheme.onEventContainer
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = eventColor,
                                contentColor = baseContentColor
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 24.dp,
                                        top = 16.dp,
                                        bottom = 16.dp,
                                        end = 24.dp
                                    )
                            ) {
                                // Event Badge and Title
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(eventTitleRes),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = baseContentColor,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = baseContentColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.badge_event),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = baseContentColor,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Time and Duration Section
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(eventTimeRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = baseContentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DurationBadge(
                                        text = stringResource(eventDurationRes),
                                        baseColor = baseContentColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Description
                                Text(
                                    text = stringResource(eventDescriptionRes),
                                    color = baseContentColor.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Hosted by / Location Section
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val hostString = stringResource(
                                        R.string.label_hosted_by,
                                        stringResource(eventHostRes)
                                    )
                                    val locationString = stringResource(eventLocationRes)
                                    Text(
                                        text = "$hostString • $locationString",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = baseContentColor.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (showPlan) {
                        // Replicate PlanItem exactly from CalendarItem.kt (No title, starts with Time/Duration)
                        val isPlan1 = selectedDay == 10
                        val planColor = MaterialTheme.colorScheme.planContainer
                        val baseContentColor = MaterialTheme.colorScheme.onPlanContainer
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = planColor,
                                contentColor = baseContentColor
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 24.dp,
                                        top = 16.dp,
                                        bottom = 16.dp,
                                        end = 24.dp
                                    )
                            ) {
                                // Time and Duration Section
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text =
                                            if (isPlan1) stringResource(R.string.onboarding_mock_plan1_time)
                                            else stringResource(R.string.onboarding_mock_plan2_time),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = baseContentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DurationBadge(
                                        text =
                                            if (isPlan1) stringResource(R.string.onboarding_mock_plan1_duration)
                                            else stringResource(R.string.onboarding_mock_plan2_duration),
                                        baseColor = baseContentColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Note section
                                Text(
                                    text =
                                        if (isPlan1) stringResource(R.string.onboarding_mock_plan1_note)
                                        else stringResource(R.string.onboarding_mock_plan2_note),
                                    color = baseContentColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Friends visibility row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = baseContentColor.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_everyone),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = baseContentColor.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(start = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Symmetrical horizontal divider
            AppHorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            // Hints section at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: Hint 1 & Hint 2
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberBadge(
                            number = 1,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_calendar_date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberBadge(
                            number = 2,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_calendar_indicators),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                // Right Column: Hint 3
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        NumberBadge(
                            number = 3,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_text_hint_calendar_add),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
