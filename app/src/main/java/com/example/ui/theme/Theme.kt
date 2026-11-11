package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = MaxBurgundyLight,
    onPrimary = Color.White,
    primaryContainer = MaxBurgundyDark,
    onPrimaryContainer = MaxGoldLight,
    secondary = MaxGold,
    onSecondary = Color.Black,
    secondaryContainer = MaxBurgundy,
    onSecondaryContainer = Color.White,
    tertiary = MaxBronze,
    background = MaxDarkBackground,
    onBackground = MaxTextPrimary,
    surface = MaxDarkSurface,
    onSurface = MaxTextPrimary,
    surfaceVariant = MaxDarkCard,
    onSurfaceVariant = MaxTextSecondary,
    outline = MaxBurgundyLight.copy(alpha = 0.5f)
  )

private val LightColorScheme = DarkColorScheme // Default to premium dark brand aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}

