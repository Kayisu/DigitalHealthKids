package com.example.digitalhealthkids.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Şimdilik sadece Light Theme odaklı gidiyoruz (MVP için en temiz çözüm)
private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimaryBlue.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryBlue,

    secondary = SecondaryTeal,
    onSecondary = SurfaceWhite,

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed
)

@Composable
fun DigitalHealthKidsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Koyu mod olsa bile şimdilik Light tasarım kullanıyoruz ki tasarım tutarlı olsun
    val colorScheme = LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Status bar arka plan rengiyle bütünleşsin
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Type.kt dosyan standart kalabilir
        content = content
    )
}