package com.example.calorietracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Brand accents used for gradients and per-metric colours. Same in light and dark mode. */
object Brand {
    val Violet = Color(0xFF6C5CFF)
    val Purple = Color(0xFF9B4DFF)
    val Pink = Color(0xFFFF5F8F)
    val Amber = Color(0xFFFFB547)
    val Orange = Color(0xFFFF8A3D)
    val Sky = Color(0xFF3EC6FF)
    val Green = Color(0xFF34D399)

    /** Hero cards and the main call-to-action buttons. */
    val HeroGradient = Brush.linearGradient(listOf(Violet, Purple, Pink))

    /** A deeper version of [HeroGradient] behind the calorie ring, so the macro colours stand out. */
    val HeroCardGradient = Brush.linearGradient(listOf(Color(0xFF2A1C7A), Color(0xFF4A1F8F), Color(0xFF7A2266)))
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B47F5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF1E1066),
    secondary = Color(0xFFF0623E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1D7),
    onSecondaryContainer = Color(0xFF3D1003),
    tertiary = Color(0xFF0FA89C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDF5F0),
    onTertiaryContainer = Color(0xFF00302C),
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF16151D),
    surface = Color(0xFFF6F5FB),
    onSurface = Color(0xFF16151D),
    surfaceVariant = Color(0xFFE6E3F0),
    onSurfaceVariant = Color(0xFF5E5B70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF1EFF8),
    surfaceContainerHigh = Color(0xFFEAE8F3),
    surfaceContainerHighest = Color(0xFFE2DFEE),
    outline = Color(0xFF8C899D),
    outlineVariant = Color(0xFFDCD9E8),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF5A0A0D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8E80FF),
    onPrimary = Color(0xFF14104A),
    primaryContainer = Color(0xFF2E2760),
    onPrimaryContainer = Color(0xFFE3DEFF),
    secondary = Color(0xFFFF8A6B),
    onSecondary = Color(0xFF3D1003),
    secondaryContainer = Color(0xFF4A2418),
    onSecondaryContainer = Color(0xFFFFDBD0),
    tertiary = Color(0xFF2FD3C5),
    onTertiary = Color(0xFF00201D),
    tertiaryContainer = Color(0xFF0F3F3B),
    onTertiaryContainer = Color(0xFFB4F5EE),
    background = Color(0xFF0E0E14),
    onBackground = Color(0xFFF2F1F7),
    surface = Color(0xFF0E0E14),
    onSurface = Color(0xFFF2F1F7),
    surfaceVariant = Color(0xFF2C2C3A),
    onSurfaceVariant = Color(0xFFA8A6B8),
    surfaceContainerLowest = Color(0xFF0A0A0F),
    surfaceContainerLow = Color(0xFF17171F),
    surfaceContainer = Color(0xFF1C1C26),
    surfaceContainerHigh = Color(0xFF23232F),
    surfaceContainerHighest = Color(0xFF2C2C3A),
    outline = Color(0xFF5A5870),
    outlineVariant = Color(0xFF34344A),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0003),
    errorContainer = Color(0xFF4D1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * The app uses its own palette. Set [dynamicColor] to true to follow the wallpaper
 * colours on Android 12+ instead.
 */
@Composable
fun CalorieTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
