package com.example.omnilog.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnilog.data.model.LogEntry
import com.example.omnilog.ui.theme.*
import com.example.omnilog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPaymentLogsScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    val paymentLogs by viewModel.debtPayments.collectAsState()

    var selectedDebtForPayment by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                GradientText(
                    text = "Payment History",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Track ledger adjustments and manual payments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Log Payment Form Card
                if (debts.isNotEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Record New Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Deducts outstanding balance and registers as a ledger expense.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                
                                Spacer(Modifier.height(4.dp))

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x10FFFFFF))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .clickable { expandedDropdown = true }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedDebtForPayment.isEmpty()) "Select Target Debt" else selectedDebtForPayment,
                                                color = if (selectedDebtForPayment.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Icon(Icons.Default.ArrowDropDown, null, tint = BrandViolet)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expandedDropdown,
                                        onDismissRequest = { expandedDropdown = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .background(Color(0xFF131929))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    ) {
                                        debts.forEach { debt ->
                                            DropdownMenuItem(
                                                text = { Text("${debt.name} (${formatCurrency(debt.balance)})") },
                                                onClick = {
                                                    selectedDebtForPayment = debt.name
                                                    expandedDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = paymentAmount,
                                    onValueChange = { paymentAmount = it },
                                    label = { Text("Payment Amount", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

                                Spacer(Modifier.height(2.dp))

                                val isButtonEnabled = selectedDebtForPayment.isNotEmpty() && paymentAmount.toDoubleOrNull() != null
                                val buttonGradient = if (isButtonEnabled) {
                                    Brush.horizontalGradient(colors = listOf(BrandGradientStart, BrandGradientEnd))
                                } else {
                                    Brush.horizontalGradient(colors = listOf(Color(0x10FFFFFF), Color(0x10FFFFFF)))
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(buttonGradient)
                                        .clickable(enabled = isButtonEnabled) {
                                            val amt = paymentAmount.toDoubleOrNull()
                                            if (selectedDebtForPayment.isNotEmpty() && amt != null && amt > 0) {
                                                viewModel.logDebtPayment(selectedDebtForPayment, amt)
                                                paymentAmount = ""
                                                selectedDebtForPayment = ""
                                            }
                                        }
                                        .pressScale()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = if (isButtonEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Post Payment",
                                            color = if (isButtonEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Default.CreditCardOff, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("No Active Debts to Pay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Add debt accounts inside 'My Debts' to enable recording payments.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                // Payment History Feed Header
                item {
                    Text(
                        text = "Historical Activity Log (${paymentLogs.size} records)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Feed
                if (paymentLogs.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp).fillMaxWidth()
                            ) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(Modifier.height(12.dp))
                                Text("No payments registered yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Your posted debt installments will be archived chronologically in this feed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    items(paymentLogs.sortedByDescending { it.timestamp }) { log ->
                        PaymentLogItemCard(log)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentLogItemCard(log: LogEntry) {
    val amount = "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble()
    val debtName = "\"debtName\":\\s*\"([^\"]+)\"".toRegex().find(log.structuredData)?.groupValues?.get(1) ?: "Debt"

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = BrandIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp).border(1.dp, BrandIndigo.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = "Receipt",
                            tint = BrandIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = "$debtName Payment",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(log.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnimatedCounter(
                    value = amount ?: 0.0,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = BrandEmerald
                )
                NeonBadge(
                    text = "POSTED",
                    color = BrandEmerald
                )
            }
        }
    }
}
