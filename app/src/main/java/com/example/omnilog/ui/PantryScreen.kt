package com.example.omnilog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.omnilog.ui.getCurrencySymbol
import com.example.omnilog.viewmodel.MainViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ShoppingCart

@Composable
fun PantryScreen(viewModel: MainViewModel) {
    val inventory by viewModel.inventory.collectAsState()
    val mealSuggestion by viewModel.mealSuggestion.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var restockItemName by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text("Your Pantry", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).pressScale()
                ) {
                    Column {
                        Button(
                            onClick = { viewModel.suggestMeal() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Get AI Meal Suggestion")
                        }

                        mealSuggestion?.let { suggestion ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            items(inventory) { item ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).pressScale()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("Last updated: Just now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${item.quantity} ${item.unit}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Button(
                            onClick = { restockItemName = item.name },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mark as Purchased", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Item")
        }

        if (showAddDialog) {
            ManualAddDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, qty, unit ->
                    viewModel.addInventoryManually(name, qty, unit)
                    showAddDialog = false
                }
            )
        }

        restockItemName?.let { name ->
            RestockDialog(
                itemName = name,
                onDismiss = { restockItemName = null },
                onRestock = { qty, amount ->
                    viewModel.restockInventory(name, qty, amount)
                    restockItemName = null
                }
            )
        }
    }
}

@Composable
fun RestockDialog(itemName: String, onDismiss: () -> Unit, onRestock: (Double, Double) -> Unit) {
    var qty by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock $itemName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the details of your purchase.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity Purchased") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Actual Amount Spent (${getCurrencySymbol()})") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(getCurrencySymbol()) }
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                onRestock(qty.toDoubleOrNull() ?: 0.0, amount.toDoubleOrNull() ?: 0.0)
            }) {
                Text("Confirm & Log Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManualAddDialog(onDismiss: () -> Unit, onAdd: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pantry Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") })
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (pcs, kg, etc.)") })
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, qty.toDoubleOrNull() ?: 0.0, unit) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
