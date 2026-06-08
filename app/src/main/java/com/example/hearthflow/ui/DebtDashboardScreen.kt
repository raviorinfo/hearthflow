package com.example.hearthflow.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hearthflow.ui.theme.*
import com.example.hearthflow.viewmodel.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDashboardScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    val profile by viewModel.financialProfile.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()
    val isPremiumActive by viewModel.isPremiumActive.collectAsState()
    
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val timeline = viewModel.calculatePayoffTimeline("Snowball", includeHistory = true)
    val totalDebt = debts.sumOf { it.balance }
    val monthsToFree = timeline.lastOrNull()?.month?.coerceAtLeast(0) ?: 0

    var dropdownFocused by remember { mutableStateOf(false) }
    var amountFocused by remember { mutableStateOf(false) }

    val amountGlow by animateFloatAsState(
        targetValue = if (amountFocused) 0.6f else 0.15f,
        animationSpec = tween(300),
        label = "amountGlow"
    )

    val amountBorderBrush = Brush.linearGradient(
        colors = listOf(
            BrandGradientStart.copy(alpha = amountGlow),
            BrandGradientMid.copy(alpha = amountGlow * 0.5f),
            BrandGradientEnd.copy(alpha = amountGlow)
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header GlassCard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x15FFFFFF))
                        .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val userName = userAccount?.name ?: "Guest"
                            GradientText(
                                text = "Welcome, $userName",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Debt Workspace Command Center", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { showProfileDialog = true }, modifier = Modifier.pressScale()) {
                            Icon(Icons.Default.Settings, "Financial Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            val currentProfile = profile
            if (currentProfile == null || currentProfile.monthlySalary == 0.0) {
                item {
                    OnboardingFinancialCard(onSetup = { showProfileDialog = true })
                }
            } else {
                item {
                    var daysLeft = 0L
                    if (isPremiumActive) {
                        val expiry = userAccount?.proExpiryTimestamp ?: 0L
                        daysLeft = ((expiry - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)
                    }

                    if (isPremiumActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1E1E28), Color(0xFF2C2516))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFFFD700), Color(0x33FFFFFF), Color(0xFFFFD700))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFD700)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("👑", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = userAccount?.subscriptionPlan ?: "Premium Pro",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD700)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Your subscription is active and gives you full access to unlimited debt & investment sheets.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Renew subscription at any time to extend your premium access.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x1AFFFFD7))
                                            .border(2.dp, Color(0xFFFFD700), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$daysLeft",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = "days left",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFFD700),
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Renew",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        ),
                                        modifier = Modifier
                                            .clickable { showSubscriptionDialog = true }
                                            .background(Color(0x33FFD700), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF13131A), Color(0xFF1C1E3A))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0x33FFFFFF), BrandGradientStart, Color(0x33FFFFFF))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚡", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        GradientText(
                                            text = "Upgrade to Premium Plan",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Unlock unlimited tracking for debts & investments starting at just ₹25.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { showSubscriptionDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandGradientStart,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("Get Pro", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // Summary Cards Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(modifier = Modifier.weight(1.4f)) {
                            Text("Total Outstanding Liabilities", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(6.dp))
                            GradientText(
                                text = formatCurrency(totalDebt),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text("Debt-Free Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(6.dp))
                            if (debts.isEmpty()) {
                                Text("Debt-Free!", style = MaterialTheme.typography.titleMedium, color = BrandGradientEnd, fontWeight = FontWeight.Bold)
                            } else {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.MONTH, maxOf(0, monthsToFree - 1))
                                Text("${cal.get(Calendar.YEAR)}", style = MaterialTheme.typography.headlineMedium, color = BrandGradientEnd, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text("Discretionary Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(4.dp))
                            val disc = (profile?.monthlySalary ?: 0.0) - (profile?.fixedExpenses ?: 0.0) - (profile?.monthlyInvestments ?: 0.0)
                            Text(formatCurrency(disc), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            Text("potential monthly extra", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text("Active Debts Count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(4.dp))
                            Text("${debts.size} debts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("under management", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // Payoff Progress Card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Journey Status Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                if (debts.isEmpty()) {
                                    Text("Outstanding! You are fully debt free. Keep building your financial future!", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    val totalMinPayment = debts.sumOf { it.minPayment }
                                    Text("To remain on track, a total of ${formatCurrency(totalMinPayment)} in Monthly EMI payments must be made this month.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                // Premium visual glow indicator
                                CircularProgressIndicator(
                                    progress = if (debts.isEmpty()) 1f else 0.15f,
                                    color = BrandGradientStart,
                                    trackColor = Color(0x1AFFFFFF),
                                    strokeWidth = 8.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    if (debts.isEmpty()) "100%" else "Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGradientEnd
                                )
                            }
                        }
                    }
                }

                // Strategic Wealth Advisor Card
                item {
                    val currentProfile = profile
                    if (currentProfile != null) {
                        val totalDebt = debts.sumOf { it.balance }
                        val totalMinPayment = debts.sumOf { it.minPayment }
                        val weightedApr = if (totalDebt > 0) debts.sumOf { it.balance * it.interestRate } / totalDebt else 0.0
                        val dti = if (currentProfile.monthlySalary > 0) ((currentProfile.fixedExpenses + totalMinPayment) / currentProfile.monthlySalary * 100) else 0.0
                        
                        val (dtiLabel, dtiColor) = when {
                            dti <= 36.0 -> "Low Risk" to BrandEmerald
                            dti <= 50.0 -> "Moderate Risk" to BrandAmber
                            else -> "High Risk" to BrandRose
                        }

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color(0x187C3AED)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, null, tint = BrandCyan, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Strategic Wealth Advisor",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(dtiColor.copy(alpha = 0.15f))
                                            .border(1.dp, dtiColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = dtiLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = dtiColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x15FFFFFF))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("DTI Ratio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${String.format("%.1f", dti)}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dti > 45) BrandRose else Color.White
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text("Weighted Interest Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${String.format("%.2f", weightedApr)}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (weightedApr > 8) BrandAmber else BrandCyan
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text("Monthly Invest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = formatCurrency(currentProfile.monthlyInvestments),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGradientEnd
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x0AFFFFFF))
                                        .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "Offline Wealth Suggestion:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BrandCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        val adviceText = when {
                                            totalDebt == 0.0 -> {
                                                "Congratulations! You are completely debt-free. Keep channelizing your discretionary cash flow of ${formatCurrency(currentProfile.monthlySalary - currentProfile.fixedExpenses)} into compound growth assets like equities, mutual funds, or tax-advantaged instruments to supercharge your wealth building."
                                            }
                                            weightedApr > 8.0 -> {
                                                "Your average interest rate (${String.format("%.1f", weightedApr)}%) is higher than typical long-term stock market returns (~8%). It is mathematically superior to **temporarily redirect your monthly investments (${formatCurrency(currentProfile.monthlyInvestments)})** into your extra debt payment plan. This generates a **guaranteed, risk-free, tax-free return equal to your interest rate (${String.format("%.1f", weightedApr)}%)**."
                                            }
                                            weightedApr >= 4.0 -> {
                                                "Your weighted average interest rate of ${String.format("%.1f", weightedApr)}% lies in the transition zone. Consider a **Hybrid Balanced Strategy**: maintain your current monthly investments of ${formatCurrency(currentProfile.monthlyInvestments)} for wealth accumulation and route all of your remaining discretionary cash flow of ${formatCurrency(currentProfile.monthlySalary - currentProfile.fixedExpenses - currentProfile.monthlyInvestments - totalMinPayment)} strictly towards debt payoff."
                                            }
                                            else -> {
                                                "Excellent! Your average interest rate (${String.format("%.1f", weightedApr)}%) is exceptionally low. Since long-term index market returns (~8%) historically outperform this rate, maintaining your current monthly investments of ${formatCurrency(currentProfile.monthlyInvestments)} is mathematically optimal. Keep paying Monthly EMI payments on low-interest debt while wealth compiles."
                                            }
                                        }

                                        Text(
                                            text = adviceText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                            lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Pay Logger
                if (debts.isNotEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Quick Log Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Instantly log a manual payment towards one of your debts.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                
                                Spacer(Modifier.height(4.dp))

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    val dropdownBorderGlow by animateFloatAsState(
                                        targetValue = if (dropdownFocused) 0.6f else 0.15f,
                                        animationSpec = tween(300),
                                        label = "dropdownGlow"
                                    )
                                    val dropdownBorderBrush = Brush.linearGradient(
                                        colors = listOf(
                                            BrandGradientStart.copy(alpha = dropdownBorderGlow),
                                            BrandGradientMid.copy(alpha = dropdownBorderGlow * 0.5f),
                                            BrandGradientEnd.copy(alpha = dropdownBorderGlow)
                                        )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x0CFFFFFF))
                                            .border(1.5.dp, dropdownBorderBrush, RoundedCornerShape(14.dp))
                                            .clickable { 
                                                expandedDropdown = true 
                                                dropdownFocused = true
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedDebtForPayment.isEmpty()) "Select Target Debt" else selectedDebtForPayment,
                                                color = if (selectedDebtForPayment.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color.White
                                            )
                                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = expandedDropdown,
                                        onDismissRequest = { 
                                            expandedDropdown = false 
                                            dropdownFocused = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .background(Color(0xFF131929))
                                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                    ) {
                                        debts.forEach { debt ->
                                            DropdownMenuItem(
                                                text = { Text("${debt.name} (${formatCurrency(debt.balance)})", color = Color.White) },
                                                onClick = {
                                                    selectedDebtForPayment = debt.name
                                                    expandedDropdown = false
                                                    dropdownFocused = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x0CFFFFFF))
                                        .border(1.5.dp, amountBorderBrush, RoundedCornerShape(14.dp))
                                ) {
                                    TextField(
                                        value = paymentAmount,
                                        onValueChange = { paymentAmount = it },
                                        label = { Text("Payment Amount", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                                        prefix = { Text(getCurrencySymbol() + " ", color = BrandVioletText) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { amountFocused = it.isFocused },
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        singleLine = true
                                    )
                                }

                                Button(
                                    onClick = {
                                        val amt = paymentAmount.toDoubleOrNull()
                                        if (selectedDebtForPayment.isNotEmpty() && amt != null && amt > 0) {
                                            viewModel.logDebtPayment(selectedDebtForPayment, amt)
                                            paymentAmount = ""
                                            selectedDebtForPayment = ""
                                        }
                                    },
                                    enabled = selectedDebtForPayment.isNotEmpty() && paymentAmount.toDoubleOrNull() != null,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Log Secure Payment", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Strategic Payoff Insights
                item {
                    val highestDebt = debts.maxByOrNull { it.interestRate }
                    val snowballTarget = debts.minByOrNull { it.balance }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, null, tint = BrandGradientEnd, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Strategic Payoff Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGradientEnd)
                            }

                            if (debts.isEmpty()) {
                                Text("No active liabilities found. Keep tracking your cash flow and build up your emergency fund buffer!", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text(
                                    text = "Based on your active liabilities, your dashboard has calculated the top payoff paths:\n\n" +
                                            "• **Avalanche Priority**: Focus all discretionary potential on **${highestDebt?.name}** (${highestDebt?.interestRate}% interest rate) to save the absolute maximum in interest fees over time.\n\n" +
                                            "• **Snowball Priority**: Focus all discretionary potential on **${snowballTarget?.name}** (${formatCurrency(snowballTarget?.balance ?: 0.0)} balance) to secure an immediate psychological victory.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog
    if (showProfileDialog) {
        ProfileSetupDialog(
            initialSalary = profile?.monthlySalary ?: 0.0,
            initialExpenses = profile?.fixedExpenses ?: 0.0,
            initialInvestments = profile?.monthlyInvestments ?: 0.0,
            onDismiss = { showProfileDialog = false },
            onSave = { s, e, i -> 
                viewModel.updateFinancialProfile(s, e, i)
                showProfileDialog = false
            }
        )
    }

    if (showSubscriptionDialog) {
        PremiumSubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onPurchase = { plan, duration ->
                viewModel.purchasePremiumPlan(plan, duration)
                showSubscriptionDialog = false
            }
        )
    }
}
