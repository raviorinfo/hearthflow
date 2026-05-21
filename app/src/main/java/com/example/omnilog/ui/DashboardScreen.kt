package com.example.omnilog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.omnilog.data.model.LogCategory
import com.example.omnilog.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.AutoAwesome

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val logs by viewModel.allLogs.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()
    
    val lowStockAlerts = inventory.filter { it.daysRemaining > 0 && it.daysRemaining <= 3 }
    val recentLogs = logs.take(5)
    val totalExpenses = logs.filter { it.category == LogCategory.EXPENSE }
        .sumOf { log ->
            "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble() ?: 0.0
        }

    val categoryDistribution = logs.groupBy { it.category }
        .mapValues { it.value.size.toFloat() }
        .mapKeys { it.key.name }

    val categoryColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.headlineMedium
            )
            userAccount?.let {
                Text(
                    text = if (it.isPro) "RoutineLog Pro Account" else "Credits remaining: ${it.aiCredits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("Expenses", style = MaterialTheme.typography.labelMedium)
                    Text(formatCurrency(totalExpenses), style = MaterialTheme.typography.titleLarge)
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("Pantry Items", style = MaterialTheme.typography.labelMedium)
                    Text("${inventory.size}", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activity Mix", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (categoryDistribution.isEmpty()) {
                            Text("No activity logged yet. Start by adding a log below!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        } else {
                            categoryDistribution.forEach { (cat, count) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(categoryColors[categoryDistribution.keys.indexOf(cat) % categoryColors.size], RoundedCornerShape(2.dp)))
                                    Spacer(Modifier.width(8.dp))
                                    Text("$cat: ${count.toInt()}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    PieChart(
                        data = if (categoryDistribution.isEmpty()) mapOf("Empty" to 1f) else categoryDistribution,
                        colors = if (categoryDistribution.isEmpty()) listOf(MaterialTheme.colorScheme.surfaceVariant) else categoryColors,
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AI Insights", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (lowStockAlerts.isEmpty()) "All systems normal. Your pantry is well-stocked."
                            else "Action required: ${lowStockAlerts.size} items are running low.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (lowStockAlerts.isNotEmpty()) {
            item {
                GlassCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ) {
                    Column {
                        Row {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Predictive Alerts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(8.dp))
                        lowStockAlerts.forEach { alert ->
                            Text(
                                "• ${alert.name} will run out in ${alert.daysRemaining} days",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Recent Activity", style = MaterialTheme.typography.titleLarge)
        }

        items(recentLogs) { log ->
            LogItem(log)
        }
    }
}
