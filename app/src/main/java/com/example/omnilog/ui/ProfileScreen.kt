package com.example.omnilog.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

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
    var showContactDialog by remember { mutableStateOf(false) }
    var showEncryptingProgressDialog by remember { mutableStateOf(false) }
    var targetEncryptionState by remember { mutableStateOf(false) }
    var familyEmail by remember { mutableStateOf("") }
    var tempP by remember { mutableStateOf("") }
    var tempC by remember { mutableStateOf("") }
    var tempF by remember { mutableStateOf("") }

    // Interactive contact support states
    var contactCategory by remember { mutableStateOf("General Support") }
    var contactMessage by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var isSubmittingContact by remember { mutableStateOf(false) }
    var showContactSuccess by remember { mutableStateOf(false) }
    var generatedTicketId by remember { mutableStateOf("") }
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()

    val privacyPolicyText by viewModel.privacyPolicyText.collectAsState()
    val aboutUsText by viewModel.aboutUsText.collectAsState()
    val priceMonthly by viewModel.priceMonthlyPlan.collectAsState()
    val priceYearly by viewModel.priceYearlyPlan.collectAsState()
    val priceLifetime by viewModel.priceLifetimePlan.collectAsState()
    val paymentGatewayProvider by viewModel.paymentGatewayProvider.collectAsState()
    val paymentGatewayPublicKey by viewModel.paymentGatewayPublicKey.collectAsState()
    val paymentGatewaySecretKey by viewModel.paymentGatewaySecretKey.collectAsState()
    val paymentGatewayUpiId by viewModel.paymentGatewayUpiId.collectAsState()
    val isAdmin by viewModel.isAdminUser.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()

    var showAdminConsoleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userAccount) {
        if (contactEmail.isEmpty() && userAccount != null) {
            contactEmail = userAccount?.email ?: ""
        }
    }

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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        
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
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("About RoutineLog", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
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
                                Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Privacy Policy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        }

                        // Contact Us row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showContactDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Contact Support", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        }

                        // ⚙️ Admin Console Master Row (exclusive to Admin users)
                        if (isAdmin) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(BrandViolet.copy(alpha = 0.15f), BrandCyan.copy(alpha = 0.15f))
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(listOf(BrandViolet, BrandCyan)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showAdminConsoleDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Settings, null, tint = BrandViolet, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "⚙️ Admin Master Console",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = BrandViolet
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = BrandViolet, modifier = Modifier.size(18.dp))
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

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
                                        color = MaterialTheme.colorScheme.onSurface
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

            // Invite Friends Section
            item {
                Spacer(Modifier.height(16.dp))
                var inviteEmail by remember { mutableStateOf("") }
                val context = LocalContext.current
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GroupAdd, null, tint = BrandViolet, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Invite Friends",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandViolet
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Invite friends to start tracking their routines and finances securely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = inviteEmail,
                            onValueChange = { inviteEmail = it },
                            label = { Text("Friend's Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = getDynamicTextFieldColors(BrandCyan)
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (inviteEmail.isNotBlank()) {
                                        viewModel.sendAppInvitation(inviteEmail)
                                        inviteEmail = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandCyan)
                            ) {
                                Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Email Invite")
                            }
                            
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:")
                                        putExtra("sms_body", "Hey! Join me on RoutineLog, the ultimate secure hybrid financial ledger. Download it now!")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald)
                            ) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SMS Invite")
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
            },
            monthlyPrice = priceMonthly,
            yearlyPrice = priceYearly,
            lifetimePrice = priceLifetime
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
                        text = "Version v1.5.0-PRO",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandGold,
                        modifier = Modifier
                            .background(BrandGold.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Text(
                        text = aboutUsText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    // Offline Sandbox Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Local Sandbox (Offline Mode)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandAmber
                            )
                        }
                        Text(
                            text = "Standard mode is 100% offline. All debt calculators, roadmaps, and nutritional logs are stored securely on-device with custom AES-256 database key encryption, requiring zero network permissions.",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Online Sync Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Firebase Sync (Online Mode)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandCyan
                            )
                        }
                        Text(
                            text = "Online mode enables real-time collaborative bill splitting among household members. Uses secure, isolated Firebase nodes to sync invited groups while private personal finances remain locked locally on-device.",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Dynamic System Status Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isConnected = cloudSyncStatus.contains("Connected")
                        val isEncrypted by viewModel.isDatabaseEncrypted.collectAsState()
                        
                        // Cloud Sync status chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isConnected) BrandEmerald.copy(alpha = 0.08f) else BrandAmber.copy(alpha = 0.08f))
                                .border(0.5.dp, if (isConnected) BrandEmerald.copy(alpha = 0.3f) else BrandAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Sync Status",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isConnected) "Online ☁️" else "Sandbox ⚡",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isConnected) BrandEmerald else BrandAmber
                                )
                            }
                        }

                        // Encryption Status chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isEncrypted) BrandEmerald.copy(alpha = 0.08f) else BrandAmber.copy(alpha = 0.08f))
                                .border(0.5.dp, if (isEncrypted) BrandEmerald.copy(alpha = 0.3f) else BrandAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "DB Security",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isEncrypted) "AES-256 🔒" else "Standard 🔓",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isEncrypted) BrandEmerald else BrandAmber
                                )
                            }
                        }
                    }

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
                    Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
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
                    text = "Privacy Shield & Guidelines",
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
                            .background(BrandEmerald.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, BrandEmerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Your data is entirely your property. We enforce zero tracking, zero advertising, and absolute ledger isolation.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandEmerald
                            )
                        }
                    }
                    
                    Text(
                        text = privacyPolicyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 260.dp)
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

    // Contact Us Support Dialog
    if (showContactDialog) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { 
                if (!isSubmittingContact) {
                    showContactDialog = false 
                    showContactSuccess = false
                    contactMessage = ""
                }
            },
            icon = {
                Icon(
                    imageVector = if (showContactSuccess) Icons.Default.CheckCircle else Icons.Default.Email, 
                    null, 
                    tint = if (showContactSuccess) BrandEmerald else BrandViolet, 
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                GradientText(
                    text = if (showContactSuccess) "Ticket Dispatched! ✉️" else "Contact Support",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                if (showContactSuccess) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Reference Ticket ID",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = generatedTicketId,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                            color = BrandGold,
                            modifier = Modifier
                                .background(BrandGold.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(0.5.dp, BrandGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        Text(
                            text = "We have securely received your request under the [$contactCategory] category from $contactEmail.\n\nOur specialized 24/7 Pro support team has been notified, and an agent will follow up with you within 12 to 24 hours. Keep this ticket ID for your records.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrandEmerald.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🔒 A copy of this secure receipt has been synced with your device notifications manager.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandEmerald,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Select inquiry category, write your secure ticket, and our support team will contact you shortly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Categories selection
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = listOf("Billing", "Bug Report", "Feature", "General")
                            categories.forEach { cat ->
                                val isSelected = contactCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BrandViolet.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            0.5.dp, 
                                            if (isSelected) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), 
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { if (!isSubmittingContact) contactCategory = cat }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) BrandViolet else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Registered Email Input
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Your Registered Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSubmittingContact,
                            colors = getDynamicTextFieldColors(BrandViolet)
                        )

                        // Multi-line inquiry message
                        OutlinedTextField(
                            value = contactMessage,
                            onValueChange = { contactMessage = it },
                            label = { Text("Message Details") },
                            placeholder = { Text("Describe your support inquiry or feedback in detail...") },
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            maxLines = 5,
                            singleLine = false,
                            enabled = !isSubmittingContact,
                            colors = getDynamicTextFieldColors(BrandViolet)
                        )

                        Text(
                            text = "🔒 Security Shield: Our support agents will NEVER ask for passwords or AES-256 decryption keys. Keep them completely private.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandRose
                        )
                    }
                }
            },
            confirmButton = {
                if (showContactSuccess) {
                    Button(
                        onClick = { 
                            showContactDialog = false 
                            showContactSuccess = false
                            contactMessage = ""
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                        modifier = Modifier.pressScale().fillMaxWidth()
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    val isValid = contactMessage.isNotBlank() && contactEmail.contains("@") && contactEmail.contains(".")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showContactDialog = false },
                            enabled = !isSubmittingContact,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (isValid && !isSubmittingContact) {
                                    scope.launch {
                                        isSubmittingContact = true
                                        delay(1800)
                                        generatedTicketId = "RTL-${(1000..9999).random()}-SEC"
                                        viewModel.submitInquiry(generatedTicketId, contactEmail, contactCategory, contactMessage)
                                        isSubmittingContact = false
                                        showContactSuccess = true
                                        viewModel.sendSupportTicketNotification(generatedTicketId, contactCategory)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            enabled = isValid && !isSubmittingContact,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                            modifier = Modifier.pressScale().weight(1f)
                        ) {
                            if (isSubmittingContact) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("Submit", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ⚙️ Admin Master Console Dialog
    if (showAdminConsoleDialog) {
        var adminTab by remember { mutableStateOf("Legals") }
        var editedPrivacyText by remember(privacyPolicyText) { mutableStateOf(privacyPolicyText) }
        var editedAboutText by remember(aboutUsText) { mutableStateOf(aboutUsText) }
        var editedPriceMonthly by remember(priceMonthly) { mutableStateOf(priceMonthly) }
        var editedPriceYearly by remember(priceYearly) { mutableStateOf(priceYearly) }
        var editedPaymentProvider by remember(paymentGatewayProvider) { mutableStateOf(paymentGatewayProvider) }
        var editedPaymentPublic by remember(paymentGatewayPublicKey) { mutableStateOf(paymentGatewayPublicKey) }
        var editedPaymentSecret by remember(paymentGatewaySecretKey) { mutableStateOf(paymentGatewaySecretKey) }
        var editedPaymentUpiId by remember(paymentGatewayUpiId) { mutableStateOf(paymentGatewayUpiId) }

        AlertDialog(
            onDismissRequest = { showAdminConsoleDialog = false },
            icon = {
                Icon(Icons.Default.Settings, null, tint = BrandViolet, modifier = Modifier.size(32.dp))
            },
            title = {
                GradientText(
                    text = "Admin Master Console",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    // Navigation Dashboard (2x2 Grid)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val tabs = listOf(
                            "Legals" to "📝 Legals", "Pricing" to "💸 Pricing",
                            "Gateway" to "💳 Gateway", "Tickets" to "🎟️ Tickets"
                        )
                        for (rowTabs in tabs.chunked(2)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                rowTabs.forEach { (tabId, label) ->
                                    val isSelected = adminTab == tabId
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) BrandViolet.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(width = 0.5.dp, color = if (isSelected) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                            .clickable { adminTab = tabId }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isSelected) BrandViolet else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Tab Contents
                    when (adminTab) {
                        "Legals" -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "About Us Workspace Info",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = editedAboutText,
                                    onValueChange = { editedAboutText = it },
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    maxLines = 4,
                                    singleLine = false,
                                    colors = getDynamicTextFieldColors(BrandCyan)
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "Privacy Policy Guidelines Text",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = editedPrivacyText,
                                    onValueChange = { editedPrivacyText = it },
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    maxLines = 8,
                                    singleLine = false,
                                    colors = getDynamicTextFieldColors(BrandEmerald)
                                )

                                Button(
                                    onClick = {
                                        viewModel.updatePrivacyPolicy(editedPrivacyText)
                                        viewModel.updateAboutUs(editedAboutText)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                                    modifier = Modifier.fillMaxWidth().pressScale().padding(top = 4.dp, bottom = 16.dp)
                                ) {
                                    Text("💾 Publish Content", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        "Pricing" -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Modify active pricing tiers for standard checkouts dynamically:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = editedPriceMonthly,
                                    onValueChange = { editedPriceMonthly = it },
                                    label = { Text("Monthly Plan (e.g. ₹199)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getDynamicTextFieldColors(BrandViolet)
                                )

                                OutlinedTextField(
                                    value = editedPriceYearly,
                                    onValueChange = { editedPriceYearly = it },
                                    label = { Text("Yearly Plan (e.g. ₹1199)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getDynamicTextFieldColors(BrandViolet)
                                )

                                Text(
                                    text = "💡 Sub-hint: Modified prices immediately override dynamic paywall selectors globally in real-time.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        viewModel.updatePricingPlans(editedPriceMonthly, editedPriceYearly, "")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                    modifier = Modifier.fillMaxWidth().pressScale()
                                ) {
                                    Text("💸 Publish New Pricing", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        "Tickets" -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (supportTickets.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(BrandEmerald.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .border(0.5.dp, BrandEmerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🎉 Zero active support tickets!\nOutstanding work.",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = BrandEmerald,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                                    ) {
                                        supportTickets.forEach { ticket ->
                                            val tId = ticket["ticketId"] as? String ?: ""
                                            val email = ticket["email"] as? String ?: ""
                                            val cat = ticket["category"] as? String ?: ""
                                            val msg = ticket["message"] as? String ?: ""
                                            val status = ticket["status"] as? String ?: "Received"
                                            val isResolved = status.equals("Resolved", ignoreCase = true)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (isResolved) BrandEmerald.copy(alpha = 0.05f) 
                                                        else BrandViolet.copy(alpha = 0.05f), 
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = if (isResolved) BrandEmerald.copy(alpha = 0.25f) 
                                                                else BrandViolet.copy(alpha = 0.25f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = tId,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                                            color = BrandGold
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            // Category Badge
                                                            Text(
                                                                text = cat,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = BrandViolet,
                                                                modifier = Modifier
                                                                    .background(BrandViolet.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                            // Status Badge
                                                            Text(
                                                                text = status,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = if (isResolved) BrandEmerald else BrandAmber,
                                                                modifier = Modifier
                                                                    .background(
                                                                        if (isResolved) BrandEmerald.copy(alpha = 0.1f) 
                                                                        else BrandAmber.copy(alpha = 0.1f), 
                                                                        RoundedCornerShape(4.dp)
                                                                    )
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "From: $email",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = msg,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (!isResolved) {
                                                        Button(
                                                            onClick = { viewModel.resolveTicket(tId) },
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                                                            modifier = Modifier.align(Alignment.End).height(28.dp).pressScale(),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("✓ Resolve Ticket", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Gateway" -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Payment Gateway Config",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = editedPaymentProvider,
                                    onValueChange = { editedPaymentProvider = it },
                                    label = { Text("Provider (e.g. Stripe, Razorpay)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getDynamicTextFieldColors(BrandEmerald)
                                )
                                OutlinedTextField(
                                    value = editedPaymentPublic,
                                    onValueChange = { editedPaymentPublic = it },
                                    label = { Text("Public / Client Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getDynamicTextFieldColors(BrandCyan)
                                )
                                OutlinedTextField(
                                    value = editedPaymentSecret,
                                    onValueChange = { editedPaymentSecret = it },
                                    label = { Text("Secret Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getDynamicTextFieldColors(BrandViolet)
                                )
                                OutlinedTextField(
                                    value = editedPaymentUpiId,
                                    onValueChange = { editedPaymentUpiId = it },
                                    label = { Text("UPI ID (e.g. user@ybl)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = getDynamicTextFieldColors(BrandRose)
                                )
                                
                                Button(
                                    onClick = {
                                        viewModel.updatePaymentGateway(editedPaymentProvider, editedPaymentPublic, editedPaymentSecret, editedPaymentUpiId)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                                    modifier = Modifier.fillMaxWidth().pressScale()
                                ) {
                                    Text("💳 Save Gateway Config", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAdminConsoleDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                    modifier = Modifier.pressScale().fillMaxWidth()
                ) {
                    Text("Close Panel", fontWeight = FontWeight.Bold, color = Color.White)
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
