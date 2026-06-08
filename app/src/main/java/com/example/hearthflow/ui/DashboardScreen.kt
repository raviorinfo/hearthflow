package com.example.hearthflow.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.example.hearthflow.data.model.LogCategory
import com.example.hearthflow.viewmodel.MainViewModel
import com.example.hearthflow.ui.theme.BrandEmerald
import com.example.hearthflow.ui.theme.BrandGold
import com.example.hearthflow.ui.theme.BrandGradientEnd
import com.example.hearthflow.ui.theme.BrandGradientMid
import com.example.hearthflow.ui.theme.BrandGradientStart
import com.example.hearthflow.ui.theme.BrandViolet
import com.example.hearthflow.ui.theme.BrandCyan
import com.example.hearthflow.ui.theme.BrandRose
import com.example.hearthflow.ui.theme.BrandAmber
import com.example.hearthflow.ui.theme.BrandIndigo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import java.util.Calendar


@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val allRawLogs by viewModel.allLogs.collectAsState()
    val logs = remember(allRawLogs) {
        allRawLogs.filter { !it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") }
    }
    val inventory by viewModel.inventory.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()
    val isPremiumActive by viewModel.isPremiumActive.collectAsState()
    var showPremiumDialog by remember { mutableStateOf(false) }

    val lowStockAlerts = inventory.filter { it.daysRemaining > 0 && it.daysRemaining <= 3 }
    val recentLogs = logs.take(5)
    val totalExpenses = logs.filter { it.category == LogCategory.EXPENSE }
        .sumOf { log ->
            "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble() ?: 0.0
        }

    val categoryDistribution = logs.groupBy { it.category }
        .mapValues { it.value.size.toFloat() }
        .mapKeys { it.key.name }

    // Weekly streak calculation
    val weeklyStreak = remember(logs) {
        var streak = 0
        for (dayOffset in 0..6) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dayOffset) }
            val hasLog = logs.any { log ->
                val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                logCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR) &&
                logCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
            }
            if (hasLog) streak++ else if (dayOffset > 0) break
        }
        streak
    }

    val logsThisWeek = remember(logs) {
        val weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        logs.count { it.timestamp >= weekAgo }
    }

    val categoryColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )

    // Time-of-day greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11  -> "Good morning ☀️"
        in 12..16 -> "Good afternoon 🌤️"
        in 17..20 -> "Good evening 🌇"
        else      -> "Good night 🌙"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // ── Hero Banner ──────────────────────────────────────────────────────
        item {
            val isDark = isSystemInDarkTheme()
            val glassColor = if (isDark) Color(0x1F111827) else Color(0xDDFFFFFF)
            val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
            val glowIntensity by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowIntensity"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassColor)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                BrandViolet.copy(alpha = glowIntensity),
                                BrandCyan.copy(alpha = glowIntensity * 0.5f),
                                BrandGold.copy(alpha = glowIntensity)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                // Background glowing canvas orbs
                Canvas(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(24.dp))) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(BrandViolet.copy(alpha = 0.22f), Color.Transparent)),
                        radius = size.width * 0.45f,
                        center = Offset(x = size.width * 0.9f, y = size.height * 0.1f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(BrandCyan.copy(alpha = 0.18f), Color.Transparent)),
                        radius = size.width * 0.4f,
                        center = Offset(x = size.width * 0.1f, y = size.height * 0.8f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    
                    val nameText = userAccount?.name?.ifBlank { "Your Dashboard" } ?: "Your Dashboard"
                    GradientText(
                        text = nameText,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonBadge("${logs.size} Logs", BrandViolet)
                        NeonBadge("${inventory.size} Pantry Items", BrandCyan)
                        if (isPremiumActive) {
                            NeonBadge("Pro Tier 👑", BrandGold)
                        }
                    }
                }
            }
        }

        // ── Animated Stat Cards ──────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total Expenses",
                    value = formatCurrency(totalExpenses),
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = BrandCyan,
                    modifier = Modifier.weight(1f).pressScale()
                )
                StatCard(
                    label = "Pantry Items",
                    value = "${inventory.size}",
                    icon = Icons.Default.Kitchen,
                    accentColor = BrandViolet,
                    modifier = Modifier.weight(1f).pressScale()
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Logs Today",
                    value = "${logs.count { 
                        val cal = Calendar.getInstance()
                        val logCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        cal.get(Calendar.DAY_OF_YEAR) == logCal.get(Calendar.DAY_OF_YEAR)
                    }}",
                    icon = Icons.Default.Today,
                    accentColor = BrandEmerald,
                    modifier = Modifier.weight(1f).pressScale()
                )
                StatCard(
                    label = "Low Stock",
                    value = "${lowStockAlerts.size}",
                    icon = Icons.Default.Warning,
                    accentColor = if (lowStockAlerts.isEmpty()) BrandEmerald else BrandAmber,
                    modifier = Modifier.weight(1f).pressScale()
                )
            }
        }

        // ── Weekly Streak Banner ─────────────────────────────────────────────
        item {
            val isDark = isSystemInDarkTheme()
            val cardBg = if (isDark) Color(0x1FFFFFFF) else Color(0xF2F8FAFF)
            
            // Streak pulse glow
            val infiniteTransition = rememberInfiniteTransition(label = "streakPulse")
            val borderGlowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "streakGlow"
            )
            
            val accentColor = when {
                weeklyStreak >= 7 -> BrandGold
                weeklyStreak >= 4 -> BrandViolet
                weeklyStreak >= 1 -> BrandCyan
                else -> Color.Gray
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = borderGlowAlpha),
                                Color.Transparent,
                                BrandGradientEnd.copy(alpha = borderGlowAlpha * 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .pressScale()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Glowing fire/energy avatar circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    weeklyStreak >= 7 -> "🔥"
                                    weeklyStreak >= 3 -> "⚡"
                                    weeklyStreak >= 1 -> "✅"
                                    else -> "💤"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            val titleColor = if (isDark) Color.White else Color.Black
                            val subColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
                            Text(
                                text = if (weeklyStreak == 0) "Start Your Streak!" else "$weeklyStreak-Day Streak",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = titleColor
                            )
                            Text(
                                text = "$logsThisWeek logs this week · Keep it going!",
                                style = MaterialTheme.typography.labelSmall,
                                color = subColor
                            )
                        }
                    }
                    // Day dots with glowing active states
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 6 downTo 0) {
                            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                            val hasMark = logs.any { log ->
                                val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                                logCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR) &&
                                logCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                            }
                            
                            val dotColor = if (hasMark) accentColor else Color.White.copy(alpha = 0.25f)
                            val dotBorder = if (hasMark) accentColor.copy(alpha = 0.6f) else Color.Transparent
                            
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .then(
                                        if (hasMark) Modifier.border(1.5.dp, dotBorder, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }

        // ── Premium CTA for non-premium users ──────────────────────────────
        if (!isPremiumActive) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF13131A), Color(0xFF1C1E3A))
                            )
                        )
                        .border(
                            1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(BrandViolet.copy(alpha = 0.4f), BrandCyan.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.width(6.dp))
                                GradientText(
                                    text = "Unlock Premium",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Unlimited splits, groups, debts & investments.",
                                style = MaterialTheme.typography.labelSmall,
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
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Upgrade", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // ── Activity Mix ──────────────────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Activity Mix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (categoryDistribution.isEmpty()) {
                            Text(
                                "No activity yet. Start logging!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            categoryDistribution.entries.forEachIndexed { idx, (cat, count) ->
                                val catColor = categoryColors[idx % categoryColors.size]
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(catColor)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "$cat  ·  ${count.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    PieChart(
                        data = if (categoryDistribution.isEmpty()) mapOf("Empty" to 1f) else categoryDistribution,
                        colors = if (categoryDistribution.isEmpty()) listOf(MaterialTheme.colorScheme.surfaceVariant) else categoryColors,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }
        }

        // ── Smart Insights ───────────────────────────────────────────────────
        item {
            val allOk = lowStockAlerts.isEmpty()
            val pulseAnim = rememberInfiniteTransition(label = "pulse")
            val dotAlpha by pulseAnim.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    androidx.compose.animation.core.tween(900), RepeatMode.Reverse
                ),
                label = "dotAlpha"
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (allOk)
                    BrandEmerald.copy(alpha = 0.08f)
                else
                    Color(0xFFF59E0B).copy(alpha = 0.08f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                (if (allOk) BrandEmerald else Color(0xFFF59E0B)).copy(alpha = dotAlpha)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (allOk) "All systems normal. Your pantry is well-stocked."
                            else "Action required: ${lowStockAlerts.size} items running low.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = if (allOk) BrandEmerald else Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Low Stock Alerts (horizontal chips) ───────────────────────────────
        if (lowStockAlerts.isNotEmpty()) {
            item {
                GlassCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Predictive Alerts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lowStockAlerts) { alert ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Inventory, null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "${alert.name} · ${alert.daysRemaining}d",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Recent Activity ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (logs.size > 5) {
                    TextButton(onClick = {}) {
                        Text("See all →", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inbox, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        Text("No logs yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        Text("Type or speak your first log below!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        items(recentLogs) { log ->
            LogItem(log)
        }
    }

    // Premium subscription dialog (triggered from dashboard CTA)
    if (showPremiumDialog) {
        PremiumSubscriptionDialog(
            onDismiss = { showPremiumDialog = false },
            onPurchase = { plan, duration ->
                viewModel.purchasePremiumPlan(plan, duration)
                showPremiumDialog = false
            }
        )
    }
}
