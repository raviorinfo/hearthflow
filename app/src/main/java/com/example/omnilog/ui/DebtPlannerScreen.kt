package com.example.omnilog.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import com.example.omnilog.data.model.DebtEntry
import com.example.omnilog.data.model.InvestmentEntry
import com.example.omnilog.ui.theme.*
import com.example.omnilog.viewmodel.MainViewModel

@Composable
fun DebtPlannerScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    val investments by viewModel.investments.collectAsState()
    val isPremiumActive by viewModel.isPremiumActive.collectAsState()
    
    var selectedTab by remember { mutableStateOf("Liabilities") } // "Liabilities" or "Assets"
    
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtEntry?>(null) }
    
    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<InvestmentEntry?>(null) }
    
    var showPremiumDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // ─── Consolidated Net Wealth summary ───
            val totalLiabilities = remember(debts) { debts.sumOf { it.balance } }
            val totalAssets = remember(investments) { investments.sumOf { it.balance } }
            val netWealth = totalAssets - totalLiabilities
            val isDark = isSystemInDarkTheme()

            val netWorthGradient = when {
                netWealth > 0 -> Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF064E3B))) // Deep slate to deep forest green
                netWealth < 0 -> Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF451A03))) // Deep slate to deep amber/rust red
                else -> Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1E2D))) // Deep slate to grey
            }

            val netWorthBgModifier = if (isDark) {
                Modifier.background(netWorthGradient)
            } else {
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .then(netWorthBgModifier)
                    .border(
                        1.dp,
                        if (netWealth > 0) BrandEmerald.copy(alpha = 0.4f) else if (netWealth < 0) BrandRose.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Consolidated Net Wealth",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = formatCurrency(netWealth),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = if (netWealth > 0) BrandEmerald else if (netWealth < 0) BrandRose else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (netWealth > 0) BrandEmerald.copy(alpha = 0.15f) else if (netWealth < 0) BrandRose.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f))
                                .border(1.dp, if (netWealth > 0) BrandEmerald.copy(alpha = 0.4f) else if (netWealth < 0) BrandRose.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (netWealth > 0) "Wealth Surplus 📈" else if (netWealth < 0) "Net Liability 📉" else "Balanced ⚖️",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (netWealth > 0) BrandEmerald else if (netWealth < 0) BrandRose else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Assets / Liabilities breakdown summary row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandCyan))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Assets: ${formatCurrency(totalAssets)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandRose))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Liabilities: ${formatCurrency(totalLiabilities)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Asset-to-Liability dynamic progress bar slider
                    val totalSum = totalAssets + totalLiabilities
                    val assetRatio = if (totalSum > 0) (totalAssets / totalSum).toFloat() else 0.5f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(BrandRose.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(assetRatio)
                                .clip(CircleShape)
                                .background(BrandCyan)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(assetRatio * 100).toInt()}% Assets",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = BrandCyan
                        )
                        Text(
                            text = "${((1f - assetRatio) * 100).toInt()}% Liabilities",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = BrandRose
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(4.dp))

                    val totalMinPayments = remember(debts) { debts.sumOf { it.minPayment } }
                    val totalContributions = remember(investments) { investments.sumOf { it.monthlyContribution } }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Min Payments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(totalMinPayments),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandRose
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Monthly Contributions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(totalContributions),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandCyan
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sliding Tab Selector Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("Liabilities", "Assets")
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
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
                                .clickable { selectedTab = tab }
                                .pressScale()
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab == "Liabilities") "Liabilities (Loans)" else "Assets (Investments)",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == "Liabilities") {
                // Header Box
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
                        Column(modifier = Modifier.weight(1f)) {
                            GradientText(
                                text = "My Liabilities Ledger",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Manage your outstanding debts and interest APRs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Button(
                            onClick = {
                                if (!isPremiumActive && debts.size >= 1) {
                                    showPremiumDialog = true
                                } else {
                                    showAddDebtDialog = true
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.pressScale(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Debt", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (debts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.SentimentSatisfied,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = BrandGradientStart
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("No Outstanding Debts Logged!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Add a credit card, school loan, or car payment to begin plotting your payoff roadmap.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    DebtStrategyCard(debts)
                    Spacer(Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(debts) { debt ->
                            DebtItemCard(
                                debt = debt,
                                onEdit = { editingDebt = debt },
                                onDelete = { viewModel.deleteDebt(debt.id) },
                                onToggleEmiPaid = { isPaid -> viewModel.toggleEmiPaid(debt.id, isPaid) }
                            )
                        }
                    }
                }
            } else {
                // Assets / Investments Tab
                // Header Box
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
                        Column(modifier = Modifier.weight(1f)) {
                            GradientText(
                                text = "My Assets Ledger",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Track your active investments and expected growth rates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Button(
                            onClick = {
                                if (!isPremiumActive && investments.size >= 1) {
                                    showPremiumDialog = true
                                } else {
                                    showAddInvestmentDialog = true
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.pressScale(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Asset", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (investments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = BrandCyan
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("No Active Assets Logged!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Add your mutual funds, stock portfolios, fixed deposits, or gold to dynamically compile your net worth.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(investments) { investment ->
                            InvestmentItemCard(
                                investment = investment,
                                onEdit = { editingInvestment = investment },
                                onDelete = { viewModel.deleteInvestment(investment.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { n, b, r, m ->
                viewModel.addDebt(n, b, r, m)
                showAddDebtDialog = false
            }
        )
    }

    editingDebt?.let { debt ->
        EditDebtDialog(
            debt = debt,
            onDismiss = { editingDebt = null },
            onSave = { name, balance, rate, min ->
                viewModel.editDebt(debt.id, name, balance, rate, min)
                editingDebt = null
            }
        )
    }

    if (showAddInvestmentDialog) {
        AddInvestmentDialog(
            onDismiss = { showAddInvestmentDialog = false },
            onSave = { name, balance, contribution, expectedReturn ->
                viewModel.addInvestment(name, balance, contribution, expectedReturn)
                showAddInvestmentDialog = false
            }
        )
    }

    editingInvestment?.let { investment ->
        EditInvestmentDialog(
            investment = investment,
            onDismiss = { editingInvestment = null },
            onSave = { name, balance, contribution, expectedReturn ->
                viewModel.editInvestment(investment.id, name, balance, contribution, expectedReturn)
                editingInvestment = null
            }
        )
    }

    if (showPremiumDialog) {
        PremiumSubscriptionDialog(
            onDismiss = { showPremiumDialog = false },
            onPurchase = { plan, duration ->
                viewModel.purchasePremiumPlan(plan, duration)
                showPremiumDialog = false
            }
        )
    }
}

@Composable
fun DebtStrategyCard(debts: List<DebtEntry>) {
    if (debts.isEmpty() || debts.size == 1) return

    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0x15FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    val highestInterest = debts.maxByOrNull { it.interestRate }
    val lowestBalance = debts.minByOrNull { it.balance }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, BrandAmber.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Recommended Strategy",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))

            if (highestInterest != null && lowestBalance != null) {
                if (highestInterest.id == lowestBalance.id) {
                    Text(
                        text = "Focus all extra payments on '${highestInterest.name}'. It has both the highest interest rate and the lowest balance. This is an optimal target for fast payoff.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(BrandRose).align(Alignment.CenterVertically))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Avalanche Method (Save Money)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                Text("Focus extra payments on '${highestInterest.name}' (${highestInterest.interestRate}%) to minimize total interest paid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(BrandCyan).align(Alignment.CenterVertically))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Snowball Method (Psychological Win)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                Text("Pay off '${lowestBalance.name}' (${formatCurrency(lowestBalance.balance)}) first to clear an account quickly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- Financial Math Utilities ---

data class PayoffProjection(
    val months: Int,
    val totalInterest: Double,
    val totalCost: Double,
    val isNegativeAmortization: Boolean
)

fun calculatePayoff(balance: Double, apr: Double, minPayment: Double): PayoffProjection {
    if (balance <= 0) return PayoffProjection(0, 0.0, 0.0, false)
    if (apr <= 0) {
        val months = if (minPayment > 0) Math.ceil(balance / minPayment).toInt() else -1
        return PayoffProjection(months, 0.0, balance, months == -1)
    }
    
    val monthlyRate = apr / 100.0 / 12.0
    val interestThisMonth = balance * monthlyRate
    
    if (minPayment <= interestThisMonth) {
        return PayoffProjection(-1, 0.0, 0.0, true) // Negative amortization
    }
    
    val monthsDouble = -Math.log(1.0 - (monthlyRate * balance) / minPayment) / Math.log(1.0 + monthlyRate)
    val months = Math.ceil(monthsDouble).toInt()
    
    // In reality the final month is a partial payment, but multiplying gives a close estimate
    // A slightly more accurate cost is:
    var remainingBalance = balance
    var totalCost = 0.0
    var monthCount = 0
    while (remainingBalance > 0 && monthCount < 1200) { // cap at 100 years
        monthCount++
        val interest = remainingBalance * monthlyRate
        val payment = minOf(minPayment, remainingBalance + interest)
        totalCost += payment
        remainingBalance = remainingBalance + interest - payment
    }
    
    val totalInterest = totalCost - balance
    return PayoffProjection(monthCount, totalInterest, totalCost, false)
}

data class InvestmentProjection(
    val futureValue: Double,
    val totalContributions: Double,
    val totalGrowth: Double
)

fun calculate10YearInvestment(balance: Double, monthlyContribution: Double, annualReturnRate: Double): InvestmentProjection {
    val months = 120
    val monthlyRate = annualReturnRate / 100.0 / 12.0
    
    var futureValue = balance
    for (i in 1..months) {
        futureValue += monthlyContribution
        futureValue *= (1.0 + monthlyRate)
    }
    
    val totalContributions = balance + (monthlyContribution * months)
    val totalGrowth = futureValue - totalContributions
    
    return InvestmentProjection(futureValue, totalContributions, totalGrowth)
}

@Composable
fun DebtItemCard(
    debt: DebtEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEmiPaid: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val (aprBadgeColor, aprBadgeLabel, aprBadgeBg) = when {
        debt.interestRate >= 15.0 -> Triple(BrandRose, "CRITICAL APR", BrandRose.copy(alpha = 0.15f))
        debt.interestRate >= 8.0 -> Triple(BrandAmber, "MODERATE APR", BrandAmber.copy(alpha = 0.15f))
        else -> Triple(BrandEmerald, "MANAGEABLE APR", BrandEmerald.copy(alpha = 0.15f))
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            aprBadgeColor.copy(alpha = 0.5f),
            if (isDark) Color(0x10FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            BrandGradientEnd.copy(alpha = 0.3f)
        )
    )

    val bg = if (isDark) Color(0x15FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (debt.isEmiPaid) 0.65f else 1.0f)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
            .clickable { onEdit() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = debt.name, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(aprBadgeBg)
                            .border(1.dp, aprBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = aprBadgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = aprBadgeColor
                        )
                    }
                }
                
                Spacer(Modifier.height(10.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Balance", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                        Text(
                            text = formatCurrency(debt.balance),
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("Interest Rate", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                        Text(
                            text = "${debt.interestRate}% APR",
                            style = MaterialTheme.typography.titleMedium,
                            color = aprBadgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Required Min Pay: ${formatCurrency(debt.minPayment)}/mo",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (debt.isEmiPaid) BrandEmerald else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    val buttonBg = if (debt.isEmiPaid) BrandEmerald.copy(alpha = 0.2f) else (if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    val buttonBorder = if (debt.isEmiPaid) BrandEmerald.copy(alpha = 0.5f) else (if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    val buttonText = if (debt.isEmiPaid) "EMI Paid ✓" else "Mark EMI Paid"
                    val buttonTextColor = if (debt.isEmiPaid) BrandEmerald else (if (isDark) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(8.dp))
                            .clickable { onToggleEmiPaid(!debt.isEmiPaid) }
                            .pressScale()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = buttonTextColor
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(Modifier.height(12.dp))

                // Payoff Projection Section
                val projection = remember(debt) {
                    calculatePayoff(debt.balance, debt.interestRate, debt.minPayment)
                }

                if (projection.isNegativeAmortization) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = BrandRose, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Warning: Payment too low. Balance will grow forever.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandRose
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Est. Payoff Time", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                            val years = projection.months / 12
                            val months = projection.months % 12
                            val timeStr = if (years > 0) "${years}y ${months}m" else "${months}m"
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Interest", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                            Text(
                                text = formatCurrency(projection.totalInterest),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = aprBadgeColor
                            )
                        }
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.pressScale()) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit Debt",
                        tint = BrandCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.pressScale()) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete Debt",
                        tint = BrandRose.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InvestmentItemCard(
    investment: InvestmentEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val rateBadgeColor = when {
        investment.expectedReturnRate >= 15.0 -> BrandEmerald
        investment.expectedReturnRate >= 8.0 -> BrandCyan
        else -> Color(0xFF818CF8) // Brighter Indigo (Indigo 400)
    }
    
    val rateBadgeBg = rateBadgeColor.copy(alpha = 0.15f)

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            rateBadgeColor.copy(alpha = 0.5f),
            if (isDark) Color(0x10FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            BrandGradientEnd.copy(alpha = 0.3f)
        )
    )

    val bg = if (isDark) Color(0x15FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
            .clickable { onEdit() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = investment.name, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(rateBadgeBg)
                            .border(1.dp, rateBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${investment.expectedReturnRate}% Expected",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = rateBadgeColor
                        )
                    }
                }
                
                Spacer(Modifier.height(10.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Balance", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                        Text(
                            text = formatCurrency(investment.balance),
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("Monthly Contribution", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                        Text(
                            text = formatCurrency(investment.monthlyContribution),
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(Modifier.height(12.dp))

                // 10-Year Projection Section
                val projection = remember(investment) {
                    calculate10YearInvestment(investment.balance, investment.monthlyContribution, investment.expectedReturnRate)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("10-Year Projection", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                        Text(
                            text = formatCurrency(projection.futureValue),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = rateBadgeColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Compound Growth", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                        Text(
                            text = "+${formatCurrency(projection.totalGrowth)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandEmerald
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.pressScale()) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit Asset",
                        tint = BrandCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.pressScale()) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete Asset",
                        tint = BrandRose.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(onDismiss: () -> Unit, onSave: (String, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            GradientText(
                text = "Add New Liability",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Debt Account Name (e.g. Mastercard)"
                )
                DialogTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = "Current Outstanding Balance",
                    prefix = getCurrencySymbol() + " "
                )
                DialogTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = "Annual Percentage Rate (APR %)"
                )
                DialogTextField(
                    value = min,
                    onValueChange = { min = it },
                    label = "Minimum Monthly Payment",
                    prefix = getCurrencySymbol() + " "
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(
                        name,
                        balance.toDoubleOrNull() ?: 0.0,
                        rate.toDoubleOrNull() ?: 0.0,
                        min.toDoubleOrNull() ?: 0.0
                    ) 
                },
                enabled = name.isNotBlank() && balance.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.pressScale()
            ) {
                Text("Add Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
    )
}

@Composable
fun EditDebtDialog(
    debt: DebtEntry,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(debt.name) }
    var balance by remember { mutableStateOf(debt.balance.toString()) }
    var rate by remember { mutableStateOf(debt.interestRate.toString()) }
    var min by remember { mutableStateOf(debt.minPayment.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            GradientText(
                text = "Modify Liability Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Debt Account Name"
                )
                DialogTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = "Outstanding Balance",
                    prefix = getCurrencySymbol() + " "
                )
                DialogTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = "Annual Percentage Rate (APR %)"
                )
                DialogTextField(
                    value = min,
                    onValueChange = { min = it },
                    label = "Minimum Monthly Payment",
                    prefix = getCurrencySymbol() + " "
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(
                        name,
                        balance.toDoubleOrNull() ?: 0.0,
                        rate.toDoubleOrNull() ?: 0.0,
                        min.toDoubleOrNull() ?: 0.0
                    ) 
                },
                enabled = name.isNotBlank() && balance.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.pressScale()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
    )
}

@Composable
fun AddInvestmentDialog(onDismiss: () -> Unit, onSave: (String, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    var expectedReturn by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            GradientText(
                text = "Add New Investment Asset",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Asset Account Name (e.g. Nifty Index Fund)"
                )
                DialogTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = "Current Capital Balance",
                    prefix = getCurrencySymbol() + " "
                )
                DialogTextField(
                    value = contribution,
                    onValueChange = { contribution = it },
                    label = "Monthly SIP Contribution Amount",
                    prefix = getCurrencySymbol() + " "
                )
                DialogTextField(
                    value = expectedReturn,
                    onValueChange = { expectedReturn = it },
                    label = "Expected Annual Return Rate (CAGR %)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(
                        name,
                        balance.toDoubleOrNull() ?: 0.0,
                        contribution.toDoubleOrNull() ?: 0.0,
                        expectedReturn.toDoubleOrNull() ?: 0.0
                    ) 
                },
                enabled = name.isNotBlank() && balance.toDoubleOrNull() != null && contribution.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.pressScale()
            ) {
                Text("Add Asset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
    )
}

@Composable
fun EditInvestmentDialog(
    investment: InvestmentEntry,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(investment.name) }
    var balance by remember { mutableStateOf(investment.balance.toString()) }
    var contribution by remember { mutableStateOf(investment.monthlyContribution.toString()) }
    var expectedReturn by remember { mutableStateOf(investment.expectedReturnRate.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            GradientText(
                text = "Modify Investment Asset Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Asset Account Name"
                )
                DialogTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = "Capital Balance",
                    prefix = getCurrencySymbol() + " "
                )
                DialogTextField(
                    value = contribution,
                    onValueChange = { contribution = it },
                    label = "Monthly SIP Contribution Amount",
                    prefix = getCurrencySymbol() + " "
                )
                DialogTextField(
                    value = expectedReturn,
                    onValueChange = { expectedReturn = it },
                    label = "Expected Annual Return Rate (CAGR %)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(
                        name,
                        balance.toDoubleOrNull() ?: 0.0,
                        contribution.toDoubleOrNull() ?: 0.0,
                        expectedReturn.toDoubleOrNull() ?: 0.0
                    ) 
                },
                enabled = name.isNotBlank() && balance.toDoubleOrNull() != null && contribution.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.pressScale()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
    )
}

@Composable
fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusGlow by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0.15f,
        animationSpec = tween(300),
        label = "dialogFieldGlow"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            BrandGradientStart.copy(alpha = focusGlow),
            BrandGradientMid.copy(alpha = focusGlow * 0.5f),
            BrandGradientEnd.copy(alpha = focusGlow)
        )
    )

    val isDark = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x0AFFFFFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)) },
            prefix = if (prefix.isNotEmpty()) { { Text(prefix, color = BrandCyan) } } else null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = BrandCyan,
                focusedLabelColor = BrandCyan,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}
