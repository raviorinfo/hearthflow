package com.example.omnilog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp

// ─── Deep Navy Brand Palette ─────────────────────────────────────────────────
private val NavyDeep       = Color(0xFF0A0E1A)   // Background base
private val NavySurface    = Color(0xFF131929)   // Card surfaces
private val NavyElevated   = Color(0xFF1C2438)   // Elevated surfaces

private val BrandViolet    = Color(0xFF7C3AED)   // Primary – vivid violet
private val BrandIndigo    = Color(0xFF6366F1)   // Secondary – indigo
private val BrandCyan      = Color(0xFF06B6D4)   // Tertiary – cyan accent
private val BrandAmber     = Color(0xFFF59E0B)   // Warning/highlight
private val BrandRose      = Color(0xFFE11D48)   // Error / destructive

private val OnBrand        = Color(0xFFFFFFFF)
private val TextPrimary    = Color(0xFFF1F5F9)   // Slate 100
private val TextSecondary  = Color(0xFF94A3B8)   // Slate 400
private val TextMuted      = Color(0xFF475569)   // Slate 600

// ─── Light surface palette ────────────────────────────────────────────────────
private val LightBg        = Color(0xFFF8FAFF)
private val LightSurface   = Color(0xFFFFFFFF)
private val LightElevated  = Color(0xFFEEF2FF)
private val LightOnBg      = Color(0xFF1E293B)
private val LightOnSurface = Color(0xFF334155)

private val DarkColorScheme = darkColorScheme(
    primary              = BrandViolet,
    onPrimary            = OnBrand,
    primaryContainer     = Color(0xFF3B1FA8),
    onPrimaryContainer   = Color(0xFFD9C4FF),

    secondary            = BrandIndigo,
    onSecondary          = OnBrand,
    secondaryContainer   = Color(0xFF2D2F8F),
    onSecondaryContainer = Color(0xFFCACFFF),

    tertiary             = BrandCyan,
    onTertiary           = Color(0xFF003547),
    tertiaryContainer    = Color(0xFF004D63),
    onTertiaryContainer  = Color(0xFF9EF0FF),

    error                = BrandRose,
    onError              = OnBrand,

    background           = NavyDeep,
    onBackground         = TextPrimary,

    surface              = NavySurface,
    onSurface            = TextPrimary,
    surfaceVariant       = NavyElevated,
    onSurfaceVariant     = TextSecondary,

    outline              = TextMuted,
    outlineVariant       = Color(0xFF2D3748),
)

private val LightColorScheme = lightColorScheme(
    primary              = BrandViolet,
    onPrimary            = OnBrand,
    primaryContainer     = Color(0xFFEDE9FE),
    onPrimaryContainer   = Color(0xFF3B0764),

    secondary            = BrandIndigo,
    onSecondary          = OnBrand,
    secondaryContainer   = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),

    tertiary             = Color(0xFF0891B2),
    onTertiary           = OnBrand,
    tertiaryContainer    = Color(0xFFCFF4FD),
    onTertiaryContainer  = Color(0xFF002633),

    error                = BrandRose,
    onError              = OnBrand,

    background           = LightBg,
    onBackground         = LightOnBg,

    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightElevated,
    onSurfaceVariant     = Color(0xFF475569),

    outline              = Color(0xFFCBD5E1),
    outlineVariant       = Color(0xFFE2E8F0),
)

// ─── Typography ──────────────────────────────────────────────────────────────
private val AppTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ─── Shapes ──────────────────────────────────────────────────────────────────
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ─── Theme Composable ─────────────────────────────────────────────────────────
@Composable
fun RoutineLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
