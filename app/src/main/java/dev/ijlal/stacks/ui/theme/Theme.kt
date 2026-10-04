package dev.ijlal.stacks.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ijlal.stacks.R

// Dark only: blue-black background, cream text, light blue accent. No warm colours,
// so overdue/urgent things are shown by inverting to cream instead of using red.
object Midnight {
    val Void = Color(0xFF08090F)
    val Surface1 = Color(0xFF10121A)
    val Surface2 = Color(0xFF161923)
    val Surface3 = Color(0xFF1F232E)
    val Hairline = Color(0xFF262B38)
    val HairlineStrong = Color(0xFF3A4152)
    val Cream = Color(0xFFE0DCD0)
    val CreamMuted = Color(0xFFA9A59B)
    val CreamFaint = Color(0xFF8A877F)
    val Ice = Color(0xFF9DB4D6)
    val IceDeep = Color(0xFF3B5276)
    val IceContainer = Color(0xFF172133)
    val Frost = Color(0xFFE5C3D1)
}

object StacksFonts {
    val Blackletter = FontFamily(Font(R.font.unifraktur_maguntia))

    val Serif = FontFamily(
        Font(R.font.instrument_serif),
        Font(R.font.instrument_serif_italic, style = FontStyle.Italic),
    )

    val Sans = FontFamily(
        plexSans(FontWeight.Normal),
        plexSans(FontWeight.Medium),
        plexSans(FontWeight.SemiBold),
        plexSans(FontWeight.Bold),
    )

    val Mono = FontFamily(
        Font(R.font.ibm_plex_mono, FontWeight.Normal),
        Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    )

    // Plex Sans is a variable font, so every weight is the same file with a different axis value
    private fun plexSans(weight: FontWeight) = Font(
        R.font.ibm_plex_sans,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )
}

object StacksType {
    // callers uppercase the text themselves
    val Eyebrow = TextStyle(
        fontFamily = StacksFonts.Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
    )

    val Stamp = TextStyle(
        fontFamily = StacksFonts.Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.sp,
    )

    val Numeral = TextStyle(
        fontFamily = StacksFonts.Serif,
        fontSize = 52.sp,
        lineHeight = 52.sp,
    )
}

private val ColorScheme = darkColorScheme(
    primary = Midnight.Ice,
    onPrimary = Midnight.Void,
    primaryContainer = Midnight.IceContainer,
    onPrimaryContainer = Color(0xFFC9D7EC),
    inversePrimary = Midnight.IceDeep,
    secondary = Midnight.Cream,
    onSecondary = Midnight.Void,
    secondaryContainer = Midnight.Surface3,
    onSecondaryContainer = Midnight.Cream,
    tertiary = Midnight.Ice,
    onTertiary = Midnight.Void,
    tertiaryContainer = Midnight.IceContainer,
    onTertiaryContainer = Color(0xFFC9D7EC),
    background = Midnight.Void,
    onBackground = Midnight.Cream,
    surface = Midnight.Void,
    onSurface = Midnight.Cream,
    surfaceVariant = Midnight.Surface2,
    onSurfaceVariant = Midnight.CreamMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = Midnight.Cream,
    inverseOnSurface = Midnight.Void,
    error = Midnight.Frost,
    onError = Midnight.Void,
    errorContainer = Color(0xFF2A1F27),
    onErrorContainer = Midnight.Frost,
    outline = Midnight.HairlineStrong,
    outlineVariant = Midnight.Hairline,
    scrim = Color(0xFF000000),
    surfaceBright = Midnight.Surface3,
    surfaceDim = Midnight.Void,
    surfaceContainerLowest = Midnight.Void,
    surfaceContainerLow = Midnight.Surface1,
    surfaceContainer = Midnight.Surface1,
    surfaceContainerHigh = Midnight.Surface2,
    surfaceContainerHighest = Midnight.Surface3,
)

private val Serif = StacksFonts.Serif
private val Sans = StacksFonts.Sans

private val StacksTypography = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Serif, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-0.3).sp),
    displaySmall = TextStyle(fontFamily = Serif, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-0.2).sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontSize = 30.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontSize = 26.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontSize = 23.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontSize = 20.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = StacksFonts.Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = StacksFonts.Mono, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.sp),
)

private val StacksShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// no light theme, the app is dark on purpose
@Composable
fun StacksTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = StacksTypography,
        shapes = StacksShapes,
        content = content,
    )
}
