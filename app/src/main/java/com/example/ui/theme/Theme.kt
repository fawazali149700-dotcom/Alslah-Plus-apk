package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NoorDarkColorScheme = darkColorScheme(
    primary = EmeraldLuminous,
    onPrimary = Color(0xFF04261C),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = TextCream,
    secondary = GoldLuminous,
    onSecondary = Color(0xFF2B2004),
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldLuminous,
    tertiary = SkySlateDark,
    onTertiary = MidnightNavy,
    background = DarkBackground,
    onBackground = TextCream,
    surface = DarkSurface,
    onSurface = TextCream,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMutedDark,
    outline = Color(0xFF2B4652)
)

private val NoorLightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldDeep,
    secondary = GoldRich,
    onSecondary = Color.White,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = Color(0xFF473300),
    tertiary = MidnightNavy,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = InkDark,
    surface = IvorySurface,
    onSurface = InkDark,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFD5CEBC)
)

val NoorShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NoorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NoorDarkColorScheme else NoorLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NoorShapes,
        content = content
    )
}
