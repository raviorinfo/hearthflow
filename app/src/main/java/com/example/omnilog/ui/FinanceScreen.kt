package com.example.omnilog.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
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
import com.example.omnilog.data.model.LogCategory
import com.example.omnilog.viewmodel.MainViewModel
import com.example.omnilog.ui.theme.*

@Composable
fun FinanceScreen(viewModel: MainViewModel, navController: NavHostController) {
    val allRawLogs by viewModel.allLogs.collectAsState()
    val logs = remember(allRawLogs) {
        allRawLogs.filter { !it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") }
    }
    val expenses = logs.filter { it.category == LogCategory.EXPENSE }
    val context = LocalContext.current

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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
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
                            "Tracking ${expenses.size} active transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (expenses.isNotEmpty()) {
                        // Group by description or item for small pie chart
                        val breakdown = expenses.mapNotNull { log ->
                            "\"item\":\\s*\"([^\"]+)\"".toRegex().find(log.structuredData)?.groupValues?.get(1)
                        }.groupBy { it }.mapValues { it.value.size.toFloat() }

                        if (breakdown.isNotEmpty()) {
                            PieChart(
                                data = breakdown,
                                colors = listOf(BrandViolet, BrandIndigo, BrandCyan, BrandAmber, BrandRose),
                                modifier = Modifier.size(90.dp).weight(0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Split Bill Launcher Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.SplitExpense.route) }
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(BrandCyan, BrandViolet)),
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
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(BrandCyan.copy(alpha = 0.15f))
                            .border(1.dp, BrandCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = "Split bills",
                            tint = BrandCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Split Bills & Expenses",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Record joint liabilities with family, split cash 50/50, and settle up balances seamlessly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Split Bills",
                        tint = BrandCyan,
                        modifier = Modifier.size(20.dp)
                    )
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
