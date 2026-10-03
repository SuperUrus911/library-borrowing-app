package dev.ijlal.stacks.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// "Ink and paper": deep ink blue, warm paper surfaces, amber bookmark accents.
private val LightColors = lightColorScheme(
    primary = Color(0xFF23395B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E2F5),
    onPrimaryContainer = Color(0xFF0B1D36),
    secondary = Color(0xFFA9661F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBE3C4),
    onSecondaryContainer = Color(0xFF3D2405),
    tertiary = Color(0xFF3F6E4B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3EBD8),
    onTertiaryContainer = Color(0xFF0E2A16),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFEEE8DF),
    onSurfaceVariant = Color(0xFF5A554E),
    outline = Color(0xFF8A847B),
    outlineVariant = Color(0xFFD9D2C7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2EB),
    surfaceContainer = Color(0xFFF1ECE4),
    surfaceContainerHigh = Color(0xFFEBE5DC),
    surfaceContainerHighest = Color(0xFFE5DFD5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C4EC),
    onPrimary = Color(0xFF0E2747),
    primaryContainer = Color(0xFF2A4468),
    onPrimaryContainer = Color(0xFFD6E2F5),
    secondary = Color(0xFFF0B977),
    onSecondary = Color(0xFF4A2C07),
    secondaryContainer = Color(0xFF6B4517),
    onSecondaryContainer = Color(0xFFFBE3C4),
    tertiary = Color(0xFFA3CFAE),
    onTertiary = Color(0xFF0E2A16),
    tertiaryContainer = Color(0xFF2F5139),
    onTertiaryContainer = Color(0xFFD3EBD8),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121417),
    onBackground = Color(0xFFE5E2DD),
    surface = Color(0xFF121417),
    onSurface = Color(0xFFE5E2DD),
    surfaceVariant = Color(0xFF2B2E33),
    onSurfaceVariant = Color(0xFFC4C0B8),
    outline = Color(0xFF8E8A83),
    outlineVariant = Color(0xFF43464B),
    surfaceContainerLowest = Color(0xFF0D0F12),
    surfaceContainerLow = Color(0xFF1A1C20),
    surfaceContainer = Color(0xFF1E2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

private val BaseType = Typography()

private val StacksTypography = Typography(
    displaySmall = BaseType.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    headlineLarge = BaseType.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    headlineMedium = BaseType.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    headlineSmall = BaseType.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    titleLarge = BaseType.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    titleMedium = BaseType.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseType.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseType.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val StacksShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun StacksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = StacksTypography,
        shapes = StacksShapes,
        content = content,
    )
}
