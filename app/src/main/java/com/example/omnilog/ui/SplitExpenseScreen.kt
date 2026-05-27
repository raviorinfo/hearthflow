package com.example.omnilog.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnilog.data.model.SplitExpenseEntry
import com.example.omnilog.ui.theme.*
import com.example.omnilog.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseScreen(viewModel: MainViewModel) {
    val splitExpenses by viewModel.splitExpenses.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val isPremiumActive by viewModel.isPremiumActive.collectAsState()
    val splitGroups by viewModel.splitGroups.collectAsState()
    val groupInvitedMembers by viewModel.groupInvitedMembers.collectAsState()

    val priceMonthly by viewModel.priceMonthlyPlan.collectAsState()
    val priceYearly by viewModel.priceYearlyPlan.collectAsState()
    val priceLifetime by viewModel.priceLifetimePlan.collectAsState()

    var showAddSplitDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showInvitationSuccessDialog by remember { mutableStateOf(false) }
    var showManageGroupDialog by remember { mutableStateOf(false) }
    var lastCreatedGroupName by remember { mutableStateOf("") }
    var lastInvitedContacts by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("Active") } // "Active" or "Settled History"
    var selectedGroup by remember { mutableStateOf("All") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    val totalActiveExpensesCount = remember(splitExpenses) {
        splitExpenses.count { !it.isSettled }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

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

    val activeExpenses = remember(splitExpenses, selectedGroup) {
        splitExpenses.filter { !it.isSettled && (selectedGroup == "All" || it.groupName == selectedGroup) }
    }
    val settledExpenses = remember(splitExpenses, selectedGroup) {
        splitExpenses.filter { it.isSettled && (selectedGroup == "All" || it.groupName == selectedGroup) }
    }

    val totalOwedToYou = activeExpenses.filter { it.paidBy.equals("You", ignoreCase = true) }.sumOf { it.splitShare }
    val totalYouOwe = activeExpenses.filter { !it.paidBy.equals("You", ignoreCase = true) }.sumOf { it.splitShare }
    val netBalance = totalOwedToYou - totalYouOwe

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Title & Add Trigger
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GradientText(
                            text = "Split Ledger",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Manage shared family expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Button(
                        onClick = {
                            if (!isPremiumActive && totalActiveExpensesCount >= 2) {
                                showPremiumDialog = true
                            } else {
                                showAddSplitDialog = true
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                        modifier = Modifier.pressScale()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Split", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 👑 Premium Expiry Countdown Banner (dark-mode safe)
            if (isPremiumActive) {
                item {
                    userAccount?.let { account ->
                        val daysLeft = ((account.proExpiryTimestamp - System.currentTimeMillis()) / (1000L * 60L * 60L * 24L)).coerceAtLeast(0L)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1E1A08), Color(0xFF2A2210))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFFFD700).copy(alpha = 0.8f), BrandAmber.copy(alpha = 0.5f), Color(0xFFFFD700).copy(alpha = 0.8f))
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👑", fontSize = 18.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = account.subscriptionPlan?.ifBlank { "Premium Active" } ?: "Premium Active",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFFD700)
                                        )
                                        Text(
                                            text = "$daysLeft days remaining · Full access unlocked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFD700).copy(alpha = 0.65f)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                        .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "✓ Pro",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color(0xFFFFD700)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Premium Upgrade Banner
                item {
                    val premiumBannerBg = Brush.horizontalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF0D1E3D))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(premiumBannerBg)
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(listOf(BrandViolet, BrandCyan)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showPremiumDialog = true }
                            .pressScale()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("👑", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Upgrade to RoutineLog Premium",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Unlock unlimited split groups, active bills, & debt payoff features.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandCyan)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Upgrade",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // Group Selection Pills Row
            item {
                val unselectedPillBg = if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                val unselectedPillBorder = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "All" Pill
                    item {
                        val isSelected = selectedGroup == "All"
                        val selectionBg = Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .then(if (isSelected) Modifier.background(selectionBg) else Modifier.background(unselectedPillBg))
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else unselectedPillBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedGroup = "All" }
                                .pressScale()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "All",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Other Groups
                    items(splitGroups) { group ->
                        val isSelected = selectedGroup == group
                        val selectionBg = Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .then(if (isSelected) Modifier.background(selectionBg) else Modifier.background(unselectedPillBg))
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else unselectedPillBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedGroup = group }
                                .pressScale()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // "⚙️ Manage" Pill (Visible when a custom group is selected)
                    if (selectedGroup != "All") {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(BrandCyan.copy(alpha = 0.15f))
                                    .border(
                                        1.dp,
                                        BrandCyan.copy(alpha = 0.5f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { showManageGroupDialog = true }
                                    .pressScale()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Settings, null, tint = BrandCyan, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Manage",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = BrandCyan
                                    )
                                }
                            }
                        }
                    }
                    
                    // "+ Group" Pill
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BrandViolet.copy(alpha = 0.2f))
                                .border(
                                    1.dp,
                                    BrandViolet.copy(alpha = 0.5f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (!isPremiumActive && splitGroups.size >= 4) {
                                        showPremiumDialog = true
                                    } else {
                                        showCreateGroupDialog = true
                                    }
                                }
                                .pressScale()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, tint = BrandViolet, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Group",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BrandViolet
                                )
                            }
                        }
                    }
                }
            }

            // Net Balance Dashboard
            item {
                val netGradient = if (netBalance >= 0) {
                    Brush.linearGradient(listOf(BrandCyan.copy(alpha = 0.25f), BrandIndigo.copy(alpha = 0.05f)))
                } else {
                    Brush.linearGradient(listOf(BrandRose.copy(alpha = 0.25f), BrandIndigo.copy(alpha = 0.05f)))
                }
                val netBorder = if (netBalance >= 0) BrandCyan.copy(alpha = 0.4f) else BrandRose.copy(alpha = 0.4f)

                // Animated net balance counter
                val animatedNet by animateFloatAsState(
                    targetValue = netBalance.toFloat(),
                    animationSpec = tween(800, easing = EaseOutCubic),
                    label = "netAnim"
                )
                val dashboardCardBg = if (isDark) Color(0x11FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                val dividerColor = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, netBorder, RoundedCornerShape(20.dp)),
                    containerColor = dashboardCardBg
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Net Shared Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (animatedNet >= 0) "+${formatCurrency(animatedNet.toDouble())}" else "-${formatCurrency(Math.abs(animatedNet.toDouble()))}",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (netBalance >= 0) BrandCyan else BrandRose
                                )
                            }
                            // Status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (netBalance >= 0) BrandCyan.copy(alpha = 0.15f)
                                        else BrandRose.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        1.dp,
                                        if (netBalance >= 0) BrandCyan.copy(alpha = 0.4f) else BrandRose.copy(alpha = 0.4f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = when {
                                        activeExpenses.isEmpty() -> "✓ Settled"
                                        netBalance > 0 -> "↑ In Profit"
                                        else -> "↓ You Owe"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (netBalance >= 0) BrandCyan else BrandRose
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Animated balance ratio bar
                        if (totalOwedToYou + totalYouOwe > 0) {
                            val owedRatio = (totalOwedToYou / (totalOwedToYou + totalYouOwe)).toFloat().coerceIn(0f, 1f)
                            val animatedRatio by animateFloatAsState(
                                targetValue = owedRatio,
                                animationSpec = tween(900, easing = EaseOutCubic),
                                label = "ratioAnim"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Owed to You ${(owedRatio * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandCyan.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        "You Owe ${((1f - owedRatio) * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandRose.copy(alpha = 0.9f)
                                    )
                                }
                                // Split bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(BrandRose.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedRatio)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(BrandCyan, BrandCyan.copy(alpha = 0.7f))
                                                )
                                            )
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        HorizontalDivider(color = dividerColor)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Owed to You", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalOwedToYou), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandCyan)
                            }
                            // Vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(dividerColor)
                                    .align(Alignment.CenterVertically)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text("You Owe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalYouOwe), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandRose)
                            }
                        }

                        // Free tier usage meter (only for non-premium)
                        if (!isPremiumActive) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.07f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(Modifier.height(10.dp))
                            val billsUsed = totalActiveExpensesCount.coerceAtMost(2)
                            val groupsUsed = (splitGroups.size - 3).coerceAtLeast(0).coerceAtMost(1)
                            val usageRatio = ((billsUsed / 2f + groupsUsed / 1f) / 2f).coerceIn(0f, 1f)
                            val animatedUsage by animateFloatAsState(
                                targetValue = usageRatio,
                                animationSpec = tween(700),
                                label = "usageAnim"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Free Tier Usage",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (usageRatio >= 1f) BrandAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$billsUsed/2 bills · $groupsUsed/1 group",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedUsage)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        when {
                                                            usageRatio >= 1f -> listOf(BrandAmber, BrandRose)
                                                            usageRatio >= 0.6f -> listOf(BrandViolet, BrandAmber)
                                                            else -> listOf(BrandViolet, BrandCyan)
                                                        }
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                            if (usageRatio >= 1f) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "⚠ Free tier limit reached. Upgrade to add more splits.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandAmber
                                )
                            }
                        }
                    }
                }
            }

            // Tabs for Active vs Settled
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf("Active", "Settled History")
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val selectionBg = Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(if (isSelected) Modifier.background(selectionBg) else Modifier)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTab = tab }
                                    .pressScale()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tab == "Active") "Active Splits (${activeExpenses.size})" else "Settled (${settledExpenses.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Items List
            val displayedExpenses = if (selectedTab == "Active") activeExpenses else settledExpenses

            if (displayedExpenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == "Active") Icons.Default.CheckCircle else Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = BrandCyan
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTab == "Active") "No Active Splits Owed" else "No Settled Bills Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (selectedTab == "Active") "All shared expenses are perfectly settled up! Tap 'Add Split' to log a new bill." else "Fully settled joint payments will be listed here chronologically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(displayedExpenses) { expense ->
                    SplitItemCard(
                        expense = expense,
                        onSettle = { viewModel.settleSplitExpense(expense.id) },
                        onDelete = { viewModel.deleteSplitExpense(expense.id) }
                    )
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateGroupDialog) {
        var groupNameInput by remember { mutableStateOf("") }
        var contactInput by remember { mutableStateOf("") }
        var selectedContactType by remember { mutableStateOf("Email") } // "Email" or "Phone Number"
        val invitedContacts = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = {
                GradientText(
                    text = "Create Split Group",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    // Group Name Input
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name (e.g. Goa Trip, Flatmates)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDynamicTextFieldColors(BrandViolet),
                        singleLine = true
                    )

                    // Contact Invite Section Header
                    Text(
                        text = "Invite Contacts (Optional):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Contact Type Toggle Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf("Email", "Phone Number")
                        types.forEach { type ->
                            val isSel = selectedContactType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) BrandViolet.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (isSel) BrandViolet else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { selectedContactType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Contact Add Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = contactInput,
                            onValueChange = { contactInput = it },
                            label = { Text(if (selectedContactType == "Email") "Contact Email" else "Contact Phone Number") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (selectedContactType == "Email") KeyboardType.Email else KeyboardType.Phone
                            ),
                            colors = getDynamicTextFieldColors(BrandCyan),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                val cleanVal = contactInput.trim()
                                if (cleanVal.isNotBlank()) {
                                    val isValid = if (selectedContactType == "Email") {
                                        cleanVal.contains("@") && cleanVal.contains(".")
                                    } else {
                                        cleanVal.all { it.isDigit() || it == '+' || it == '-' } && cleanVal.length >= 7
                                    }
                                    if (isValid) {
                                        if (!invitedContacts.contains(cleanVal)) {
                                            invitedContacts.add(cleanVal)
                                        }
                                        contactInput = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandCyan)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }

                    // Invited Contacts Pill Row
                    if (invitedContacts.isNotEmpty()) {
                        Text(
                            text = "Invited Contacts List:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.outline
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(invitedContacts) { contact ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(BrandCyan.copy(alpha = 0.15f))
                                        .border(1.dp, BrandCyan, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(contact, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = BrandRose,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { invitedContacts.remove(contact) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Information Card for Join Links
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandViolet.copy(alpha = 0.1f))
                            .border(1.dp, BrandViolet.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ℹ️ Universal Invitation Link",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandViolet
                            )
                            Text(
                                text = "Invited contacts will receive a unique link. Clicking it automatically redirects them to download RoutineLog and join this group instantly.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupNameInput.isNotBlank()) {
                            val trimmedName = groupNameInput.trim()
                            viewModel.createSplitGroup(trimmedName, invitedContacts.toList())
                            selectedGroup = trimmedName
                            showCreateGroupDialog = false
                            
                            // Save invite context to show success dialog
                            lastCreatedGroupName = trimmedName
                            lastInvitedContacts = invitedContacts.toList()
                            showInvitationSuccessDialog = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.pressScale()
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
        )
    }

    // Invitation Success Dialog
    if (showInvitationSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showInvitationSuccessDialog = false 
                showAddSplitDialog = true
            },
            title = {
                GradientText(
                    text = "🎉 Invitations Ready!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Group '$lastCreatedGroupName' has been created successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "We have prepared invitation links for your contacts. If they don't have the RoutineLog app installed, they can tap the link to download the app and join the group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (lastInvitedContacts.isNotEmpty()) {
                        Text(
                            text = "Dispatch List (${lastInvitedContacts.size}):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandCyan
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .padding(10.dp)
                        ) {
                            lastInvitedContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = contact,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BrandCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SMS/Email Sent",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = BrandCyan
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Shareable Link preview card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1C2438) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Universal Join Link:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandViolet
                        )
                        Text(
                            text = "routinelog://join-group?groupId=${lastCreatedGroupName.replace(" ", "%20")}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp),
                            color = BrandViolet
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInvitationSuccessDialog = false
                        showAddSplitDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                    modifier = Modifier.pressScale()
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
        )
    }

    // Add Dialog
    if (showAddSplitDialog) {
        AddSplitDialog(
            familyMembers = familyMembers,
            splitGroups = splitGroups,
            groupInvitedMembers = groupInvitedMembers,
            defaultGroup = if (selectedGroup == "All") "" else selectedGroup,
            onDismiss = { showAddSplitDialog = false },
            onSave = { title, total, paidBy, splitWith, share, group ->
                viewModel.addSplitExpense(title, total, paidBy, splitWith, share, group)
                showAddSplitDialog = false
            }
        )
    }

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

    if (showManageGroupDialog && selectedGroup != "All") {
        ManageGroupDialog(
            groupName = selectedGroup,
            members = groupInvitedMembers[selectedGroup] ?: emptyList(),
            onDismiss = { showManageGroupDialog = false },
            onUpdateName = { oldName, newName ->
                viewModel.updateSplitGroup(oldName, newName)
                selectedGroup = newName
                showManageGroupDialog = false
            },
            onAddMember = { name, email ->
                viewModel.addMemberToGroup(name, email)
            },
            onRemoveMember = { name, email ->
                viewModel.removeMemberFromGroup(name, email)
            },
            onDeleteGroup = { name ->
                viewModel.deleteSplitGroup(name)
                selectedGroup = "All"
                showManageGroupDialog = false
            }
        )
    }
}

@Composable
fun ManageGroupDialog(
    groupName: String,
    members: List<String>,
    onDismiss: () -> Unit,
    onUpdateName: (oldName: String, newName: String) -> Unit,
    onAddMember: (groupName: String, email: String) -> Unit,
    onRemoveMember: (groupName: String, email: String) -> Unit,
    onDeleteGroup: (groupName: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    var editedName by remember { mutableStateOf(groupName) }
    var newMemberEmail by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            GradientText(
                text = "Manage Group: $groupName",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showDeleteConfirm) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandRose.copy(alpha = 0.1f))
                            .border(1.dp, BrandRose.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "⚠️ Are you absolutely sure?",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = BrandRose
                            )
                            Text(
                                text = "Deleting '$groupName' will permanently remove the group and delete all associated splits and bills in this ledger from both local storage and online. This cannot be undone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onDeleteGroup(groupName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Yes, Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showDeleteConfirm = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Group Display Name",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandCyan
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.weight(1f),
                                colors = getDynamicTextFieldColors(BrandCyan),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (editedName.isNotBlank() && editedName.trim() != groupName) {
                                        onUpdateName(groupName, editedName.trim())
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                                modifier = Modifier.height(56.dp),
                                enabled = editedName.isNotBlank() && editedName.trim() != groupName
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Group Members (${members.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandViolet
                        )

                        if (members.isEmpty()) {
                            Text(
                                text = "No other members in this group yet. Add friends below to start splitting!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                members.forEach { email ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text("👤", fontSize = 16.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = email,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveMember(groupName, email) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove member",
                                                tint = BrandRose,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newMemberEmail,
                                onValueChange = { newMemberEmail = it },
                                label = { Text("Invite contact email/phone...") },
                                modifier = Modifier.weight(1f),
                                colors = getDynamicTextFieldColors(BrandViolet),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (newMemberEmail.isNotBlank()) {
                                        onAddMember(groupName, newMemberEmail.trim())
                                        newMemberEmail = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                modifier = Modifier.height(56.dp),
                                enabled = newMemberEmail.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Danger Zone",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandRose
                        )
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, BrandRose.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, null, tint = BrandRose, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete Group Permanently", color = BrandRose, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showDeleteConfirm) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
    )
}

@Composable
fun SplitItemCard(
    expense: SplitExpenseEntry,
    onSettle: () -> Unit,
    onDelete: () -> Unit
) {
    val isOwedToUser = expense.paidBy.equals("You", ignoreCase = true)
    val accentColor = if (isOwedToUser) BrandCyan else BrandRose
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0x15FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderBrush = Brush.linearGradient(
        listOf(accentColor.copy(alpha = 0.5f), if (isDark) Color(0x10FFFFFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), BrandGradientEnd.copy(alpha = 0.3f))
    )

    // Share percentage for the progress bar
    val sharePercent = if (expense.totalAmount > 0)
        (expense.splitShare / expense.totalAmount).toFloat().coerceIn(0f, 1f)
    else 0f

    val animatedShare by animateFloatAsState(
        targetValue = sharePercent,
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "shareAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Row: Title, Group Badge, Settled Badge, and Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandViolet.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = expense.groupName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandViolet
                        )
                    }

                    if (expense.isSettled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandEmerald.copy(alpha = 0.15f))
                                .border(0.5.dp, BrandEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✓ Settled",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandEmerald
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete split",
                        tint = BrandRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Middle Section: Metadata Details
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Total Bill: ${formatCurrency(expense.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Paid By: ${expense.paidBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Balance owes description with arrow indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isOwedToUser) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isOwedToUser) {
                            "${expense.splitWith} owes you ${formatCurrency(expense.splitShare)}"
                        } else {
                            "You owe ${expense.paidBy} ${formatCurrency(expense.splitShare)}"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accentColor
                    )
                }
            }

            // Share fraction progress bar
            if (expense.totalAmount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your share",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(sharePercent * 100).toInt()}% · ${formatCurrency(expense.splitShare)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedShare)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accentColor, accentColor.copy(alpha = 0.5f))
                                    )
                                )
                        )
                    }
                }
            }

            // Settle Up Action Button (Full width at bottom for easy tapping and layout space)
            if (!expense.isSettled) {
                Button(
                    onClick = onSettle,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .pressScale()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isOwedToUser) Color.Black else Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Settle Up",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isOwedToUser) Color.Black else Color.White
                    )
                }
            }
        }
    }
}


@Composable
fun AddSplitDialog(
    familyMembers: List<String>,
    splitGroups: List<String>,
    groupInvitedMembers: Map<String, List<String>> = emptyMap(),
    defaultGroup: String = "",
    onDismiss: () -> Unit,
    onSave: (title: String, total: Double, paidBy: String, splitWith: String, share: Double, groupName: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

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

    var title by remember { mutableStateOf("") }
    var totalAmountStr by remember { mutableStateOf("") }
    
    // Dropdown state for Paid By
    var paidBy by remember { mutableStateOf("You") }
    var paidByExpanded by remember { mutableStateOf(false) }

    // Dropdown state for Group
    var selectedGroupInput by remember { mutableStateOf(defaultGroup) }
    var groupExpanded by remember { mutableStateOf(false) }

    // Checklist members states
    val allPossibleMembers = remember(familyMembers, selectedGroupInput, groupInvitedMembers) {
        val list = mutableStateListOf<String>()
        list.add("You")
        list.addAll(familyMembers)
        // Dynamically add invited contacts for this group
        groupInvitedMembers[selectedGroupInput]?.let { invites ->
            list.addAll(invites)
        }
        list
    }

    val selectedMembers = remember { mutableStateListOf<String>() }

    // Auto-sync selected checklist on change of available group members
    LaunchedEffect(allPossibleMembers.toList()) {
        selectedMembers.clear()
        selectedMembers.addAll(allPossibleMembers)
    }

    var customMemberInput by remember { mutableStateOf("") }

    // Dynamic equal division calculations
    val containsPayer = selectedMembers.contains(paidBy)
    val totalPeople = if (containsPayer) selectedMembers.size else (selectedMembers.size + 1)
    val calculatedShare: Double = remember(totalAmountStr, totalPeople) {
        val total = totalAmountStr.toDoubleOrNull() ?: 0.0
        if (totalPeople > 0) total / totalPeople else 0.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            GradientText(
                text = "New Split Bill",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Dinner, Rent)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getDynamicTextFieldColors(BrandViolet),
                    singleLine = true
                )

                // Total Amount
                OutlinedTextField(
                    value = totalAmountStr,
                    onValueChange = { totalAmountStr = it },
                    label = { Text("Total Bill Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = getDynamicTextFieldColors(BrandCyan),
                    singleLine = true
                )

                // Dropdown selector for Paid By
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid By") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(if (paidByExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                        },
                        colors = getDynamicTextFieldColors(BrandViolet)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { paidByExpanded = !paidByExpanded }
                    )
                    DropdownMenu(
                        expanded = paidByExpanded,
                        onDismissRequest = { paidByExpanded = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        val options = listOf("You") + familyMembers + (groupInvitedMembers[selectedGroupInput] ?: emptyList())
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    paidBy = option
                                    paidByExpanded = false
                                }
                            )
                        }
                    }
                }

                // Dropdown selector for Group category
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedGroupInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Group Category") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(if (groupExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                        },
                        colors = getDynamicTextFieldColors(BrandGold)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { groupExpanded = !groupExpanded }
                    )
                    DropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        splitGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedGroupInput = group
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                // Checklist Section Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split Equally With:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            if (selectedMembers.size == allPossibleMembers.size) {
                                selectedMembers.clear()
                            } else {
                                selectedMembers.clear()
                                selectedMembers.addAll(allPossibleMembers)
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedMembers.size == allPossibleMembers.size) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandCyan
                        )
                    }
                }

                // Horizontal scrollable pills list
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(allPossibleMembers) { member ->
                        val isSelected = selectedMembers.contains(member)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) BrandCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    1.dp,
                                    if (isSelected) BrandCyan else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedMembers.remove(member)
                                    } else {
                                        selectedMembers.add(member)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = BrandCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = member,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Add Custom Member Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customMemberInput,
                        onValueChange = { customMemberInput = it },
                        label = { Text("Add custom member...") },
                        modifier = Modifier.weight(1f),
                        colors = getDynamicTextFieldColors(BrandCyan),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (customMemberInput.isNotBlank()) {
                                val name = customMemberInput.trim()
                                if (!allPossibleMembers.contains(name)) {
                                    allPossibleMembers.add(name)
                                }
                                if (!selectedMembers.contains(name)) {
                                    selectedMembers.add(name)
                                }
                                customMemberInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }

                // Dynamic Split Banner
                if (totalPeople > 0 && calculatedShare > 0.0) {
                    val membersToDisplay = if (containsPayer) selectedMembers.toList() else (listOf(paidBy) + selectedMembers)
                    val isFourWaySplit = totalPeople == 4 && paidBy.equals("You", ignoreCase = true)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isFourWaySplit) BrandViolet.copy(alpha = 0.15f) else BrandCyan.copy(alpha = 0.1f))
                            .border(1.dp, if (isFourWaySplit) BrandViolet.copy(alpha = 0.5f) else BrandCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (isFourWaySplit) {
                            Text(
                                text = "🔥 Premium 4-Way Equal Split Active:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandViolet
                            )
                            Spacer(Modifier.height(4.dp))
                            val otherMembers = selectedMembers.filter { it != paidBy }.joinToString(", ")
                            Text(
                                text = "Split between 4 people including paid user (You). 3 members added to ledger ($otherMembers), paid by You. The full amount is split in 4.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "Split Breakdown:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandCyan
                            )
                            Spacer(Modifier.height(4.dp))
                            val membersListText = membersToDisplay.joinToString(", ")
                            Text(
                                text = "Splitting ${formatCurrency(totalAmountStr.toDoubleOrNull() ?: 0.0)} equally among $totalPeople members: $membersListText.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Each person's share: ${formatCurrency(calculatedShare)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isFourWaySplit) BrandViolet else BrandCyan
                        )
                    }
                }
            }
        },
        confirmButton = {
            val totalVal = totalAmountStr.toDoubleOrNull() ?: 0.0
            val isButtonEnabled = title.isNotBlank() && totalVal > 0.0 && selectedMembers.any { it != paidBy }
            
            Button(
                onClick = {
                    if (isButtonEnabled) {
                        val isFourWaySplit = totalPeople == 4 && paidBy.equals("You", ignoreCase = true)
                        
                        selectedMembers.filter { it != paidBy }.forEach { member ->
                            onSave(title, totalVal, paidBy, member, calculatedShare, selectedGroupInput)
                        }
                        
                        if (isFourWaySplit) {
                            android.widget.Toast.makeText(
                                context,
                                "Split among 4 people logged: 3 members added, amount split in 4.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Split logged successfully!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = isButtonEnabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.pressScale()
            ) {
                Text("Log Split", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
    )
}

