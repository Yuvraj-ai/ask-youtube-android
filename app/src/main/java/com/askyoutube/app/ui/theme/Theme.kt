package com.askyoutube.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Palette and type taken from the Stitch design system, which was generated from
 * .stitch/DESIGN.md. Values are the exact hex codes Stitch emitted, not
 * approximations.
 *
 * Dynamic colour is deliberately NOT used. The design system commits to one warm
 * accent across every screen; letting the wallpaper repaint it would break that
 * on most devices.
 */
private val Ember = Color(0xFFC2410C)
private val EmberDark = Color(0xFFFB923C)
private val Ink = Color(0xFF18181B)
private val InkMuted = Color(0xFF71717A)
private val InkFaint = Color(0xFFA1A1AA)
private val Canvas = Color(0xFFFAFAF9)
private val Surface = Color(0xFFFFFFFF)
private val SurfaceMuted = Color(0xFFF4F4F5)
private val Hairline = Color(0xFFE4E4E7)
private val ErrorRed = Color(0xFFB91C1C)
private val ErrorRedDark = Color(0xFFFCA5A5)
private val DarkCanvas = Color(0xFF09090B)
private val DarkSurface = Color(0xFF18181B)
private val DarkSurfaceMuted = Color(0xFF27272A)
private val DarkHairline = Color(0xFF3F3F46)

/** Colours the Material 3 scheme has no slot for. */
data class AppColors(
    val canvas: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val hairline: Color,
    val ember: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

private val LightAppColors = AppColors(
    canvas = Canvas,
    surface = Surface,
    surfaceMuted = SurfaceMuted,
    ink = Ink,
    inkMuted = InkMuted,
    inkFaint = InkFaint,
    hairline = Hairline,
    ember = Ember,
    danger = ErrorRed,
    dangerContainer = Color(0xFFFEE2E2),
    onDangerContainer = Color(0xFF7F1D1D),
)

private val DarkAppColors = AppColors(
    canvas = DarkCanvas,
    surface = DarkSurface,
    surfaceMuted = DarkSurfaceMuted,
    ink = Color(0xFFFAFAF9),
    inkMuted = Color(0xFFA1A1AA),
    inkFaint = Color(0xFF71717A),
    hairline = DarkHairline,
    ember = EmberDark,
    danger = ErrorRedDark,
    dangerContainer = Color(0xFF450A0A),
    onDangerContainer = Color(0xFFFECACA),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/**
 * The app's one typographic signature: technical data — timestamps, model ids,
 * numeric values — is set in monospace so it reads as data rather than prose.
 * Everything else stays on the platform sans.
 */
val MonoSmall = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Medium,
)

val MonoBody = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
)

private val LightScheme = lightColorScheme(
    primary = Ember,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF832600),
    secondary = Color(0xFF5D5E66),
    onSecondary = Color.White,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkMuted,
    background = Canvas,
    onBackground = Ink,
    outline = Hairline,
    outlineVariant = Hairline,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkScheme = darkColorScheme(
    primary = EmberDark,
    onPrimary = Color(0xFF561F00),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    surface = DarkSurface,
    onSurface = Color(0xFFFAFAF9),
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = Color(0xFFA1A1AA),
    background = DarkCanvas,
    onBackground = Color(0xFFFAFAF9),
    outline = DarkHairline,
    outlineVariant = DarkHairline,
    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        // The answer is the only place leading really matters, because it is
        // the only place anyone reads at length.
        bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

/**
 * Material 3 Expressive shape scale.
 *
 * The "Increased" and extraExtraLarge slots are the expressive additions: they
 * are deliberately softer than the classic scale, which is what gives the app its
 * rounder, friendlier feel. Screens read these from MaterialTheme.shapes rather
 * than hardcoding radii, so changing one number here restyles every component.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    largeIncreased = RoundedCornerShape(26.dp),
    extraExtraLarge = RoundedCornerShape(36.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AskYoutubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            // The expressive motion scheme supplies spring-based spatial specs
            // and effects specs, which is what M3 Expressive components animate
            // with. Safe defaults are used automatically when omitted, but
            // passing it explicitly makes the intent visible and lets the
            // components below pick it up.
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}
