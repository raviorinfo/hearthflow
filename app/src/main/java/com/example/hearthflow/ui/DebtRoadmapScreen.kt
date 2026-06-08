package com.example.hearthflow.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hearthflow.ui.theme.*
import com.example.hearthflow.viewmodel.MainViewModel
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtRoadmapScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    val profile by viewModel.financialProfile.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()
    val isPro by viewModel.isPremiumActive.collectAsState()

    var selectedStrategy by remember { mutableStateOf("Snowball") }
    var processingUpgrade by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val timeline = viewModel.calculatePayoffTimeline(selectedStrategy, includeHistory = true)
    val filteredTimeline = timeline

    if (processingUpgrade) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { 
                GradientText(
                    text = "Unlocking Premium Plan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(color = BrandViolet)
                    Spacer(Modifier.height(16.dp))
                    Text("Securing access to advanced debt strategies...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    GradientText(
                        text = "Payoff Roadmap",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Step-by-step liquidation timeline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            val currentProfile = profile
            if (currentProfile == null || currentProfile.monthlySalary == 0.0) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PulseRing(color = BrandViolet, size = 80.dp)
                                Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(36.dp), tint = BrandViolet)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Setup Financial Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Please configure your monthly salary and fixed expenses on the Workspace Home screen first to calculate your discretionary payoff potential.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (debts.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PulseRing(color = BrandIndigo, size = 80.dp)
                                Icon(Icons.Default.CardMembership, null, modifier = Modifier.size(36.dp), tint = BrandIndigo)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("No Active Liabilities Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Add debts in the 'My Debts' workspace page to view your structured strategy map here.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Strategy Toggle Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Strategy:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        
                        val strategyOptions = listOf("Snowball", "Avalanche")
                        strategyOptions.forEach { opt ->
                            val isSelected = selectedStrategy == opt
                            val gradientSelected = Brush.horizontalGradient(
                                colors = listOf(BrandGradientStart, BrandGradientEnd)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(if (isSelected) Modifier.background(gradientSelected) else Modifier)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedStrategy = opt }
                                    .pressScale()
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable Timeline
                val displayLimit = filteredTimeline.size
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(filteredTimeline) { index, snapshot ->
                        TimelineItem(
                            index = index,
                            snapshot = snapshot,
                            isLast = index == displayLimit - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    index: Int,
    snapshot: MainViewModel.PayoffSnapshot,
    isLast: Boolean
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, snapshot.month)
    }
    val monthName = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(cal.time)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Connector Dot and Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp).padding(top = 10.dp)
        ) {
            val dotColor = if (snapshot.totalBalance == 0.0) {
                BrandEmerald
            } else if (snapshot.month < 0) {
                BrandEmerald
            } else {
                BrandViolet
            }
            
            Box(contentAlignment = Alignment.Center) {
                if (snapshot.totalBalance == 0.0) {
                    PulseRing(color = BrandEmerald, size = 32.dp)
                } else if (snapshot.month < 0) {
                    PulseRing(color = BrandEmerald, size = 26.dp)
                } else {
                    PulseRing(color = BrandViolet, size = 26.dp)
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                )
            }
            
            if (!isLast) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    dotColor,
                                    dotColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Content Card
        GlassCard(
            modifier = Modifier.fillMaxWidth().weight(1f),
            containerColor = if (snapshot.totalBalance == 0.0) BrandEmerald.copy(alpha = 0.12f) else null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (snapshot.totalBalance == 0.0) BrandEmerald else MaterialTheme.colorScheme.onSurface
                    )
                    if (snapshot.totalBalance == 0.0) {
                        NeonBadge(
                            text = "DEBT-FREE!",
                            color = BrandEmerald
                        )
                    } else if (snapshot.remainingDebts.all { it.paidEmiCount == 0 }) {
                        NeonBadge(
                            text = "Starting Balance",
                            color = BrandCyan
                        )
                    } else if (snapshot.month <= 0) {
                        NeonBadge(
                            text = "Paid ✓",
                            color = BrandEmerald
                        )
                    } else {
                        NeonBadge(
                            text = "Month ${snapshot.month}",
                            color = BrandViolet
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Remaining Balance:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedCounter(
                        value = snapshot.totalBalance,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (snapshot.totalBalance == 0.0) BrandEmerald else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Interest Paid this Month:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedCounter(
                        value = snapshot.remainingDebts.sumOf { it.interestAccrued },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BrandRose.copy(alpha = 0.85f)
                    )
                }

                val debtsToShow = snapshot.remainingDebts.filter { it.balance > 0.0 || it.interestAccrued > 0.0 || it.openingBalance > 0.0 }
                if (debtsToShow.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "EMI Amortization Details:",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandCyan,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        debtsToShow.forEach { debtInfo ->
                            val deductionDate = calculateDeductionDate(debtInfo.emiDate, snapshot.month)
                            val emiTitle = if (debtInfo.balance == 0.0 && debtInfo.openingBalance > 0.0) {
                                "EMI #${debtInfo.paidEmiCount} (Paid Off!)"
                            } else {
                                "EMI #${debtInfo.paidEmiCount} (${debtInfo.pendingEmiCount} pending)"
                            }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x05FFFFFF), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(BrandIndigo)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${debtInfo.name} - $emiTitle",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = deductionDate,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BrandCyan
                                    )
                                }
                                
                                Spacer(Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Opening: ${formatCurrency(debtInfo.openingBalance)} | Interest: +${formatCurrency(debtInfo.interestAccrued)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (debtInfo.balance == 0.0 && debtInfo.openingBalance > 0.0) "Paid Off!" else "Closing: ${formatCurrency(debtInfo.balance)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (debtInfo.balance == 0.0 && debtInfo.openingBalance > 0.0) BrandEmerald else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun calculateDeductionDate(emiDateDay: Int, monthsOffset: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1) // Anchor to prevent month length rollover issues
    calendar.add(Calendar.MONTH, monthsOffset)
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, minOf(emiDateDay, maxDay))
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(calendar.time)
}



