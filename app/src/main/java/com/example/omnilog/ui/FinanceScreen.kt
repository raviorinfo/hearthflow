package com.example.omnilog.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.omnilog.data.model.LogCategory
import com.example.omnilog.viewmodel.MainViewModel

@Composable
fun FinanceScreen(viewModel: MainViewModel) {
    val logs by viewModel.allLogs.collectAsState()
    val expenses = logs.filter { it.category == LogCategory.EXPENSE }
    val context = LocalContext.current

    val totalExpenses = expenses.sumOf { log ->
        "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble() ?: 0.0
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Finance Hub", style = MaterialTheme.typography.headlineMedium)
            IconButton(
                onClick = {
                    val report = viewModel.generateExpenseReport()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export CSV"))
                },
                modifier = Modifier.pressScale()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Monthly Summary Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Monthly Spending", style = MaterialTheme.typography.labelMedium)
                    Text(formatCurrency(totalExpenses), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Tracking ${expenses.size} transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }

                if (expenses.isNotEmpty()) {
                    // Group by description or item for small pie chart
                    val breakdown = expenses.mapNotNull { log ->
                        "\"item\":\\s*\"([^\"]+)\"".toRegex().find(log.structuredData)?.groupValues?.get(1)
                    }.groupBy { it }.mapValues { it.value.size.toFloat() }

                    if (breakdown.isNotEmpty()) {
                        PieChart(
                            data = breakdown,
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses) { log ->
                LogItem(log)
            }
        }
    }
}
