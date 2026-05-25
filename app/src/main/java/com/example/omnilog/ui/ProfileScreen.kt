package com.example.omnilog.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnilog.viewmodel.MainViewModel
import com.example.omnilog.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MainViewModel, onSignOut: () -> Unit = {}) {
    val userAccount by viewModel.userAccount.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val isPremiumActive by viewModel.isPremiumActive.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }
    var showFamilyDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showEncryptingProgressDialog by remember { mutableStateOf(false) }
    var targetEncryptionState by remember { mutableStateOf(false) }
    var familyEmail by remember { mutableStateOf("") }
    var tempP by remember { mutableStateOf("") }
    var tempC by remember { mutableStateOf("") }
    var tempF by remember { mutableStateOf("") }

    val isDark = isSystemInDarkTheme()

    // Helper function to get text field colors dynamically
    @Composable
    fun getDynamicTextFieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = accentColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        focusedLabelColor = accentColor,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        cursorColor = accentColor,
        focusedContainerColor = if (isDark) Color(0xFF1C2438) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        unfocusedContainerColor = if (isDark) Color(0xFF131929) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    )

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                GradientText(
                    text = "Daily Nutritional Targets",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = tempP,
                        onValueChange = { tempP = it },
                        label = { Text("Protein Target (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDynamicTextFieldColors(BrandRose),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempC,
                        onValueChange = { tempC = it },
                        label = { Text("Carbohydrates Target (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDynamicTextFieldColors(BrandCyan),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempF,
                        onValueChange = { tempF = it },
                        label = { Text("Fat Target (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDynamicTextFieldColors(BrandGold),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMacroGoals(
                            tempP.toIntOrNull() ?: 0,
                            tempC.toIntOrNull() ?: 0,
                            tempF.toIntOrNull() ?: 0
                        )
                        showGoalDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.pressScale()
                ) {
                    Text("Save Goals", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { 
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
        )
    }

    if (showFamilyDialog) {
        AlertDialog(
            onDismissRequest = { showFamilyDialog = false },
            title = {
                GradientText(
                    text = "Add Family Member",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = familyEmail,
                        onValueChange = { familyEmail = it },
                        label = { Text("Family Member's Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDynamicTextFieldColors(BrandViolet),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (familyEmail.isNotBlank()) {
                            viewModel.addFamilyMember(familyEmail)
                            familyEmail = ""
                            showFamilyDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.pressScale()
                ) {
                    Text("Send Invitation", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFamilyDialog = false }) { 
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                GradientText(
                    text = "Account Hub",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(16.dp))
                
                // Profile Picture Placeholder with circular glass styling and concentric pulse rings
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    PulseRing(color = BrandViolet, size = 130.dp)
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandGradientStart.copy(alpha = 0.2f), BrandGradientEnd.copy(alpha = 0.2f))
                                )
                            )
                            .border(1.5.dp, BrandViolet.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = BrandViolet
                        )
                    }

                    // Superimposed active tier indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 20.dp, end = 20.dp)
                            .size(32.dp)
                            .blur(4.dp)
                            .background(BrandGold, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 20.dp, end = 20.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFFFFF7ED), BrandGold))
                            )
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF78350F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                userAccount?.let { account ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = account.name.ifBlank { "RoutineLog User" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = account.email.ifBlank { "user@routinelog.com" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            item {
                // Account Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.History,
                        iconColor = BrandCyan,
                        label = "Total Logs",
                        value = "${allLogs.size} logged",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Groups,
                        iconColor = BrandViolet,
                        label = "Family Plan",
                        value = "${familyMembers.size} active",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Subscription Status Card
            item {
                if (isPremiumActive) {
                    val expiry = userAccount?.proExpiryTimestamp ?: 0L
                    val daysLeft = ((expiry - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)
                    val planName = userAccount?.subscriptionPlan ?: "Premium Pro"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E1E28), Color(0xFF2C2516))
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD700), Color(0x33FFFFFF), Color(0xFFFFD700))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A1600))
                                        .border(1.5.dp, Color(0xFFFFD700), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👑", fontSize = 20.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = planName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    )
                                    Text(
                                        text = "Full access · $daysLeft days remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            TextButton(
                                onClick = { showPremiumDialog = true },
                                modifier = Modifier.pressScale()
                            ) {
                                Text(
                                    text = "Renew",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD700)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF13131A), Color(0xFF1C1E3A))
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(BrandViolet.copy(alpha = 0.5f), BrandCyan.copy(alpha = 0.5f))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { showPremiumDialog = true }
                            .pressScale()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(8.dp))
                                    GradientText(
                                        text = "You\'re on Free Plan",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Upgrade to unlock unlimited splits, groups, debts & investments. Plans from just \u20b925/mo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { showPremiumDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandViolet,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.pressScale()
                            ) {
                                Text(
                                    "Upgrade",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Edit Personal Details Card
            item {
                userAccount?.let { account ->
                    var name by remember(account) { mutableStateOf(account.name) }
                    var email by remember(account) { mutableStateOf(account.email) }
                    
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(BrandCyan, BrandViolet)),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ManageAccounts, null, tint = BrandCyan, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Edit Personal Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = getDynamicTextFieldColors(BrandCyan),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = getDynamicTextFieldColors(BrandCyan),
                                singleLine = true
                            )
                            
                            Button(
                                onClick = {
                                    if (name.isNotBlank() && email.isNotBlank()) {
                                        viewModel.updateProfile(account.copy(name = name, email = email))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().pressScale(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save Details")
                            }
                        }
                    }
                }
            }

            // Daily Macro targets
            item {
                userAccount?.let { account ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(BrandViolet, BrandGold)),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .pressScale()
                            .clickable {
                                tempP = account.proteinGoal.toString()
                                tempC = account.carbsGoal.toString()
                                tempF = account.fatGoal.toString()
                                showGoalDialog = true
                            }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FitnessCenter, null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (account.name.isBlank()) "Daily Nutritional Targets" else "${account.name}'s Daily Nutritional Targets",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            
                            MacroRow("Protein Goal", "${account.proteinGoal}g", BrandRose)
                            MacroRow("Carbohydrates Goal", "${account.carbsGoal}g", BrandCyan)
                            MacroRow("Healthy Fats Goal", "${account.fatGoal}g", BrandGold)
                        }
                    }
                }
            }

            // Security Toggle
            item {
                userAccount?.let { account ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Security Credentials",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, null, tint = BrandCyan, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Biometric Authentication", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Log in using secure biometrics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = account.biometricEnabled,
                                    onCheckedChange = { viewModel.toggleBiometric(it) }
                                )
                            }
                        }
                    }
                }
            }

            // Family sharing (fully unlocked)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Family Networking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(BrandCyan, BrandViolet)),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Groups, null, tint = BrandViolet, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Active Family Plan",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                IconButton(
                                    onClick = { showFamilyDialog = true },
                                    modifier = Modifier.pressScale()
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = "Add Member", tint = BrandViolet)
                                }
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                            familyMembers.forEach { member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            Icons.Default.Face,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = BrandCyan
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(member, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeFamilyMember(member) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Member",
                                            tint = BrandRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🔒 General Settings & Legal Options
            item {
                Spacer(Modifier.height(16.dp))
                val isEncrypted by viewModel.isDatabaseEncrypted.collectAsState()
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null, tint = BrandCyan, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Workspace Settings & Legals",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        
                        // About Us row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAboutDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("About RoutineLog", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        }
                        
                        // Privacy Policy row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPrivacyDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Privacy Policy", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Database Encryption Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (isEncrypted) BrandEmerald else BrandAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Core Database Security",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (isEncrypted) "🔒 Secured via AES-256 Key lock" else "🔓 Standard plain-text storage active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isEncrypted) BrandEmerald else BrandAmber,
                                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                                )
                            }
                            
                            Button(
                                onClick = {
                                    targetEncryptionState = !isEncrypted
                                    showEncryptingProgressDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEncrypted) BrandRose.copy(alpha = 0.15f) else BrandEmerald.copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(
                                    0.5.dp, 
                                    if (isEncrypted) BrandRose.copy(alpha = 0.4f) else BrandEmerald.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.pressScale()
                            ) {
                                Text(
                                    text = if (isEncrypted) "Decrypt" else "Encrypt",
                                    color = if (isEncrypted) BrandRose else BrandEmerald,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Sign Out Button
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showSignOutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose.copy(alpha = 0.2f)),
                    border = BorderStroke(0.5.dp, BrandRose.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().pressScale()
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = BrandRose, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out of Account", color = BrandRose, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Premium subscription dialog
    if (showPremiumDialog) {
        PremiumSubscriptionDialog(
            onDismiss = { showPremiumDialog = false },
            onPurchase = { plan, duration ->
                viewModel.purchasePremiumPlan(plan, duration)
                showPremiumDialog = false
            }
        )
    }

    // Sign-out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(Icons.Default.ExitToApp, null, tint = BrandRose, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Sign Out?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "You'll be returned to the login screen. Your data remains stored on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // About Us Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(Icons.Default.Info, null, tint = BrandCyan, modifier = Modifier.size(32.dp))
            },
            title = {
                GradientText(
                    text = "About RoutineLog",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Version v1.4.2",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandGold,
                        modifier = Modifier
                            .background(BrandGold.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "RoutineLog is a premium offline-first financial ledger and local tracking system designed for privacy, speed, and visual elegance.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Our mission is to empower individuals and families to take complete control of their personal finance, macros, and split sharing ledger without leaking private details to the cloud. All operations are kept 100% inside your local device sandbox.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Crafted with ❤️ by the RoutineLog Dev Team",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BrandViolet
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                    modifier = Modifier.pressScale()
                ) {
                    Text("Great", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = {
                Icon(Icons.Default.VerifiedUser, null, tint = BrandEmerald, modifier = Modifier.size(32.dp))
            },
            title = {
                GradientText(
                    text = "Privacy Shield",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandEmerald.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, BrandEmerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Your data is stored 100% locally on this device. We do not sell or upload your personal finance or tracking information.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandEmerald
                            )
                        }
                    }
                    Text(
                        text = "Because RoutineLog operates on an offline-first architecture, your private details remain entirely under your control. We do not maintain any cloud databases, nor do we run remote tracking algorithms on your transaction logs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Any network interactions (such as simulated payments or invite links) are structured purely as sandboxed client-side processes. Your device is your vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                    modifier = Modifier.pressScale()
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Database Encryption Progress Dialog
    if (showEncryptingProgressDialog) {
        LaunchedEffect(targetEncryptionState) {
            delay(2200) // Simulated cryptographic workload
            viewModel.toggleDatabaseEncryption(targetEncryptionState)
            showEncryptingProgressDialog = false
        }

        AlertDialog(
            onDismissRequest = { /* Prevent dismissal during cryptographic operations */ },
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        CircularProgressIndicator(
                            color = if (targetEncryptionState) BrandEmerald else BrandAmber,
                            strokeWidth = 4.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = if (targetEncryptionState) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (targetEncryptionState) BrandEmerald else BrandAmber,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = if (targetEncryptionState) "Securing Workspace Database" else "Unlocking Database Storage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (targetEncryptionState) 
                            "Generating unique AES-256 local keys...\nSecuring storage files on-device..." 
                        else 
                            "Safely purging cryptographic keys...\nRestoring standard SQLite access...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}


@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun MacroRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = accentColor)
    }
}
