package com.example.omnilog.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.omnilog.R
import java.util.Locale

// ─── Currency Helpers ─────────────────────────────────────────────────────────
fun getCurrencySymbol(): String {
    val locale = Locale.getDefault()
    // Explicit check for India regardless of language
    if (locale.country.equals("IN", ignoreCase = true)) return "₹"
    
    return try {
        val currency = java.util.Currency.getInstance(locale)
        if (currency.currencyCode == "INR") "₹"
        else currency.symbol
    } catch (e: Exception) {
        "$"
    }
}

fun formatCurrency(amount: Double): String {
    val symbol = getCurrencySymbol()
    return if (symbol == "₹") {
        "₹${String.format("%,.0f", amount)}"
    } else {
        "$symbol${String.format("%,.2f", amount)}"
    }
}

fun getStorePrice(usdPrice: Double): String {
    val symbol = getCurrencySymbol()
    val rate = if (symbol == "₹") 80.0 else 1.0
    val converted = usdPrice * rate
    return if (symbol == "₹") {
        "₹${String.format("%.0f", converted)}"
    } else {
        "$symbol${String.format("%.2f", converted)}"
    }
}

// ─── Animated Background ──────────────────────────────────────────────────────
@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val color2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    val color3 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f)

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

    Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color1, Color.Transparent)),
            radius = size.width * 0.8f,
            center = center.copy(x = xOffset - 200f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(color2, Color.Transparent)),
            radius = size.width * 0.9f,
            center = center.copy(y = yOffset)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(color3, Color.Transparent)),
            radius = size.width * 0.6f,
            center = center.copy(x = size.width - xOffset + 300f, y = size.height - yOffset + 200f)
        )
    }
}

// ─── Logo ─────────────────────────────────────────────────────────────────────
@Composable
fun RoutineLogo(
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
            contentDescription = "RoutineLog Logo",
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
    
    Canvas(modifier = modifier.aspectRatio(1f)) {
        data.values.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
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
    val borderColor  = if (isDark) Color(0x33FFFFFF) else Color(0x66C4B5FD)
    val color = containerColor ?: defaultColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
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
    onImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassColor  = if (isDark) Color(0x33FFFFFF) else Color(0xCCFFFFFF)
    val borderColor = if (isDark) Color(0x44FFFFFF) else Color(0x88C4B5FD)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        color = glassColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onImageClick) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onVoiceClick) {
                Icon(Icons.Default.Mic, contentDescription = "Voice",
                    tint = MaterialTheme.colorScheme.primary)
            }
            TextField(
                value = inputText,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Log expense, food...",
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

// ─── Log Item ────────────────────────────────────────────────────────────────
@Composable
fun LogItem(log: com.example.omnilog.data.model.LogEntry) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
    ) {
        val isDark = isSystemInDarkTheme()
        val bg     = if (isDark) Color(0x1EFFFFFF) else Color(0xE6FFFFFF)
        val border = if (isDark) Color(0x33FFFFFF) else Color(0x66C4B5FD)

        val categoryColor = when (log.category.name.uppercase()) {
            "EXPENSE"   -> Color(0xFF7C3AED)
            "INVENTORY" -> Color(0xFF06B6D4)
            "HEALTH"    -> Color(0xFF10B981)
            "FITNESS"   -> Color(0xFFF59E0B)
            else        -> Color(0xFF94A3B8)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(16.dp))
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                // Category indicator strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(categoryColor)
                        .align(Alignment.CenterVertically)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Extract Amount or Quantity
                    val amount = "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble()
                    val qty = "\"quantity\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDouble()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = categoryColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                log.category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        
                        if (amount != null) {
                            Text(formatCurrency(amount), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        } else if (qty != null) {
                            Text("${qty.toInt()} qty", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        } else {
                            Text(
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(log.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(log.content, style = MaterialTheme.typography.bodyMedium)
                    if (amount != null || qty != null) {
                        Text(
                            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(log.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// ─── Press Scale Modifier ─────────────────────────────────────────────────────
fun Modifier.pressScale() = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

// ─── Util ─────────────────────────────────────────────────────────────────────
@Composable
fun Dp.toTextUnit() = androidx.compose.ui.unit.TextUnit(
    this.value, androidx.compose.ui.unit.TextUnitType.Sp
)
