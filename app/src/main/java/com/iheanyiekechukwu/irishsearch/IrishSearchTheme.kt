package com.iheanyiekechukwu.irishsearch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Ink-blue accents and neutral surface tones keep catalog information in the foreground.
// Every colored container has a matching "on" role, including dark theme controls.
private val LightColors = lightColorScheme(
    primary = Color(0xFF29475F), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE9F0), onPrimaryContainer = Color(0xFF193449),
    secondary = Color(0xFF526774), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EBEF), onSecondaryContainer = Color(0xFF263E4B),
    tertiary = Color(0xFF706447), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE6D6), onTertiaryContainer = Color(0xFF4C422C),
    background = Color(0xFFF8FAFB), onBackground = Color(0xFF1A2024),
    surface = Color(0xFFF8FAFB), onSurface = Color(0xFF1A2024),
    surfaceVariant = Color(0xFFE7ECEF), onSurfaceVariant = Color(0xFF58636B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F5F7),
    surfaceContainer = Color(0xFFECF1F4),
    surfaceContainerHigh = Color(0xFFE5ECEF),
    surfaceContainerHighest = Color(0xFFDDE5E9),
    surfaceDim = Color(0xFFDCE3E7), surfaceBright = Color(0xFFF8FAFB),
    outline = Color(0xFF77848D), outlineVariant = Color(0xFFCED8DE),
    inverseSurface = Color(0xFF2B3338), inverseOnSurface = Color(0xFFEFF3F5),
    inversePrimary = Color(0xFFB3CBDF), surfaceTint = Color(0xFF29475F),
    error = Color(0xFFAA3C35), onError = Color.White,
    errorContainer = Color(0xFFFFDAD5), onErrorContainer = Color(0xFF6F211B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3CBDF), onPrimary = Color(0xFF143145),
    primaryContainer = Color(0xFF294254), onPrimaryContainer = Color(0xFFDDE9F0),
    secondary = Color(0xFFB6C9D5), onSecondary = Color(0xFF233A47),
    secondaryContainer = Color(0xFF303E47), onSecondaryContainer = Color(0xFFDCE7EE),
    tertiary = Color(0xFFD2C6A9), onTertiary = Color(0xFF39301D),
    tertiaryContainer = Color(0xFF4C432D), onTertiaryContainer = Color(0xFFEDE6D6),
    background = Color(0xFF121719), onBackground = Color(0xFFE1E7EB),
    surface = Color(0xFF121719), onSurface = Color(0xFFE1E7EB),
    surfaceVariant = Color(0xFF354047), onSurfaceVariant = Color(0xFFBAC6CD),
    surfaceContainerLowest = Color(0xFF0D1214),
    surfaceContainerLow = Color(0xFF1A2023),
    surfaceContainer = Color(0xFF20272B),
    surfaceContainerHigh = Color(0xFF283034),
    surfaceContainerHighest = Color(0xFF333C41),
    surfaceDim = Color(0xFF121719), surfaceBright = Color(0xFF374146),
    outline = Color(0xFF84949E), outlineVariant = Color(0xFF3D4A52),
    inverseSurface = Color(0xFFE1E7EB), inverseOnSurface = Color(0xFF2B3338),
    inversePrimary = Color(0xFF29475F), surfaceTint = Color(0xFFB3CBDF),
    error = Color(0xFFFFB4AA), onError = Color(0xFF680F0C),
    errorContainer = Color(0xFF852A23), onErrorContainer = Color(0xFFFFDAD5),
)

internal object AppSpacing {
    val Detail = 4.dp
    val Inline = 8.dp
    val Row = 12.dp
    val Group = 16.dp
    val Page = 16.dp
    val Section = 24.dp
    val Expanded = 32.dp
}

@Composable
internal fun IrishSearchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
