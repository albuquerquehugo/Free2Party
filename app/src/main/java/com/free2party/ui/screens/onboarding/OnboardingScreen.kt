package com.free2party.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.free2party.R
import com.free2party.ui.components.basic.AppTextButton
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    OnboardingScreen(
        onComplete = {
            viewModel.completeOnboarding()
            onOnboardingComplete()
        }
    )
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 7 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = 7,
                onNext = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onSkip = onComplete,
                onComplete = onComplete
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            OnboardingContent(
                pageIndex = page,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun OnboardingContent(
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Page Info
        val titleRes = when (pageIndex) {
            0 -> R.string.onboarding_title_welcome
            1 -> R.string.onboarding_title_availability
            2 -> R.string.onboarding_title_friends_circles
            3 -> R.string.onboarding_title_events_overview
            4 -> R.string.onboarding_title_event_details_rsvp
            5 -> R.string.onboarding_title_calendar
            else -> R.string.onboarding_title_ready
        }

        val descriptionRes = when (pageIndex) {
            0 -> R.string.onboarding_text_welcome
            1 -> R.string.onboarding_text_availability
            2 -> R.string.onboarding_text_friends_circles
            3 -> R.string.onboarding_text_events_overview
            4 -> R.string.onboarding_text_event_details_rsvp
            5 -> R.string.onboarding_text_calendar
            else -> R.string.onboarding_text_ready
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Live interactive preview in the center
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (pageIndex) {
                0 -> WelcomeIllustration()
                1 -> LiveAvailabilityTogglePreview()
                2 -> LiveFriendsCirclesPreview()
                3 -> LiveEventsOverviewPreview()
                4 -> LiveEventDetailsRsvpPreview()
                5 -> LiveCalendarPreview()
                else -> ReadyToPartyPreview()
            }
        }
    }
}

@Composable
fun OnboardingBottomBar(
    currentPage: Int,
    pageCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Left button: Skip
        if (currentPage < pageCount - 1) {
            AppTextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = stringResource(R.string.label_skip),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Center: Indicator Dots
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val active = index == currentPage
                val width by animateDpAsState(
                    targetValue = if (active) 24.dp else 8.dp,
                    label = "IndicatorWidth"
                )
                val color by animateColorAsState(
                    targetValue =
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    label = "IndicatorColor"
                )
                Box(
                    modifier = Modifier
                        .size(height = 8.dp, width = width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Right button: Next or Get Started
        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (currentPage < pageCount - 1) {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_next),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_get_started),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}


