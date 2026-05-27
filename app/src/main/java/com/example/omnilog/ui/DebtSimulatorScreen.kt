package com.example.omnilog.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.omnilog.ui.theme.*
import com.example.omnilog.viewmodel.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtSimulatorScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    val profile by viewModel.financialProfile.collectAsState()

    var extraPaymentInput by remember { mutableStateOf("100") }
    var selectedStrategy by remember { mutableStateOf("Snowball") }

    val extraPayment = extraPaymentInput.toDoubleOrNull() ?: 0.0

    // Calculations
    val baselineTimeline = viewModel.calculatePayoffTimeline(selectedStrategy, 0.0)
    val simulatedTimeline = viewModel.calculatePayoffTimeline(selectedStrategy, extraPayment)

    val isBaselineInsufficient = baselineTimeline.any { it.month == -1 }
    val isSimulatedInsufficient = simulatedTimeline.any { it.month == -1 }

    val baselineMonths = baselineTimeline.size
    val simulatedMonths = simulatedTimeline.size

    val baselineInterest = if (baselineTimeline.isNotEmpty()) baselineTimeline.last().totalInterestPaid else 0.0
    val simulatedInterest = if (simulatedTimeline.isNotEmpty()) simulatedTimeline.last().totalInterestPaid else 0.0

    val monthsSaved = maxOf(0, baselineMonths - simulatedMonths)
    val interestSaved = maxOf(0.0, baselineInterest - simulatedInterest)

    // Side-by-side simultaneous calculations
    val snowballTimeline = viewModel.calculatePayoffTimeline("Snowball", extraPayment)
    val avalancheTimeline = viewModel.calculatePayoffTimeline("Avalanche", extraPayment)
    val snowballBaselineTimeline = viewModel.calculatePayoffTimeline("Snowball", 0.0)
    val avalancheBaselineTimeline = viewModel.calculatePayoffTimeline("Avalanche", 0.0)

    val sbMonths = snowballTimeline.size
    val avMonths = avalancheTimeline.size
    val sbInterest = if (snowballTimeline.isNotEmpty()) snowballTimeline.last().totalInterestPaid else 0.0
    val avInterest = if (avalancheTimeline.isNotEmpty()) avalancheTimeline.last().totalInterestPaid else 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                GradientText(
                    text = "Payoff Simulator",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "See how extra payments accelerate your freedom",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (profile == null) {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
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
                Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PulseRing(color = BrandIndigo, size = 80.dp)
                                Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(36.dp), tint = BrandIndigo)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("No Active Liabilities Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Add debts in the 'My Debts' workspace page to view dynamic simulated acceleration paths.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (isBaselineInsufficient || isSimulatedInsufficient) {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PulseRing(color = BrandRose, size = 80.dp)
                                Icon(Icons.Default.Warning, null, modifier = Modifier.size(36.dp), tint = BrandRose)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Insufficient Cash Flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandRose)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Your monthly salary cannot cover the required minimum payments. Please update your budget or salary settings on the Workspace Home dashboard.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Simulator Inputs
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Simulation Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // Strategy Selector for detail plotting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "View Strategy Detail:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
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
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Extra Payment Input
                        Text("Additional Monthly Contribution:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        
                        OutlinedTextField(
                            value = extraPaymentInput,
                            onValueChange = { extraPaymentInput = it },
                            label = { Text("Extra Monthly Amount", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            prefix = { Text(getCurrencySymbol(), color = BrandVioletText, fontWeight = FontWeight.Bold) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedLabelColor = BrandViolet,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = BrandViolet,
                                focusedContainerColor = Color(0x10FFFFFF),
                                unfocusedContainerColor = Color(0x05FFFFFF),
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        val discretionary = profile!!.monthlySalary - profile!!.fixedExpenses
                        val isOverBudget = extraPayment > discretionary
                        Text(
                            text = "Remaining discretionary potential: ${formatCurrency((discretionary - extraPayment).coerceAtLeast(0.0))} /mo",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverBudget) BrandRose else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = if (isOverBudget) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Strategy Comparison Matrix Card (Simultaneous payoff overview)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color(0x10FFFFFF)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CompareArrows, null, tint = BrandCyan, modifier = Modifier.size(22.dp))
                            Text(
                                "Strategy Comparison Matrix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            "Compare metrics side-by-side at ${formatCurrency(extraPayment)}/mo extra contribution.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Table header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0CFFFFFF), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text("Metric", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Snowball", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BrandVioletText, textAlign = TextAlign.End)
                            Text("Avalanche", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BrandCyan, textAlign = TextAlign.End)
                        }

                        // Row 1: Months to Free
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Months to Free", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("$sbMonths months", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                            Text("$avMonths months", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0BFFFFFF)))

                        // Row 2: Target Year
                        val sbCal = Calendar.getInstance().apply { add(Calendar.MONTH, sbMonths) }
                        val avCal = Calendar.getInstance().apply { add(Calendar.MONTH, avMonths) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Year", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("${sbCal.get(Calendar.YEAR)}", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandVioletText, textAlign = TextAlign.End)
                            Text("${avCal.get(Calendar.YEAR)}", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandCyan, textAlign = TextAlign.End)
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0BFFFFFF)))

                        // Row 3: Interest Paid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Interest Paid", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(formatCurrency(sbInterest), modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandRose, textAlign = TextAlign.End)
                            Text(formatCurrency(avInterest), modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandRose, textAlign = TextAlign.End)
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0BFFFFFF)))

                        // Row 4: Interest Saved
                        val sbBaseInterest = if (snowballBaselineTimeline.isNotEmpty()) snowballBaselineTimeline.last().totalInterestPaid else 0.0
                        val avBaseInterest = if (avalancheBaselineTimeline.isNotEmpty()) avalancheBaselineTimeline.last().totalInterestPaid else 0.0
                        val sbSaved = maxOf(0.0, sbBaseInterest - sbInterest)
                        val avSaved = maxOf(0.0, avBaseInterest - avInterest)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interest Saved", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(formatCurrency(sbSaved), modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandEmerald, textAlign = TextAlign.End)
                            Text(formatCurrency(avSaved), modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandEmerald, textAlign = TextAlign.End)
                        }
                    }
                }

                // Educational Strategy Alignment Banner (if results are identical)
                if (sbMonths == avMonths && Math.abs(sbInterest - avInterest) < 1.0) {
                    Spacer(Modifier.height(16.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0x186366F1)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                                Text(
                                    "Optimal Strategy Alignment",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandIndigoText
                                )
                            }
                            
                            Text(
                                text = "Both Snowball and Avalanche payoff timelines are completely identical. Mathematically, this optimal alignment occurs because your lowest-balance debt also happens to carry your highest interest rate (APR). Under these conditions, the psychological speed boost of Snowball and the ultimate cost efficiency of Avalanche suggest the exact same priority order—giving you the absolute best of both worlds!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Real-Time Acceleration Metrics Cards for selected detail strategy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Months Saved
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        containerColor = if (monthsSaved > 0) BrandViolet.copy(alpha = 0.12f) else null
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Months Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
                                if (monthsSaved > 0) {
                                    PulseRing(color = BrandViolet, size = 56.dp)
                                }
                                Text(
                                    text = if (monthsSaved > 0) "$monthsSaved mo" else "0 mo",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (monthsSaved > 0) BrandVioletText else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (monthsSaved > 0) {
                                Text("faster payoff!", style = MaterialTheme.typography.labelSmall, color = BrandVioletText, fontWeight = FontWeight.Bold)
                            } else {
                                Text("no acceleration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // Interest Saved
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        containerColor = if (interestSaved > 0.0) BrandCyan.copy(alpha = 0.12f) else null
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Interest Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
                                if (interestSaved > 0.0) {
                                    PulseRing(color = BrandCyan, size = 56.dp)
                                }
                                Text(
                                    text = formatCurrency(interestSaved),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (interestSaved > 0.0) BrandCyan else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (interestSaved > 0.0) {
                                Text("saved in fees!", style = MaterialTheme.typography.labelSmall, color = BrandCyan, fontWeight = FontWeight.Bold)
                            } else {
                                Text("no savings yet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Comparative Plan Summary Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Plan Comparison Detail (${selectedStrategy})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Baseline Plan
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x15FFFFFF))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Baseline (No Extra)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("Timeframe: $baselineMonths months", style = MaterialTheme.typography.bodySmall)
                                
                                val calBase = Calendar.getInstance()
                                calBase.add(Calendar.MONTH, baselineMonths)
                                val baseYear = calBase.get(Calendar.YEAR)
                                Text("Free Year: $baseYear", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                
                                Text("Interest: ${formatCurrency(baselineInterest)}", style = MaterialTheme.typography.bodySmall, color = BrandRose)
                            }

                            // Accelerated Plan
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd)))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Accelerated Path",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("Timeframe: $simulatedMonths months", style = MaterialTheme.typography.bodySmall)
                                
                                val calSim = Calendar.getInstance()
                                calSim.add(Calendar.MONTH, simulatedMonths)
                                val simYear = calSim.get(Calendar.YEAR)
                                Text("Free Year: $simYear", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandCyan)
                                
                                Text("Interest: ${formatCurrency(simulatedInterest)}", style = MaterialTheme.typography.bodySmall, color = BrandRose)
                            }
                        }

                        if (monthsSaved > 0) {
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Text(
                                text = "💡 Adding ${formatCurrency(extraPayment)} monthly reduces your payoff time by ${(monthsSaved / 12)} years and ${monthsSaved % 12} months!",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Dynamic Progress Speedup Graphic
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Payoff Speedup Graphic", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        val maxMonths = maxOf(1, baselineMonths)
                        val baselineProgress = 1.0f
                        val simulatedProgress = simulatedMonths.toFloat() / maxMonths

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Baseline Duration ($baselineMonths months)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(baselineProgress)
                                        .background(Color(0xFF64748B))
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text("Accelerated Duration ($simulatedMonths months)", style = MaterialTheme.typography.labelSmall, color = BrandCyan)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(BrandCyan.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(simulatedProgress)
                                        .background(Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd)))
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

