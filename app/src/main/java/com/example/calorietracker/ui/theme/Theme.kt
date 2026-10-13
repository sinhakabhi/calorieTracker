package com.example.calorietracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.R

/**
 * Fixed accent colours for data (the same in light and dark mode). The UI chrome uses the
 * neutral colour scheme below; these are reserved for what they represent.
 */
object Accents {
    val Protein = Color(0xFFF07A8C)
    val Carbs = Color(0xFFF2B35B)
    val Fat = Color(0xFF4FC3A1)
    val Water = Color(0xFF4DA3F5)
    val Success = Color(0xFF3CCB7F)
}

// ---------------------------------------------------------------------------------------------
// Typography: Inter (bundled variable font, SIL Open Font License; see licenses/Inter-OFL.txt).
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
    inter(FontWeight.ExtraBold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

private val AppTypography = Typography(
    displayLarge = style(56, 60, FontWeight.Bold, -1.5),
    displayMedium = style(44, 48, FontWeight.Bold, -1.2),
    displaySmall = style(34, 40, FontWeight.Bold, -0.8),
    headlineLarge = style(30, 36, FontWeight.Bold, -0.6),
    headlineMedium = style(26, 32, FontWeight.Bold, -0.5),
    headlineSmall = style(22, 28, FontWeight.SemiBold, -0.3),
    titleLarge = style(20, 26, FontWeight.SemiBold, -0.2),
    titleMedium = style(16, 22, FontWeight.SemiBold, -0.1),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium, 0.2),
    labelSmall = style(11, 14, FontWeight.Medium, 0.4),
)

/** Tabular (fixed-width) digits so numbers line up and don't jump as they change. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Small all-caps section label, e.g. "THIS WEEK". */
val TextStyle.overline: TextStyle
    get() = copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)

// ---------------------------------------------------------------------------------------------
// Shapes: one radius scale for everything.
// ---------------------------------------------------------------------------------------------

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// ---------------------------------------------------------------------------------------------
// Colours: neutral surfaces with hairline borders and a single violet accent.
// ---------------------------------------------------------------------------------------------

private val LightColors = lightColorScheme(
    primary = Color(0xFF5146E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E6FF),
    onPrimaryContainer = Color(0xFF1A1466),
    secondary = Color(0xFF5E5C70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E5EF),
    onSecondaryContainer = Color(0xFF1B1A26),
    tertiary = Color(0xFF2F8F78),
    onTertiary = Color.White,
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF111116),
    surface = Color(0xFFF6F6F8),
    onSurface = Color(0xFF111116),
    surfaceVariant = Color(0xFFE9E9EE),
    onSurfaceVariant = Color(0xFF62626F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F0F4),
    surfaceContainerHigh = Color(0xFFE9E9EE),
    surfaceContainerHighest = Color(0xFFE1E1E8),
    outline = Color(0xFFC6C6D0),
    outlineVariant = Color(0xFFE4E4EA),
    error = Color(0xFFD93F4C),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E9),
    onErrorContainer = Color(0xFF5A0A12),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8C86FF),
    onPrimary = Color(0xFF110E3A),
    primaryContainer = Color(0xFF26244D),
    onPrimaryContainer = Color(0xFFDDDBFF),
    secondary = Color(0xFFB9B8CC),
    onSecondary = Color(0xFF24232F),
    secondaryContainer = Color(0xFF2A2A35),
    onSecondaryContainer = Color(0xFFE2E1F0),
    tertiary = Color(0xFF6FD3B6),
    onTertiary = Color(0xFF00382B),
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFEDEDF2),
    surface = Color(0xFF0B0B0F),
    onSurface = Color(0xFFEDEDF2),
    surfaceVariant = Color(0xFF1F1F27),
    onSurfaceVariant = Color(0xFF9A9AA8),
    surfaceContainerLowest = Color(0xFF07070A),
    surfaceContainerLow = Color(0xFF131318),
    surfaceContainer = Color(0xFF18181E),
    surfaceContainerHigh = Color(0xFF1F1F26),
    surfaceContainerHighest = Color(0xFF292931),
    outline = Color(0xFF3B3B47),
    outlineVariant = Color(0xFF24242C),
    error = Color(0xFFFF6B75),
    onError = Color(0xFF3B0008),
    errorContainer = Color(0xFF3A1418),
    onErrorContainer = Color(0xFFFFD9DC),
)

/**
 * The app's design system. Set [dynamicColor] to true to follow the wallpaper colours on
 * Android 12+ instead of the app's own palette.
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
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, shapes = AppShapes, content = content)
}
