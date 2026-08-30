package android.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Design Tokens
val BrandGradientStart = Color(0xFF7B61FF)
val BrandGradientEnd = Color(0xFF4F46E5)
val AppBackground = Color(0xFFF6F5FB)
val AppSurface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF22213A)
val TextSecondary = Color(0xFF8B8B9E)

val PriorityLowBg = Color(0xFFE3F9E5)
val PriorityLowText = Color(0xFF22A447)
val PriorityMediumBg = Color(0xFFFFF3D6)
val PriorityMediumText = Color(0xFFE0932C)
val PriorityHighBg = Color(0xFFFDE2E4)
val PriorityHighText = Color(0xFFE0455A)

val AccentBlue = Color(0xFF4F63E0)

val BrandGradient = Brush.linearGradient(
    colors = listOf(BrandGradientStart, BrandGradientEnd)
)
