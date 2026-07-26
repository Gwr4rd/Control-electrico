package com.gerar.controlelectrico.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF229ED9),
    onPrimary = Color.White,
    secondary = Color(0xFF31A8E0),
    onSecondary = Color.White,
    tertiary = Color(0xFF65B741),
    onTertiary = Color.White,
    background = Color(0xFFF4F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7F3FB),
    outline = Color(0xFFB7CBD9),
    onBackground = Color(0xFF17212B),
    onSurface = Color(0xFF17212B),
    onSurfaceVariant = Color(0xFF536471)
)

@Composable
fun ControlElectricoTheme(
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (amoled) AmoledColors else LightColors,
        typography = AppTypography,
        content = content
    )
}

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.sp
    )
)

private val AmoledColors = darkColorScheme(
    primary = Color(0xFF2AABEE),
    onPrimary = Color.Black,
    secondary = Color(0xFF55C7E8),
    onSecondary = Color.Black,
    tertiary = Color(0xFF65B741),
    onTertiary = Color.Black,
    background = Color.Black,
    surface = Color(0xFF080808),
    surfaceVariant = Color(0xFF171717),
    outline = Color(0xFF5B6870),
    onBackground = Color(0xFFF4F4F4),
    onSurface = Color(0xFFF4F4F4),
    onSurfaceVariant = Color(0xFFCFCFCF)
)
