package com.example.omnilog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.omnilog.data.model.DebtEntry
import com.example.omnilog.viewmodel.MainViewModel
import java.util.*

@Composable
fun DebtPlannerScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    val profile by viewModel.financialProfile.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()
    
    var selectedStrategy by remember { mutableStateOf("Snowball") }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val timeline = viewModel.calculatePayoffTimeline(selectedStrategy)
    val isInsufficientIncome = timeline.any { it.month == -1 }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Debt Planner", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { showProfileDialog = true }) {
                Icon(Icons.Default.Settings, "Financial Settings")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (profile == null) {
            OnboardingFinancialCard(onSetup = { showProfileDialog = true })
        } else {
            // Summary Dashboard
            FinancialSummaryGrid(
                timeline = timeline,
                debtsCount = debts.size,
                isInsufficient = isInsufficientIncome
            )

            Spacer(Modifier.height(24.dp))

            // Strategy Selector
            StrategyToggle(
                selected = selectedStrategy,
                onSelect = { selectedStrategy = it }
            )

            Spacer(Modifier.height(16.dp))

            // Roadmap / List
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Debts", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddDebtDialog = true }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Text("Add Debt")
                        }
                    }
                }

                items(debts) { debt ->
                    DebtItemCard(debt, onDelete = { viewModel.deleteDebt(debt.id) })
                }

                item { Spacer(Modifier.height(24.dp)) }

                item {
                    Text("Payoff Roadmap", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }

                if (timeline.isNotEmpty() && !isInsufficientIncome) {
                    val isPro = userAccount?.isPro == true
                    
                    items(if (isPro) timeline else timeline.take(3)) { snapshot ->
                        RoadmapMonthCard(snapshot)
                    }

                    if (!isPro && timeline.size > 3) {
                        item {
                            PremiumPaywallCard(
                                monthsRemaining = timeline.size - 3,
                                totalInterest = timeline.last().totalInterestPaid
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showProfileDialog) {
        ProfileSetupDialog(
            initialSalary = profile?.monthlySalary ?: 0.0,
            initialExpenses = profile?.fixedExpenses ?: 0.0,
            onDismiss = { showProfileDialog = false },
            onSave = { s, e -> 
                viewModel.updateFinancialProfile(s, e)
                showProfileDialog = false
            }
        )
    }

    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { n, b, r, m ->
                viewModel.addDebt(n, b, r, m)
                showAddDebtDialog = false
            }
        )
    }
}

@Composable
fun OnboardingFinancialCard(onSetup: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Ready to be Debt-Free?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Setup your monthly budget to generate a personalized payoff roadmap.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onSetup, modifier = Modifier.fillMaxWidth()) {
                Text("Start Planning")
            }
        }
    }
}

@Composable
fun FinancialSummaryGrid(timeline: List<MainViewModel.PayoffSnapshot>, debtsCount: Int, isInsufficient: Boolean) {
    val totalDebt = if (timeline.isNotEmpty()) timeline.first().totalBalance else 0.0
    val monthsToFree = timeline.size
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Total Debt", style = MaterialTheme.typography.labelSmall)
                Text(formatCurrency(totalDebt), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        GlassCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Debt-Free Date", style = MaterialTheme.typography.labelSmall)
                if (isInsufficient) {
                    Text("Error", color = MaterialTheme.colorScheme.error)
                } else {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, monthsToFree)
                    val year = cal.get(Calendar.YEAR)
                    Text("$year", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun StrategyToggle(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        listOf("Snowball", "Avalanche").forEach { strategy ->
            val isSelected = selected == strategy
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(strategy) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    strategy,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun DebtItemCard(debt: DebtEntry, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(debt.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${formatCurrency(debt.balance)} @ ${debt.interestRate}%", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun RoadmapMonthCard(snapshot: MainViewModel.PayoffSnapshot) {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, snapshot.month)
    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
    val year = cal.get(Calendar.YEAR)

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
            Text(monthName, style = MaterialTheme.typography.labelMedium)
            Text(year.toString().takeLast(2), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        
        Spacer(Modifier.width(8.dp))
        
        GlassCard(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Remaining", style = MaterialTheme.typography.bodyMedium)
                Text(formatCurrency(snapshot.totalBalance), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PremiumPaywallCard(monthsRemaining: Int, totalInterest: Double) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Unlock Full Roadmap", style = MaterialTheme.typography.titleMedium)
            Text(
                "You have $monthsRemaining more months projected. Upgrade to Pro to see exactly how to save ${formatCurrency(totalInterest * 0.2)} in interest.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(onClick = { /* Navigate to store */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Get Pro Access")
            }
        }
    }
}

@Composable
fun ProfileSetupDialog(initialSalary: Double, initialExpenses: Double, onDismiss: () -> Unit, onSave: (Double, Double) -> Unit) {
    var salary by remember { mutableStateOf(initialSalary.toString()) }
    var expenses by remember { mutableStateOf(initialExpenses.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Financial Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Take-Home Salary") }, prefix = { Text(getCurrencySymbol()) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = expenses, onValueChange = { expenses = it }, label = { Text("Fixed Expenses (Rent, Bills)") }, prefix = { Text(getCurrencySymbol()) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(salary.toDoubleOrNull() ?: 0.0, expenses.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddDebtDialog(onDismiss: () -> Unit, onSave: (String, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Debt Name (e.g. Visa)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Current Balance") }, prefix = { Text(getCurrencySymbol()) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Annual Interest Rate (%)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = min, onValueChange = { min = it }, label = { Text("Minimum Monthly Payment") }, prefix = { Text(getCurrencySymbol()) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(name, balance.toDoubleOrNull() ?: 0.0, rate.toDoubleOrNull() ?: 0.0, min.toDoubleOrNull() ?: 0.0) 
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
