package com.example.hearthflow.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hearthflow.data.model.DebtEntry
import com.example.hearthflow.data.model.InvestmentEntry
import com.example.hearthflow.ui.theme.*
import com.example.hearthflow.viewmodel.MainViewModel
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import java.util.Calendar

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

        val totalLiabilities = remember(debts) { debts.sumOf { it.balance } }
        val totalAssets = remember(investments) { investments.sumOf { it.balance } }
        val netWealth = totalAssets - totalLiabilities
        val isDark = isSystemInDarkTheme()
        val totalSum = totalAssets + totalLiabilities
        val assetRatio = if (totalSum > 0.0) {
            val ratio = (totalAssets / totalSum).toFloat()
            if (ratio.isNaN() || ratio.isInfinite()) 0.5f else ratio
        } else 0.5f

        val wealthColor = if (netWealth >= 0) BrandEmerald else BrandRose
        val netWorthGradient = when {
            netWealth > 0 -> Brush.horizontalGradient(listOf(Color(0xFF0F261B), Color(0xFF0F172A)))
            netWealth < 0 -> Brush.horizontalGradient(listOf(Color(0xFF2C1412), Color(0xFF0F172A)))
            else -> Brush.horizontalGradient(listOf(Color(0xFF1E1E2D), Color(0xFF0F172A)))
        }

        val cardBgModifier = if (isDark) {
            Modifier.background(netWorthGradient)
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Consolidated Net Wealth summary ───
            item {
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .blur(32.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(wealthColor.copy(alpha = 0.08f), Color.Transparent)
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .then(cardBgModifier)
                            .border(
                                1.dp,
                                wealthColor.copy(alpha = 0.45f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                // Left side: Radial gauge donut ring
                                val animatedRatio by animateFloatAsState(
                                    targetValue = assetRatio,
                                    animationSpec = tween(1200, easing = EaseOutCubic),
                                    label = "wealthRadialRatio"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Background track (Liabilities = Rose)
                                        drawArc(
                                            color = BrandRose.copy(alpha = 0.25f),
                                            startAngle = 135f,
                                            sweepAngle = 270f,
                                            useCenter = false,
                                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        // Foreground active track (Assets = Cyan/Teal)
                                        drawArc(
                                            color = BrandCyan,
                                            startAngle = 135f,
                                            sweepAngle = 270f * animatedRatio,
                                            useCenter = false,
                                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    // Center share percentage
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${(animatedRatio * 100).toInt()}%",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (netWealth >= 0) BrandEmerald else BrandRose
                                        )
                                        Text(
                                            text = "Assets",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                // Right side: Financial Metrics
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CONSOLIDATED NET WORTH",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = formatCurrency(netWealth),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (netWealth > 0) BrandEmerald else if (netWealth < 0) BrandRose else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    // Soft status badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(wealthColor.copy(alpha = 0.15f))
                                            .border(0.5.dp, wealthColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (netWealth > 0) "Wealth Surplus 📈" else if (netWealth < 0) "Net Liability 📉" else "Balanced ⚖️",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = if (netWealth > 0) BrandEmerald else if (netWealth < 0) BrandRose else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Assets vs Liabilities breakdown summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Assets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text(formatCurrency(totalAssets), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandCyan)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        .align(Alignment.CenterVertically)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Liabilities", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text(formatCurrency(totalLiabilities), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandRose)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            val totalMinPayments = remember(debts) { debts.sumOf { it.minPayment } }
                            val totalContributions = remember(investments) { investments.sumOf { it.monthlyContribution } }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Monthly EMI Payments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = formatCurrency(totalMinPayments),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = BrandRose
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Monthly Contributions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = formatCurrency(totalContributions),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = BrandCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sliding Tab Selector Card
            item {
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
            }

            if (selectedTab == "Liabilities") {
                // Header Box
                item {
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
                                    text = "Manage your outstanding debts and interest rates",
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
                }

                if (debts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
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
                    }
                } else {
                    item {
                        DebtStrategyCard(debts)
                    }

                    items(debts) { debt ->
                        DebtItemCard(
                            debt = debt,
                            onEdit = { editingDebt = debt },
                            onDelete = { viewModel.deleteDebt(debt.id) },
                            onToggleEmiPaid = { isPaid -> viewModel.toggleEmiPaid(debt.id, isPaid) }
                        )
                    }
                }
            } else {
                // Assets / Investments Tab
                // Header Box
                item {
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
                }

                if (investments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
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
                    }
                } else {
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

    // Dialogs
    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { n, b, r, m, e, p, pend, tla, ten ->
                viewModel.addDebt(n, b, r, m, e, p, pend, tla, ten)
                showAddDebtDialog = false
            }
        )
    }

    editingDebt?.let { debt ->
        EditDebtDialog(
            debt = debt,
            onDismiss = { editingDebt = null },
            onSave = { name, balance, rate, min, emiDateVal, p, pend, tla, ten ->
                viewModel.editDebt(debt.id, name, balance, rate, min, emiDateVal, p, pend, tla, ten)
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
    val cardBg = if (isDark) Color(0x18FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    val highestInterest = debts.maxByOrNull { it.interestRate }
    val lowestBalance = debts.minByOrNull { it.balance }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, BrandAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = BrandAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AI Strategy Optimizer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrandAmber.copy(alpha = 0.12f))
                        .border(0.5.dp, BrandAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ACTIVE INSIGHTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = BrandAmber
                    )
                }
            }

            if (highestInterest != null && lowestBalance != null) {
                if (highestInterest.id == lowestBalance.id) {
                    Text(
                        text = "Focus all extra payments on '${highestInterest.name}'. It has both the highest interest rate and the lowest balance. This is an optimal target for fast payoff.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Panel: Avalanche (Teal/Cyan themed)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x10FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .border(0.5.dp, BrandCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandCyan))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "AVALANCHE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                                        color = BrandCyan
                                    )
                                }
                                Text(
                                    text = "Focus extra payments on '${highestInterest.name}' (${highestInterest.interestRate}%) to minimize total interest paid.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Right Panel: Snowball (Rose themed)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x10FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .border(0.5.dp, BrandRose.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandRose))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "SNOWBALL",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                                        color = BrandRose
                                    )
                                }
                                Text(
                                    text = "Pay off '${lowestBalance.name}' (${formatCurrency(lowestBalance.balance)}) first to clear an account quickly.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

private fun roundBanking(value: Double): Double {
    return java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
}

fun calculateStandardEmi(balance: Double, annualRate: Double, pendingMonths: Int): Double {
    if (pendingMonths <= 0) return 0.0
    if (annualRate <= 0.0) {
        return roundBanking(balance / pendingMonths)
    }
    val r = (annualRate / 100.0) / 12.0
    val num = balance * r * Math.pow(1.0 + r, pendingMonths.toDouble())
    val den = Math.pow(1.0 + r, pendingMonths.toDouble()) - 1.0
    return if (den > 0.0) roundBanking(num / den) else roundBanking(balance / pendingMonths)
}

fun calculateOutstandingBalance(totalLoanAmount: Double, annualRate: Double, tenure: Int, paidEmiCount: Int): Double {
    if (paidEmiCount <= 0) return totalLoanAmount
    if (tenure <= 0) return 0.0
    val emi = calculateStandardEmi(totalLoanAmount, annualRate, tenure)
    var currentBal = totalLoanAmount
    val r = (annualRate / 100.0) / 12.0
    for (i in 1..paidEmiCount) {
        val interest = roundBanking(currentBal * r)
        val payment = roundBanking(minOf(emi, currentBal + interest))
        currentBal = roundBanking(currentBal + interest - payment)
    }
    return currentBal
}

fun calculatePayoff(balance: Double, apr: Double, minPayment: Double): PayoffProjection {
    val roundedBalance = roundBanking(balance)
    if (roundedBalance <= 0) return PayoffProjection(0, 0.0, 0.0, false)
    if (apr <= 0) {
        val roundedMin = roundBanking(minPayment)
        val months = if (roundedMin > 0) Math.ceil(roundedBalance / roundedMin).toInt() else -1
        return PayoffProjection(months, 0.0, roundedBalance, months == -1)
    }
    
    val monthlyRate = apr / 100.0 / 12.0
    val interestThisMonth = roundBanking(roundedBalance * monthlyRate)
    
    if (minPayment <= interestThisMonth) {
        return PayoffProjection(-1, 0.0, 0.0, true) // Negative amortization
    }
    
    var remainingBalance = roundedBalance
    var totalCost = 0.0
    var monthCount = 0
    while (remainingBalance > 0.005 && monthCount < 1200) { // cap at 100 years
        monthCount++
        val interest = roundBanking(remainingBalance * monthlyRate)
        val payment = roundBanking(minOf(minPayment, remainingBalance + interest))
        totalCost = roundBanking(totalCost + payment)
        remainingBalance = roundBanking(remainingBalance + interest - payment)
    }
    
    val totalInterest = roundBanking(totalCost - roundedBalance)
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
        debt.interestRate >= 15.0 -> Triple(BrandRose, "CRITICAL INTEREST", BrandRose.copy(alpha = 0.15f))
        debt.interestRate >= 8.0 -> Triple(BrandAmber, "MODERATE INTEREST", BrandAmber.copy(alpha = 0.15f))
        else -> Triple(BrandEmerald, "MANAGEABLE INTEREST", BrandEmerald.copy(alpha = 0.15f))
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

    // Select suitable category avatar based on debt name keyword matching
    val nameLower = debt.name.lowercase()
    val (avatarIcon, avatarColor) = when {
        nameLower.contains("credit") || nameLower.contains("card") || nameLower.contains("hdfc") || nameLower.contains("sbi") || nameLower.contains("visa") || nameLower.contains("mastercard") -> Pair(Icons.Default.CreditCard, BrandRose)
        nameLower.contains("home") || nameLower.contains("house") || nameLower.contains("housing") || nameLower.contains("mortgage") -> Pair(Icons.Default.Home, BrandViolet)
        nameLower.contains("car") || nameLower.contains("auto") || nameLower.contains("vehicle") || nameLower.contains("bike") -> Pair(Icons.Default.DirectionsCar, BrandIndigo)
        nameLower.contains("education") || nameLower.contains("school") || nameLower.contains("college") || nameLower.contains("student") -> Pair(Icons.Default.School, BrandCyan)
        else -> Pair(Icons.Default.AccountBalance, Color(0xFF94A3B8))
    }

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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category-Specific glowing gradient avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(avatarColor, avatarColor.copy(alpha = 0.6f))))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(avatarIcon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = debt.name, 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = textColor
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(aprBadgeBg)
                                .border(0.5.dp, aprBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = aprBadgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = aprBadgeColor
                            )
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
                            text = "${debt.interestRate}%",
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
                        text = "Monthly EMI Payment: ${formatCurrency(debt.minPayment)}/mo (Due Day: ${debt.emiDate} · ${debt.paidEmiCount} paid, ${debt.pendingEmiCount} pending)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (debt.isEmiPaid) BrandEmerald else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
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

    // Select suitable category avatar based on investment name keyword matching
    val nameLower = investment.name.lowercase()
    val (avatarIcon, avatarColor) = when {
        nameLower.contains("stock") || nameLower.contains("equity") || nameLower.contains("share") -> Pair(Icons.Default.Assessment, BrandEmerald)
        nameLower.contains("mutual") || nameLower.contains("sip") || nameLower.contains("groww") -> Pair(Icons.Default.TrendingUp, BrandCyan)
        nameLower.contains("fd") || nameLower.contains("fixed") || nameLower.contains("saving") || nameLower.contains("cash") -> Pair(Icons.Default.Lock, Color(0xFF818CF8))
        nameLower.contains("gold") || nameLower.contains("sovereign") || nameLower.contains("bullion") -> Pair(Icons.Default.Stars, BrandAmber)
        else -> Pair(Icons.Default.MonetizationOn, Color(0xFF0D9488))
    }

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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category-Specific glowing gradient avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(avatarColor, avatarColor.copy(alpha = 0.6f))))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(avatarIcon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = investment.name, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = textColor
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(rateBadgeBg)
                            .border(0.5.dp, rateBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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
fun EmiDateSelector(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x0AFFFFFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentDay = if (selectedDay in 1..31) selectedDay else calendar.get(Calendar.DAY_OF_MONTH)
                
                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, _, _, dayOfMonth ->
                        onDaySelected(dayOfMonth)
                    },
                    currentYear,
                    currentMonth,
                    currentDay
                )
                datePickerDialog.show()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EMI Due Date (Day of Month)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Day $selectedDay",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Date",
                tint = BrandCyan
            )
        }
    }
}

@Composable
fun AddDebtDialog(onDismiss: () -> Unit, onSave: (String, Double, Double, Double, Int, Int, Int, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }
    var emiDate by remember { mutableStateOf(1) }
    var paidEmiCount by remember { mutableStateOf("0") }
    var pendingEmiCount by remember { mutableStateOf("12") }
    var totalLoanAmount by remember { mutableStateOf("") }
    var tenure by remember { mutableStateOf("12") }

    LaunchedEffect(totalLoanAmount, rate, tenure, paidEmiCount) {
        val loanAmtVal = totalLoanAmount.toDoubleOrNull() ?: 0.0
        val rateVal = rate.toDoubleOrNull() ?: 0.0
        val tenureVal = tenure.toIntOrNull() ?: 0
        val paidCountVal = paidEmiCount.toIntOrNull() ?: 0
        if (loanAmtVal > 0.0 && tenureVal > 0) {
            min = calculateStandardEmi(loanAmtVal, rateVal, tenureVal).toString()
            val outstanding = calculateOutstandingBalance(loanAmtVal, rateVal, tenureVal, paidCountVal)
            balance = outstanding.toString()
        }
    }

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
                    value = totalLoanAmount,
                    onValueChange = { totalLoanAmount = it },
                    label = "Total Loan Amount",
                    prefix = getCurrencySymbol() + " "
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
                    label = "Interest Rate (%)"
                )
                DialogTextField(
                    value = paidEmiCount,
                    onValueChange = { newValue ->
                        paidEmiCount = newValue
                        val paid = newValue.toIntOrNull() ?: 0
                        val pending = pendingEmiCount.toIntOrNull() ?: 0
                        tenure = (paid + pending).toString()
                    },
                    label = "Number of Paid EMIs"
                )
                DialogTextField(
                    value = pendingEmiCount,
                    onValueChange = { newValue ->
                        pendingEmiCount = newValue
                        val paid = paidEmiCount.toIntOrNull() ?: 0
                        val pending = newValue.toIntOrNull() ?: 0
                        tenure = (paid + pending).toString()
                    },
                    label = "Number of Pending EMIs"
                )
                DialogTextField(
                    value = tenure,
                    onValueChange = { newValue ->
                        tenure = newValue
                        val ten = newValue.toIntOrNull() ?: 0
                        val paid = paidEmiCount.toIntOrNull() ?: 0
                        pendingEmiCount = maxOf(0, ten - paid).toString()
                    },
                    label = "Total Tenure (Months)"
                )
                DialogTextField(
                    value = min,
                    onValueChange = { min = it },
                    label = "Monthly EMI Payment",
                    prefix = getCurrencySymbol() + " "
                )
                EmiDateSelector(
                    selectedDay = emiDate,
                    onDaySelected = { emiDate = it }
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
                        min.toDoubleOrNull() ?: 0.0,
                        emiDate,
                        paidEmiCount.toIntOrNull() ?: 0,
                        pendingEmiCount.toIntOrNull() ?: 12,
                        totalLoanAmount.toDoubleOrNull() ?: 0.0,
                        tenure.toIntOrNull() ?: 12
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
    onSave: (String, Double, Double, Double, Int, Int, Int, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf(debt.name) }
    var balance by remember { mutableStateOf(debt.balance.toString()) }
    var rate by remember { mutableStateOf(debt.interestRate.toString()) }
    var min by remember { mutableStateOf(debt.minPayment.toString()) }
    var emiDate by remember { mutableStateOf(debt.emiDate) }
    var paidEmiCount by remember { mutableStateOf(debt.paidEmiCount.toString()) }
    var pendingEmiCount by remember { mutableStateOf(debt.pendingEmiCount.toString()) }
    var totalLoanAmount by remember { mutableStateOf(debt.totalLoanAmount.toString()) }
    var tenure by remember { mutableStateOf(debt.tenure.toString()) }

    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(totalLoanAmount, rate, tenure, paidEmiCount) {
        if (!isInitialized) {
            isInitialized = true
            return@LaunchedEffect
        }
        val loanAmtVal = totalLoanAmount.toDoubleOrNull() ?: 0.0
        val rateVal = rate.toDoubleOrNull() ?: 0.0
        val tenureVal = tenure.toIntOrNull() ?: 0
        val paidCountVal = paidEmiCount.toIntOrNull() ?: 0
        if (loanAmtVal > 0.0 && tenureVal > 0) {
            min = calculateStandardEmi(loanAmtVal, rateVal, tenureVal).toString()
            val outstanding = calculateOutstandingBalance(loanAmtVal, rateVal, tenureVal, paidCountVal)
            balance = outstanding.toString()
        }
    }

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
                    value = totalLoanAmount,
                    onValueChange = { totalLoanAmount = it },
                    label = "Total Loan Amount",
                    prefix = getCurrencySymbol() + " "
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
                    label = "Interest Rate (%)"
                )
                DialogTextField(
                    value = paidEmiCount,
                    onValueChange = { newValue ->
                        paidEmiCount = newValue
                        val paid = newValue.toIntOrNull() ?: 0
                        val pending = pendingEmiCount.toIntOrNull() ?: 0
                        tenure = (paid + pending).toString()
                    },
                    label = "Number of Paid EMIs"
                )
                DialogTextField(
                    value = pendingEmiCount,
                    onValueChange = { newValue ->
                        pendingEmiCount = newValue
                        val paid = paidEmiCount.toIntOrNull() ?: 0
                        val pending = newValue.toIntOrNull() ?: 0
                        tenure = (paid + pending).toString()
                    },
                    label = "Number of Pending EMIs"
                )
                DialogTextField(
                    value = tenure,
                    onValueChange = { newValue ->
                        tenure = newValue
                        val ten = newValue.toIntOrNull() ?: 0
                        val paid = paidEmiCount.toIntOrNull() ?: 0
                        pendingEmiCount = maxOf(0, ten - paid).toString()
                    },
                    label = "Total Tenure (Months)"
                )
                DialogTextField(
                    value = min,
                    onValueChange = { min = it },
                    label = "Monthly EMI Payment",
                    prefix = getCurrencySymbol() + " "
                )
                EmiDateSelector(
                    selectedDay = emiDate,
                    onDaySelected = { emiDate = it }
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
                        min.toDoubleOrNull() ?: 0.0,
                        emiDate,
                        paidEmiCount.toIntOrNull() ?: 0,
                        pendingEmiCount.toIntOrNull() ?: 12,
                        totalLoanAmount.toDoubleOrNull() ?: 0.0,
                        tenure.toIntOrNull() ?: 12
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
