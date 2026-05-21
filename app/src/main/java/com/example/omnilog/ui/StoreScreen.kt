package com.example.omnilog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.omnilog.viewmodel.MainViewModel
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(viewModel: MainViewModel) {
    val userAccount by viewModel.userAccount.collectAsState()
    val scope = rememberCoroutineScope()
    var processingPayment by remember { mutableStateOf(false) }

    if (processingPayment) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Processing Payment") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting to secure gateway...")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("RoutineLog Store", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Power up your experience with AI credits", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        
        Spacer(Modifier.height(24.dp))

        // Credits Summary
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Current Balance", style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (userAccount?.isPro == true) "Unlimited Pro" else "${userAccount?.aiCredits ?: 0} Credits",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("Select a Plan", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        // Bronze
        PurchaseOption(
            title = "Bronze Pack",
            description = "10 AI Credits",
            price = getStorePrice(0.99),
            onClick = {
                processingPayment = true
                scope.launch {
                    delay(2000)
                    viewModel.purchaseCredits(10)
                    processingPayment = false
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // Silver
        PurchaseOption(
            title = "Silver Pack",
            description = "50 AI Credits",
            price = getStorePrice(3.99),
            isPopular = true,
            onClick = {
                processingPayment = true
                scope.launch {
                    delay(2000)
                    viewModel.purchaseCredits(50)
                    processingPayment = false
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // Pro
        PurchaseOption(
            title = "Unlimited Pro",
            description = "Full access, no credit limits",
            price = "${getStorePrice(9.99)}/mo",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            onClick = {
                processingPayment = true
                scope.launch {
                    delay(2000)
                    viewModel.upgradeToPro()
                    processingPayment = false
                }
            }
        )
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PurchaseOption(
    title: String,
    description: String,
    price: String,
    isPopular: Boolean = false,
    containerColor: Color? = null,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().pressScale().clickable(onClick = onClick),
        containerColor = containerColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isPopular) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            "MOST POPULAR",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            
            Button(onClick = onClick) {
                Text(price)
            }
        }
    }
}
