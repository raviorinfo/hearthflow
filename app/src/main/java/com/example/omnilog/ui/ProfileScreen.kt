package com.example.omnilog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.*
import com.example.omnilog.viewmodel.MainViewModel

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val userAccount by viewModel.userAccount.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }
    var showFamilyDialog by remember { mutableStateOf(false) }
    var familyEmail by remember { mutableStateOf("") }
    var tempP by remember { mutableStateOf("") }
    var tempC by remember { mutableStateOf("") }
    var tempF by remember { mutableStateOf("") }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Edit Daily Goals") },
            text = {
                Column {
                    OutlinedTextField(value = tempP, onValueChange = { tempP = it }, label = { Text("Protein (g)") })
                    OutlinedTextField(value = tempC, onValueChange = { tempC = it }, label = { Text("Carbs (g)") })
                    OutlinedTextField(value = tempF, onValueChange = { tempF = it }, label = { Text("Fat (g)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateMacroGoals(tempP.toIntOrNull() ?: 0, tempC.toIntOrNull() ?: 0, tempF.toIntOrNull() ?: 0)
                    showGoalDialog = false
                }) { Text("Save") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("User Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            
            // Profile Picture Placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(Modifier.height(16.dp))
            
            userAccount?.let { account ->
                Text(text = account.name ?: "Unknown User", style = MaterialTheme.typography.headlineSmall)
                Text(text = account.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                
                Spacer(Modifier.height(32.dp))
                
                // Account Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCard("AI Credits", if (account.isPro) "∞" else account.aiCredits.toString())
                    StatCard("Status", if (account.isPro) "PRO" else "FREE")
                }

                Spacer(Modifier.height(32.dp))

                // Macro Goals Section
                GlassCard(
                    modifier = Modifier.fillMaxWidth().pressScale().clickable {
                        tempP = account.proteinGoal.toString()
                        tempC = account.carbsGoal.toString()
                        tempF = account.fatGoal.toString()
                        showGoalDialog = true
                    }
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily Macro Goals", style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        MacroRow("Protein", "${account.proteinGoal}g")
                        MacroRow("Carbs", "${account.carbsGoal}g")
                        MacroRow("Fat", "${account.fatGoal}g")
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Security Section
                Text("Security", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Biometric Authentication", style = MaterialTheme.typography.bodyLarge)
                            Text("Unlock app with fingerprint or face", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = account.biometricEnabled,
                            onCheckedChange = { viewModel.toggleBiometric(it) }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Family Sharing Section
                Text("Family Sharing", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (account.isPro) null else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ) {
                    if (account.isPro) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Shared Members", style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = { showFamilyDialog = true }) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            familyMembers.forEach { member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.width(12.dp))
                                    Text(member, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(8.dp))
                            Text("Family Sharing Locked", style = MaterialTheme.typography.titleSmall)
                            Text("Upgrade to Pro to add family members", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { /* Store navigation handled by UI */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Get Family Pro")
                            }
                        }
                    }
                }
            }
        }

        if (showFamilyDialog) {
            item {
                AlertDialog(
                    onDismissRequest = { showFamilyDialog = false },
                    title = { Text("Invite Family Member") },
                    text = {
                        OutlinedTextField(
                            value = familyEmail,
                            onValueChange = { familyEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addFamilyMember(familyEmail)
                            familyEmail = ""
                            showFamilyDialog = false
                        }) { Text("Invite") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFamilyDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
        
        item {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { /* TODO: Sign Out */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth().pressScale()
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    GlassCard(
        modifier = Modifier.size(width = 140.dp, height = 90.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MacroRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
    }
}
