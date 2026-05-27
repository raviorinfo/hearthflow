package com.example.omnilog.ui

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnilog.data.model.InventoryItem
import com.example.omnilog.viewmodel.MainViewModel
import com.example.omnilog.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(viewModel: MainViewModel) {
    val inventory by viewModel.inventory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var restockItemName by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredInventory = remember(inventory, searchQuery, selectedCategory) {
        inventory.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                                item.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Out of Stock" -> item.quantity <= 0.0
                else -> item.category.equals(selectedCategory, ignoreCase = true)
            }
            matchesSearch && matchesCategory
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GradientText(
                            text = "Smart Pantry",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Manage inventory and track kitchen items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    Surface(
                        color = BrandViolet.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(0.5.dp, BrandViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Kitchen, null, tint = BrandViolet, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${inventory.size} Items",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandViolet
                            )
                        }
                    }
                }
            }

            // Frosted Search Bar
            item {
                var searchFocused by remember { mutableStateOf(false) }
                val focusGlow by animateFloatAsState(
                    targetValue = if (searchFocused) 0.6f else 0.15f,
                    animationSpec = tween(400),
                    label = "pantrySearchGlow"
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        BrandGradientStart.copy(alpha = focusGlow),
                        BrandGradientMid.copy(alpha = focusGlow * 0.6f),
                        BrandGradientEnd.copy(alpha = focusGlow)
                    )
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (searchFocused) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .blur(12.dp)
                                .background(
                                    BrandGradientStart.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x16FFFFFF))
                            .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search pantry stock...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { searchFocused = it.isFocused },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
            }

            // Premium Category Filtering Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterCategories = listOf("All", "Groceries", "Out of Stock")
                    filterCategories.forEach { category ->
                        val isSelected = selectedCategory == category
                        PremiumFilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = category
                        )
                    }
                }
            }

            // Pantry Inventory Title
            item {
                Text(
                    "Pantry Stock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Empty State
            if (filteredInventory.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(120.dp)
                            ) {
                                PulseRing(color = BrandViolet, size = 100.dp)
                                Icon(
                                    Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = BrandViolet,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isNotEmpty() || selectedCategory != "All") "No Items Match Filter" else "Your Pantry is Empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (searchQuery.isNotEmpty() || selectedCategory != "All") "Try adjusting your filters or search keywords." else "Log groceries manually or snap an image receipt using the voice/camera bar below to auto-populate groceries.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredInventory) { item ->
                    PantryItemRow(
                        item = item,
                        onRestockClick = { restockItemName = item.name },
                        onQtyAdjust = { delta ->
                            viewModel.adjustInventoryQty(item.name, delta)
                        }
                    )
                }
            }
        }

        // Animated glowing Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .blur(12.dp)
                    .background(BrandViolet.copy(alpha = 0.5f), CircleShape)
            )
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandViolet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.pressScale()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(24.dp))
            }
        }

        // Dialogs
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
fun PantryItemRow(
    item: InventoryItem,
    onRestockClick: () -> Unit,
    onQtyAdjust: (Double) -> Unit
) {
    val isOut = item.quantity <= 0.0
    val isLowStock = !isOut && item.quantity <= 1.5
    val colorAccent = when {
        isOut -> BrandRose
        isLowStock -> BrandAmber
        else -> BrandCyan
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            colorAccent.copy(alpha = 0.4f),
            Color(0x10FFFFFF),
            BrandGradientEnd.copy(alpha = 0.2f)
        )
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(),
        containerColor = if (isOut) Color(0x12EF4444) else null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorAccent)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when {
                                isOut -> "Out of stock ❌"
                                isLowStock -> "Low stock alert ⚠️"
                                else -> "Stock is healthy"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colorAccent
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decrement Button
                    IconButton(
                        onClick = { onQtyAdjust(-1.0) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0x1AFFFFFF), CircleShape)
                            .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }

                    Surface(
                        color = colorAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(0.5.dp, colorAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} ${item.unit}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Increment Button
                    IconButton(
                        onClick = { onQtyAdjust(1.0) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0x1AFFFFFF), CircleShape)
                            .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Button(
                onClick = onRestockClick,
                modifier = Modifier.fillMaxWidth().pressScale(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                border = borderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Replenish / Log Purchase", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}


// Utility for borders
fun borderStroke(width: Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun RestockDialog(itemName: String, onDismiss: () -> Unit, onRestock: (Double, Double) -> Unit) {
    var qty by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            GradientText(
                text = "Restock $itemName",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    "Log manual replenishment specs. This generates an EXPENSE log entry automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity Purchased") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = BrandCyan,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        cursorColor = BrandCyan
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Actual Amount Spent") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(getCurrencySymbol(), color = BrandCyan, fontWeight = FontWeight.Bold) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = BrandCyan,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        cursorColor = BrandCyan
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val q = qty.toDoubleOrNull() ?: 0.0
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (q > 0.0 && a >= 0.0) {
                        onRestock(q, a)
                    }
                },
                enabled = qty.toDoubleOrNull() != null && amount.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.pressScale()
            ) {
                Text("Post Purchase", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
        },
        containerColor = Color(0xFF131929),
        modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
    )
}

@Composable
fun ManualAddDialog(onDismiss: () -> Unit, onAdd: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val unitOptions = listOf("pcs", "kg", "Liters", "g", "ml", "Pack")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            GradientText(
                text = "Add Groceries",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = BrandCyan,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        cursorColor = BrandCyan
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Initial Stock Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = BrandCyan,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        cursorColor = BrandCyan
                    ),
                    singleLine = true
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Unit", tint = BrandCyan)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = BrandCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = BrandCyan,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            cursorColor = BrandCyan
                        ),
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { dropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF131929))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    ) {
                        unitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.White) },
                                onClick = {
                                    unit = option
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val q = qty.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && q >= 0.0) {
                        onAdd(name, q, unit)
                    }
                },
                enabled = name.isNotBlank() && qty.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.pressScale()
            ) {
                Text("Add to Pantry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
        },
        containerColor = Color(0xFF131929),
        modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
    )
}
