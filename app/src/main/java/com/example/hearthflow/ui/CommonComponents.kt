package com.example.hearthflow.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hearthflow.R
import com.example.hearthflow.ui.theme.*
import kotlin.math.roundToInt

// ─── Currency Helpers ─────────────────────────────────────────────────────────
// Hardcoded to INR — app is India-specific.
// Locale-based detection fails on phones with en_US locale (very common in India).
fun getCurrencySymbol(): String = "₹"

fun formatCurrency(amount: Double): String {
    return "₹${String.format("%,.0f", amount)}"
}

fun getStorePrice(usdPrice: Double): String {
    val converted = usdPrice * 84.0  // approximate USD → INR rate
    return "₹${String.format("%.0f", converted)}"
}

// ─── Pulse Ring ───────────────────────────────────────────────────────────────
@Composable
fun PulseRing(
    color: Color,
    size: Dp = 120.dp,
    delayMs: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_$delayMs")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delayMs, easing = EaseOutCirc),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delayMs, easing = EaseOutCirc),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(1.5.dp, color.copy(alpha = 0.6f), CircleShape)
    )
}

// ─── Gradient Text ────────────────────────────────────────────────────────────
@OptIn(ExperimentalTextApi::class)
@Composable
fun GradientText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = if (isDark) {
        listOf(
            Color(0xFFD8B4FE), // Brighter Purple (Purple 300)
            Color(0xFFA5B4FC), // Brighter Indigo (Indigo 300)
            Color(0xFF22D3EE)  // Brighter Cyan (Cyan 400)
        )
    } else {
        listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd)
    }
    val gradientBrush = Brush.horizontalGradient(colors = colors)
    Text(
        text = text,
        style = style.copy(brush = gradientBrush),
        modifier = modifier
    )
}

// ─── Animated Counter ────────────────────────────────────────────────────────
@Composable
fun AnimatedCounter(
    value: Double,
    prefix: String = "₹",
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "counterAnim"
    )
    Text(
        text = if (prefix == "₹") "₹${String.format("%,.0f", animatedValue.toDouble())}"
               else "$prefix${animatedValue.roundToInt()}",
        style = style,
        color = color,
        modifier = modifier
    )
}

// ─── Neon Badge ───────────────────────────────────────────────────────────────
@Composable
fun NeonBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        modifier = modifier.border(1.dp, color.copy(alpha = glow), RoundedCornerShape(50))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Shimmer Box ─────────────────────────────────────────────────────────────
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1000f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmerX"
    )
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF1C2438) else Color(0xFFE2E8F0)
    val shineColor = if (isDark) Color(0xFF2D3748) else Color(0xFFF8FAFF)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(baseColor, shineColor, baseColor),
                    startX = shimmerX,
                    endX = shimmerX + 600f
                )
            )
    )
}

// ─── Animated Background ──────────────────────────────────────────────────────
@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val color2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
    val color3 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f)

    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "x"
    )
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 200f, targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Reverse),
        label = "y"
    )
    val zOffset by infiniteTransition.animateFloat(
        initialValue = 100f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse),
        label = "z"
    )

    Canvas(modifier = Modifier.fillMaxSize().blur(120.dp)) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color1, Color.Transparent)),
            radius = size.width * 0.85f,
            center = center.copy(x = xOffset - 200f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(color2, Color.Transparent)),
            radius = size.width * 0.90f,
            center = center.copy(y = yOffset)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(color3, Color.Transparent)),
            radius = size.width * 0.65f,
            center = center.copy(x = size.width - xOffset + 300f, y = size.height - yOffset + 200f)
        )
        // Third orb — cyan accent
        drawCircle(
            brush = Brush.radialGradient(listOf(
                color3.copy(alpha = 0.07f),
                Color.Transparent
            )),
            radius = size.width * 0.50f,
            center = center.copy(x = zOffset + 100f, y = size.height - zOffset)
        )
    }
}

// ─── Logo ─────────────────────────────────────────────────────────────────────
@Composable
fun HearthFlowLogo(
    size: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "HearthFlow Logo",
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─── Pie Chart ────────────────────────────────────────────────────────────────
@Composable
fun PieChart(
    data: Map<String, Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    var startAngle = 270f

    // Animate arc sweep on first composition
    val sweepAnim by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "pieSweep"
    )

    Canvas(modifier = modifier.aspectRatio(1f)) {
        data.values.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * sweepAnim
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += (value / total) * 360f
        }
    }
}

