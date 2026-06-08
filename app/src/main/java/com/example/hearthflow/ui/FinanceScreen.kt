package com.example.hearthflow.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.roundToLong
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.hearthflow.data.model.LogCategory
import com.example.hearthflow.viewmodel.MainViewModel
import com.example.hearthflow.ui.theme.*

@Composable
fun FinanceScreen(viewModel: MainViewModel, navController: NavHostController) {
    val allRawLogs by viewModel.allLogs.collectAsState()
    val logs = remember(allRawLogs) {
        allRawLogs.filter { !it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") }
    }
    val expenses = logs.filter { it.category == LogCategory.EXPENSE }
    val context = LocalContext.current
    
    val profile by viewModel.financialProfile.collectAsState()
    val disc = remember(profile) {
        val calculated = (profile?.monthlySalary ?: 0.0) - (profile?.fixedExpenses ?: 0.0) - (profile?.monthlyInvestments ?: 0.0)
        if (calculated <= 0.0) 25000.0 else calculated
    }

    val totalExpenses = expenses.sumOf { log ->
        "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble() ?: 0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Title Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    GradientText(
                        text = "Finance Hub",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Track your daily consumer expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                IconButton(
                    onClick = {
                        val report = viewModel.generateExpenseReport()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export CSV Report"))
                    },
                    modifier = Modifier
                        .pressScale()
                        .border(0.5.dp, BrandViolet.copy(alpha = 0.4f), CircleShape)
                        .background(BrandViolet.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        tint = BrandViolet
                    )
                }
            }
        }

        // Monthly Summary Card
        item {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val cardBg = if (isDark) Color(0x1F111827) else Color(0xDDFFFFFF)
            
            val ratio = if (disc > 0.0) (totalExpenses / disc).toFloat().coerceIn(0f, 1f) else 0f
            val infiniteTransition = rememberInfiniteTransition(label = "financePulse")
            val pulseGlow by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseGlow"
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale()
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                BrandCyan.copy(alpha = pulseGlow),
                                BrandViolet.copy(alpha = pulseGlow * 0.4f),
                                BrandCyan.copy(alpha = pulseGlow)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                containerColor = cardBg
            ) {
                // Background mesh glowing orbs
                Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(BrandCyan.copy(alpha = 0.12f), Color.Transparent)),
                            radius = size.width * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(x = size.width * 0.8f, y = size.height * 0.2f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                "Total Monthly Spending",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(4.dp))
                            AnimatedCounter(
                                value = totalExpenses,
                                prefix = "₹",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = BrandCyan
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Budget: ${formatCurrency(disc)} · Limit Used",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Circular Discretionary Spending Ratio Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(90.dp).weight(0.8f)
                        ) {
                            val sweepAngle = ratio * 360f
                            val animatedSweep by animateFloatAsState(
                                targetValue = sweepAngle,
                                animationSpec = tween(1200, easing = EaseOutCubic),
                                label = "gaugeSweep"
                            )
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(75.dp)) {
                                // Track circle
                                drawArc(
                                    color = Color.White.copy(alpha = 0.08f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                                // Active progress arc
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(BrandCyan, BrandViolet, BrandCyan)),
                                    startAngle = 270f,
                                    sweepAngle = animatedSweep,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(ratio * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                    color = Color.White
                                )
                                Text(
                                    text = "Used",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // Split Bill Launcher Card
        item {
            val infiniteTransition = rememberInfiniteTransition(label = "splitLauncher")
            val pulseGlow by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "launcherGlow"
            )
            
            val launcherBorder = Brush.linearGradient(
                listOf(
                    BrandCyan.copy(alpha = pulseGlow),
                    BrandViolet.copy(alpha = pulseGlow * 0.4f),
                    BrandCyan.copy(alpha = pulseGlow)
                )
            )
            
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale()
                    .clickable { navController.navigate(Screen.SplitExpense.route) }
                    .border(
                        width = 1.2.dp,
                        brush = launcherBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(BrandCyan.copy(alpha = 0.15f))
                            .border(1.5.dp, BrandCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = "Split bills",
                            tint = BrandCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Split Bills & Expenses",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Record joint liabilities with family, split cash 50/50, and settle up balances seamlessly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Split Bills",
                            tint = BrandCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Transactions List Title
        item {
            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Empty state
        if (expenses.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(120.dp)
                        ) {
                            PulseRing(color = BrandCyan, size = 100.dp)
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BrandCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No Expenses Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Log consumer spending manually or use the voice logging helper below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            items(expenses) { log ->
                LogItem(log)
            }
        }
    }
}
