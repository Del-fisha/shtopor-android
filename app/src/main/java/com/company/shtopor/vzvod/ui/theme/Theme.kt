package com.company.shtopor.vzvod.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.company.shtopor.vzvod.R

@OptIn(ExperimentalTextApi::class)
val VzvodSans = FontFamily(
    Font(
        R.font.manrope_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.manrope_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.manrope_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.manrope_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

val VzvodDisplay = FontFamily(
    Font(R.font.source_serif_semibold, weight = FontWeight.SemiBold)
)

val Mist = Color(0xFFE8EEF2)
val MistDeep = Color(0xFFD7E0E8)
val Paper = Color(0xFFF5F7FA)
val Ink = Color(0xFF1E2A32)
val InkMuted = Color(0xFF5A6A75)
val Steel = Color(0xFF3D5A73)
val TealMuted = Color(0xFF4A6B6F)
val Online = Color(0xFF2D6A4F)
val Offline = Color(0xFF9A3412)
val SyncPending = Color(0xFFB45309)
val Danger = Color(0xFF8B3A3A)

private val LightColors = lightColorScheme(
    primary = Steel,
    onPrimary = Color.White,
    primaryContainer = MistDeep,
    onPrimaryContainer = Ink,
    secondary = TealMuted,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E4E5),
    onSecondaryContainer = Ink,
    background = Mist,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = MistDeep,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFF9AA8B3),
    error = Danger,
    onError = Color.White
)

private val VzvodTypography = Typography(
    displayLarge = TextStyle(fontFamily = VzvodDisplay, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = VzvodDisplay, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = VzvodDisplay, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = VzvodSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = VzvodSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = VzvodSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = VzvodSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = VzvodSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = VzvodSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp)
)

@Composable
fun VzvodTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = VzvodTypography,
        content = content
    )
}
