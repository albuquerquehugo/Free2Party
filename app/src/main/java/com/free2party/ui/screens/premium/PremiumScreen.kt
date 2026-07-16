package com.free2party.ui.screens.premium

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.free2party.data.billing.DurationType
import com.free2party.data.billing.PremiumPackage
import com.free2party.R
import com.free2party.ui.components.TopBar
import com.free2party.ui.theme.Gold
import com.free2party.ui.theme.PremiumBannerColor1
import com.free2party.ui.theme.PremiumBannerColor2
import com.free2party.util.findActivity
import com.free2party.util.parsePrice
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PremiumRoute(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val premiumWelcomeMessage = stringResource(R.string.toast_premium_welcome)
    val activityNotFoundMessage = stringResource(R.string.toast_activity_not_found)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is PremiumUiEvent.PurchaseSuccess -> {
                    Toast.makeText(
                        context,
                        premiumWelcomeMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    onBack()
                }

                is PremiumUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    PremiumScreen(
        uiState = uiState,
        gradientBackground = viewModel.gradientBackground,
        onBack = onBack,
        onPackageSelected = { pkg ->
            val activity = context.findActivity()
            if (activity != null) {
                viewModel.purchase(activity, pkg)
            } else {
                Toast.makeText(
                    context,
                    activityNotFoundMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onRestoreClick = {
            viewModel.restore()
        }
    )
}

@Composable
fun PremiumScreen(
    uiState: PremiumUiState,
    gradientBackground: Boolean,
    onBack: () -> Unit,
    onPackageSelected: (PremiumPackage) -> Unit,
    onRestoreClick: () -> Unit
) {
    var selectedPackage by remember { mutableStateOf<PremiumPackage?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                color = MaterialTheme.colorScheme.onSurface,
                showBackButton = true,
                onBack = onBack
            )
        },
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when (uiState) {
                is PremiumUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is PremiumUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text(stringResource(R.string.label_go_back))
                        }
                    }
                }

                is PremiumUiState.Success -> {
                    // Automatically select default package (Annual is standard default recommendation)
                    if (selectedPackage == null && uiState.packages.isNotEmpty()) {
                        selectedPackage = uiState.packages.find {
                            it.durationType == DurationType.ANNUALLY
                        } ?: uiState.packages.firstOrNull()
                    }

                    val monthlyPackage = remember(uiState.packages) {
                        uiState.packages.find { it.durationType == DurationType.MONTHLY }
                    }
                    val annualPackage = remember(uiState.packages) {
                        uiState.packages.find { it.durationType == DurationType.ANNUALLY }
                    }
                    val savingsPercentage = remember(uiState.packages) {
                        if (monthlyPackage != null && annualPackage != null) {
                            val monthlyVal = parsePrice(monthlyPackage.price)
                            val annualVal = parsePrice(annualPackage.price)
                            if (monthlyVal != null && annualVal != null && monthlyVal > 0.0) {
                                val totalMonthlyCost = monthlyVal * 12
                                val savings = totalMonthlyCost - annualVal
                                val percent = (savings / totalMonthlyCost) * 100
                                percent.coerceIn(0.0, 100.0).roundToInt()
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Premium Header Section with custom gradient background card
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, shape = RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PremiumBannerColor1,
                                                PremiumBannerColor2
                                            )
                                        )
                                    )
                                    .padding(24.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_crown),
                                            contentDescription = null,
                                            tint = Gold,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.app_name) + " " +
                                                    stringResource(R.string.label_premium),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = Color.White
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_crown),
                                            contentDescription = null,
                                            tint = Gold,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.text_premium_header),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Premium Features Checklist
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.label_features_included),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FeatureItem(
                                title = stringResource(R.string.label_feature_no_ads),
                                description = stringResource(R.string.text_feature_no_ads)
                            )
                            FeatureItem(
                                title = stringResource(R.string.label_feature_customization),
                                description = stringResource(R.string.text_feature_customization)
                            )
                            FeatureItem(
                                title = stringResource(R.string.label_feature_circles),
                                description = stringResource(R.string.text_feature_circles)
                            )
                            //TODO: Add calendar sync to premium version
//                            FeatureItem(
//                                title = stringResource(R.string.feature_sync_title),
//                                description = stringResource(R.string.feature_sync_desc)
//                            )
                            //TODO: Add passive analytics trends to premium version
//                            FeatureItem(
//                                title = stringResource(R.string.feature_trends_title),
//                                description = stringResource(R.string.feature_trends_desc)
//                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Simulated mode info badge
                        // Checked statically from the API key state
                        if (uiState.isSandboxMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.text_premium_sandbox_warning),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Pricing Tier Selection list
                        Text(
                            text = stringResource(R.string.label_choose_plan),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        uiState.packages.forEach { pkg ->
                            val isSelected = selectedPackage?.id == pkg.id
                            PackageCard(
                                pkg = pkg,
                                isSelected = isSelected,
                                savingsPercentage = savingsPercentage,
                                onClick = { selectedPackage = pkg }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Checkout Actions
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { selectedPackage?.let { onPackageSelected(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = selectedPackage != null && !uiState.isPurchasing
                        ) {
                            if (uiState.isPurchasing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (uiState.isPremium) {
                                        stringResource(R.string.label_renew_subscription)
                                    } else {
                                        stringResource(R.string.label_upgrade_now)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onRestoreClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !uiState.isPurchasing
                        ) {
                            Text(
                                text = stringResource(R.string.label_restore_purchases),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PackageCard(
    pkg: PremiumPackage,
    isSelected: Boolean,
    savingsPercentage: Int? = null,
    onClick: () -> Unit
) {
    val outlineColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(
            alpha = 0.5f
        )
    val cardBackground =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.3f
        )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = outlineColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pkg.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (pkg.durationType == DurationType.ANNUALLY && savingsPercentage != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.label_yearly_save,
                                    savingsPercentage
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pkg.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pkg.price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (pkg.durationType) {
                        DurationType.MONTHLY -> stringResource(R.string.label_billing_per_month)
                        DurationType.ANNUALLY -> stringResource(R.string.label_billing_per_year)
                        DurationType.LIFETIME -> stringResource(R.string.label_billing_one_time)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
