package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PremiumBlueDark,
    onPrimary = Color.White,
    primaryContainer = PremiumBlueContainerDark,
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = PremiumSlateDark,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PremiumBlueLight,
    onPrimary = Color.White,
    primaryContainer = PremiumBlueContainerLight,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = PremiumSlateLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
  )

@Composable
fun GeoTaskTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) = GeoTaskTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
