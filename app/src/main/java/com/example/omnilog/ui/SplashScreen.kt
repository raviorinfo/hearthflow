package com.example.omnilog.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnilog.ui.theme.BrandGradientEnd
import com.example.omnilog.ui.theme.BrandGradientMid
import com.example.omnilog.ui.theme.BrandGradientStart
import kotlinx.coroutines.delay

@OptIn(ExperimentalTextApi::class)
@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Logo scale — bouncy entrance
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "logoAlpha"
    )

    // Breathing loop after entrance
    val breathe = rememberInfiniteTransition(label = "breathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breatheScale"
    )

    // Progress bar
    val progress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(2400, easing = LinearEasing),
        label = "progress"
    )

    // Shimmer sweep across text
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "shimmerX"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2600)
        onAnimationFinished()
    }

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd)
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            BrandGradientStart,
            BrandGradientEnd,
            Color.White,
            BrandGradientEnd,
            BrandGradientStart
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 400f, 0f)
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedBackground()

        // Concentric pulse rings behind logo
        Box(contentAlignment = Alignment.Center) {
            PulseRing(
                color = MaterialTheme.colorScheme.primary,
                size = 260.dp,
                delayMs = 0
            )
            PulseRing(
                color = MaterialTheme.colorScheme.secondary,
                size = 200.dp,
                delayMs = 600
            )
            PulseRing(
                color = MaterialTheme.colorScheme.tertiary,
                size = 150.dp,
                delayMs = 1200
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RoutineLogo(
                    size = 110.dp,
                    modifier = Modifier
                        .scale(scale * if (startAnimation) breatheScale else 1f)
                        .graphicsLayer(alpha = alpha)
                )

                Spacer(Modifier.height(28.dp))

                AnimatedVisibility(
                    visible = startAnimation,
                    enter = fadeIn(tween(800, 400)) + expandVertically(tween(800, 400))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Gradient shimmer app name
                        Text(
                            text = "RoutineLog",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                brush = shimmerBrush
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Your Life, Logged.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Bottom progress bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 48.dp, vertical = 48.dp)
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(gradientBrush)
            )
        }
    }
}