// ─── Glass Card ───────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val defaultColor = if (isDark) Color(0x26FFFFFF) else Color(0xCCFFFFFF)
    val color = containerColor ?: defaultColor

    // Animated shimmer border
    val infiniteTransition = rememberInfiniteTransition(label = "cardBorder")
    val borderHue by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "borderHue"
    )

    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                BrandGradientStart.copy(alpha = 0.4f + borderHue * 0.2f),
                BrandGradientMid.copy(alpha = 0.2f),
                BrandGradientEnd.copy(alpha = 0.4f + (1f - borderHue) * 0.2f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0x99C4B5FD),
                Color(0x66A5B4FC),
                Color(0x9967E8F0),
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

// ─── Glass OmniBar ────────────────────────────────────────────────────────────
@Composable
fun GlassOmniBar(
    inputText: String,
    onTextChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassColor  = if (isDark) Color(0x40FFFFFF) else Color(0xCCFFFFFF)
    val borderColor = if (isDark) Color(0x55FFFFFF) else Color(0xAAC4B5FD)

    // Glow when text is being typed
    val hasText = inputText.isNotEmpty()
    val glowAlpha by animateFloatAsState(
        targetValue = if (hasText) 0.5f else 0f,
        animationSpec = tween(300),
        label = "omniGlow"
    )

    Box {
        // Outer glow shadow
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .blur(16.dp)
                    .background(
                        BrandGradientStart.copy(alpha = glowAlpha * 0.3f),
                        RoundedCornerShape(32.dp)
                    )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp)),
            color = glassColor,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(32.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVoiceClick) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice",
                        tint = MaterialTheme.colorScheme.primary)
                }
                TextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Log expense, food, debt…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor  = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                FilledIconButton(
                    onClick = onSendClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

// ─── Log Item ────────────────────────────────────────────────────────────────
@Composable
fun LogItem(
    log: com.example.hearthflow.data.model.LogEntry,
    onDeleteClick: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
    ) {
        val isDark = isSystemInDarkTheme()
        val bg     = if (isDark) Color(0x1EFFFFFF) else Color(0xE6FFFFFF)

        val (avatarIcon, categoryColor, categoryGradient) = when (log.category.name.uppercase()) {
            "EXPENSE"   -> Triple(Icons.Default.ReceiptLong, Color(0xFF7C3AED), Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1))))
            "INVENTORY" -> Triple(Icons.Default.Kitchen, Color(0xFF06B6D4), Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2))))
            "HEALTH"    -> Triple(Icons.Default.Favorite, Color(0xFF10B981), Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))))
            "FITNESS"   -> Triple(Icons.Default.FitnessCenter, Color(0xFFF59E0B), Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
            "CONSUMPTION"-> Triple(Icons.Default.Fastfood, Color(0xFFEF4444), Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C))))
            else        -> Triple(Icons.Default.Description, Color(0xFF94A3B8), Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B))))
        }

        val borderBrush = Brush.linearGradient(
            listOf(categoryColor.copy(alpha = 0.35f), Color.Transparent, categoryColor.copy(alpha = 0.15f))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Glowing Category circular gradient avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(categoryGradient)
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(avatarIcon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    val amount = "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble()
                    val qty = "\"quantity\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Pill Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(categoryGradient)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                log.category.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (amount != null) {
                            AnimatedCounter(
                                value = amount,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = categoryColor
                            )
                        } else if (qty != null) {
                            Text("${qty.toInt()} qty",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.secondary)
                        } else {
                            Text(
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(log.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(log.content, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (amount != null || qty != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(log.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Log",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Press Scale Modifier ─────────────────────────────────────────────────────
fun Modifier.pressScale(targetScale: Float = 0.96f) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

// ─── Stat Metric Card ─────────────────────────────────────────────────────────
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0x22FFFFFF) else Color(0xEEFFFFFF)
    val borderBrush = Brush.linearGradient(
        listOf(accentColor.copy(alpha = 0.5f), accentColor.copy(alpha = 0.1f))
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            // Icon with glow circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Onboarding Financial Card ──────────────────────────────────────────────
@Composable
fun OnboardingFinancialCard(onSetup: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Setup Your Financial Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Configure your monthly income and fixed expenses to unlock your personalized debt payoff workspace.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Configure Profile")
            }
        }
    }
}

// ─── Profile Setup Dialog ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupDialog(
    initialSalary: Double,
    initialExpenses: Double,
    initialInvestments: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double) -> Unit
) {
    var salary by remember { mutableStateOf(if (initialSalary > 0) initialSalary.toLong().toString() else "") }
    var expenses by remember { mutableStateOf(if (initialExpenses > 0) initialExpenses.toLong().toString() else "") }
    var investments by remember { mutableStateOf(if (initialInvestments > 0) initialInvestments.toLong().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            GradientText(
                text = "Financial Profile Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(
                    "Configure your monthly pillars to calibrate recommendations and payoffs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                // Net Salary
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Net Income (Salary)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    prefix = { Text(getCurrencySymbol() + " ", color = BrandVioletText, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedLabelColor = BrandViolet,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = Color(0x10FFFFFF),
                        unfocusedContainerColor = Color(0x05FFFFFF),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Fixed Expenses
                OutlinedTextField(
                    value = expenses,
                    onValueChange = { expenses = it },
                    label = { Text("Monthly Fixed Expenses (Bills, Rent)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    prefix = { Text(getCurrencySymbol() + " ", color = BrandVioletText, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedLabelColor = BrandViolet,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = Color(0x10FFFFFF),
                        unfocusedContainerColor = Color(0x05FFFFFF),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Monthly Investments
                OutlinedTextField(
                    value = investments,
                    onValueChange = { investments = it },
                    label = { Text("Monthly Investments (Stocks, Mutual Funds)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    prefix = { Text(getCurrencySymbol() + " ", color = BrandCyan, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedLabelColor = BrandCyan,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = Color(0x10FFFFFF),
                        unfocusedContainerColor = Color(0x05FFFFFF),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val sVal = salary.toDoubleOrNull() ?: 0.0
                val eVal = expenses.toDoubleOrNull() ?: 0.0
                val iVal = investments.toDoubleOrNull() ?: 0.0
                val disc = sVal - eVal - iVal

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (disc >= 0) BrandViolet.copy(alpha = 0.08f)
                            else BrandRose.copy(alpha = 0.08f)
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = if (disc >= 0) "Discretionary Cash Flow:" else "Cash Flow Deficit:",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (disc >= 0) BrandViolet else BrandRose,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${formatCurrency(Math.abs(disc))}/mo remaining for debt acceleration",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (disc >= 0) Color.White else BrandRose.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        salary.toDoubleOrNull() ?: 0.0,
                        expenses.toDoubleOrNull() ?: 0.0,
                        investments.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = salary.toDoubleOrNull() != null && expenses.toDoubleOrNull() != null && investments.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.pressScale()
            ) { Text("Save Settings", fontWeight = FontWeight.Bold) }
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

// ─── Util ─────────────────────────────────────────────────────────────────────
@Composable
fun Dp.toTextUnit() = androidx.compose.ui.unit.TextUnit(
    this.value, androidx.compose.ui.unit.TextUnitType.Sp
)

@Composable
fun PremiumFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )

    val chipBg = if (selected) {
        Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientEnd))
    } else {
        Brush.horizontalGradient(listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF)))
    }

    val chipBorder = if (selected) {
        Brush.horizontalGradient(listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd))
    } else {
        Brush.horizontalGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF)))
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun PremiumSubscriptionDialog(
    onDismiss: () -> Unit,
    onPurchase: (planName: String, durationDays: Long) -> Unit,
    monthlyPrice: String = "₹199",
    yearlyPrice: String = "₹1199",
    lifetimePrice: String = "₹2999"
) {
    var checkoutStep by remember { mutableStateOf(0) } // 0 = Selection, 1 = Checkout form, 2 = Loader, 3 = Success
    var selectedPlanIndex by remember { mutableStateOf(0) }
    
    val plans = listOf(
        Triple("Monthly Plan", monthlyPrice, 30L),
        Triple("Yearly Plan", yearlyPrice, 365L)
    )

    // Form inputs for Credit/Debit card
    var paymentMethod by remember { mutableStateOf("UPI") } // "UPI" or "Card"
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    // Form inputs for UPI
    var upiId by remember { mutableStateOf("") }
    var isUpiVerified by remember { mutableStateOf(false) }

    // Error messages
    var errorMessage by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (checkoutStep < 2) onDismiss()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131929))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(BrandViolet.copy(alpha = 0.5f), Color(0x11FFFFFF), BrandCyan.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (checkoutStep == 0) {
                    // CROWN ICON & TITLE
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GradientText(
                        text = "HearthFlow Premium Plan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Unlock the full suite of Split Ledger & Debt payoff planners with advanced tracking algorithms.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ADVANCED PREMIUM BENEFITS LIST
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val benefits = listOf(
                            "👥  Unlimited Split Groups (Free: max 1)",
                            "📝  Unlimited Active Split Bills (Free: max 2)",
                            "📉  Unlimited Debt Payoff Roadmap Tracking (Free: max 1)",
                            "📈  Unlimited Asset Growth Portfolios (Free: max 1)",
                            "📊  Dynamic visual breakdowns & automated calculations",
                            "🔒  Ad-free premium offline-first data sync layers"
                        )
                        benefits.forEach { benefit ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // PLANS LIST SELECTORS
                    plans.forEachIndexed { index, plan ->
                        val isSelected = selectedPlanIndex == index
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 1f,
                            label = "planScale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) BrandViolet.copy(alpha = 0.15f) else Color(0x0EFFFFFF)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    brush = if (isSelected) {
                                        Brush.horizontalGradient(listOf(BrandViolet, BrandCyan))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF)))
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedPlanIndex = index }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = plan.first,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BrandCyan else Color.White
                                        )
                                    )
                                    Text(
                                        text = if (plan.third >= 36500L) "Lifetime full access" else "${plan.third} Days of full access",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = plan.second,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlanIndex = index },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = BrandCyan,
                                            unselectedColor = Color.White.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { checkoutStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressScale(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandViolet,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Proceed to Checkout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Maybe Later",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                } else if (checkoutStep == 1) {
                    // PAYMENT FORM SCREEN
                    val plan = plans[selectedPlanIndex]
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { checkoutStep = 0 }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        Text(
                            text = "Secure Checkout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(48.dp)) // Equal spacing balance
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Brief Plan Summary Item
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = plan.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "HearthFlow Pro Features Sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = plan.second,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tab Selector for Payment Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val methods = listOf("UPI", "Card")
                        methods.forEach { method ->
                            val isSel = paymentMethod == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) BrandViolet.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, if (isSel) BrandViolet else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        paymentMethod = method
                                        errorMessage = ""
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (method == "UPI") "UPI Mobile" else "Credit/Debit Card",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (paymentMethod == "Card") {
                        // LIVE CARD PREVIEW REPLICA
                        CardPreview(cardNumber, cardHolder, expiryDate, cvv)
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        // Card Input Fields
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.length <= 16) {
                                    cardNumber = clean
                                }
                            },
                            label = { Text("Card Number (16 Digits)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = BrandViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedLabelColor = BrandViolet,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                cursorColor = BrandViolet,
                                focusedContainerColor = Color(0xFF1C2438),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = cardHolder,
                            onValueChange = { if (it.length <= 22) cardHolder = it },
                            label = { Text("Cardholder Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = BrandViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedLabelColor = BrandViolet,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                cursorColor = BrandViolet,
                                focusedContainerColor = Color(0xFF1C2438),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { input ->
                                    var clean = input.filter { it.isDigit() || it == '/' }
                                    if (clean.length == 2 && expiryDate.length == 1 && !clean.contains("/")) {
                                        clean += "/"
                                    }
                                    if (clean.length <= 5) {
                                        expiryDate = clean
                                    }
                                },
                                label = { Text("Expiry (MM/YY)") },
                                placeholder = { Text("12/28") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = BrandViolet,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedLabelColor = BrandViolet,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    cursorColor = BrandViolet,
                                    focusedContainerColor = Color(0xFF1C2438),
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { input ->
                                    val clean = input.filter { it.isDigit() }
                                    if (clean.length <= 3) {
                                        cvv = clean
                                    }
                                },
                                label = { Text("CVV") },
                                placeholder = { Text("123") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = BrandViolet,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedLabelColor = BrandViolet,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    cursorColor = BrandViolet,
                                    focusedContainerColor = Color(0xFF1C2438),
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }

                    } else {
                        // UPI INPUT FORM
                        Text(
                            text = "Enter your Virtual Payment Address (UPI ID):",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = upiId,
                            onValueChange = {
                                upiId = it.trim()
                                isUpiVerified = it.contains("@") && it.length >= 5
                            },
                            label = { Text("UPI ID (e.g. mobile@ybl)") },
                            placeholder = { Text("yourname@okaxis") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (isUpiVerified) {
                                    Text("Verified ✓", color = BrandCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = BrandCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedLabelColor = BrandCyan,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                cursorColor = BrandCyan,
                                focusedContainerColor = Color(0xFF1C2438),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fast Apps row
                        Text(
                            text = "Or Pay Instantly with Supported Apps:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val upiApps = listOf("GPay" to "okaxis", "PhonePe" to "ybl", "Paytm" to "paytm")
                            upiApps.forEach { app ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            upiId = "hearthflow.${app.first.lowercase()}@${app.second}"
                                            isUpiVerified = true
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.first,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage, color = BrandRose, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // High-glowing Pay button
                    Button(
                        onClick = {
                            if (paymentMethod == "Card") {
                                if (cardNumber.length < 16) {
                                    errorMessage = "Please enter a valid 16-digit card number."
                                } else if (cardHolder.isBlank()) {
                                    errorMessage = "Please enter cardholder name."
                                } else if (expiryDate.length < 5 || !expiryDate.contains("/")) {
                                    errorMessage = "Please enter valid expiry MM/YY."
                                } else if (cvv.length < 3) {
                                    errorMessage = "Please enter valid 3-digit CVV."
                                } else {
                                    checkoutStep = 2
                                    errorMessage = ""
                                }
                            } else {
                                if (!isUpiVerified) {
                                    errorMessage = "Please enter a valid UPI ID (e.g. mobile@upi)."
                                } else {
                                    checkoutStep = 2
                                    errorMessage = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressScale(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Pay Securely ${plan.second}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    }

                } else if (checkoutStep == 2) {
                    // GLOWING SECURE PAYMENT PROCESSING LOADER
                    val infiniteTransition = rememberInfiniteTransition(label = "secure_rotation")
                    val rotationAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "loaderRotate"
                    )

                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2500)
                        checkoutStep = 3
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        PulseRing(color = BrandCyan, size = 160.dp, delayMs = 0)
                        PulseRing(color = BrandViolet, size = 160.dp, delayMs = 1000)

                        // Circular progress ring
                        CircularProgressIndicator(
                            strokeWidth = 4.dp,
                            color = BrandCyan,
                            modifier = Modifier
                                .size(76.dp)
                                .graphicsLayer { rotationZ = rotationAngle }
                        )

                        Text("💸", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Verifying Transaction Security...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Authenticating with central payment servers. Do not hit back or lock your screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

                } else if (checkoutStep == 3) {
                    // CELEBRATION SUCCESS SCREEN
                    val plan = plans[selectedPlanIndex]
                    val randomTxnId = remember { "TXN_OMNI_${(100000..999999).random()}" }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandEmerald, Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Payment Successful! 🎉",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Welcome to HearthFlow Pro",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = BrandCyan
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Transaction Bill Details Breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transaction ID", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                            Text(randomTxnId, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Purchased Tier", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                            Text(plan.first, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount Billed", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                            Text(plan.second + ".00", color = BrandCyan, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Account Status", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                            Text("ACTIVE PRO", color = BrandEmerald, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onPurchase(plan.first, plan.third)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressScale(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Let's Go!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CardPreview(
    number: String,
    holder: String,
    expiry: String,
    cvv: String
) {
    val displayCardNumber = remember(number) {
        val clean = number.replace(" ", "")
        val sb = StringBuilder()
        for (i in 0 until 16) {
            if (i < clean.length) {
                sb.append(clean[i])
            } else {
                sb.append("•")
            }
            if (i % 4 == 3 && i < 15) {
                sb.append(" ")
            }
        }
        sb.toString()
    }
    
    val displayExpiry = if (expiry.isEmpty()) "MM/YY" else expiry
    val displayHolder = if (holder.isEmpty()) "CARDHOLDER NAME" else holder.uppercase()
    val displayCvv = if (cvv.isEmpty()) "•••" else cvv
    
    val cardBrush = Brush.linearGradient(
        colors = listOf(
            BrandViolet,
            BrandCyan,
            BrandIndigo
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBrush)
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HEARTHFLOW SECURE CARD",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                // Chip icon replica
                Box(
                    modifier = Modifier
                        .size(36.dp, 26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2B93B))
                )
            }
            
            Text(
                text = displayCardNumber,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CARD HOLDER",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = displayHolder,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "EXPIRES",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayExpiry,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CVV",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayCvv,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

