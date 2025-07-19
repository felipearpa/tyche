package com.felipearpa.tyche.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val lightColorScheme = lightColorScheme(
    primary = lightPrimaryColor,
    onPrimary = lightOnPrimaryColor,
    primaryContainer = lightPrimaryContainer,
    onPrimaryContainer = lightOnPrimaryContainer,
    secondary = lightSecondaryColor,
    onSecondary = lightOnSecondaryColor,
    secondaryContainer = lightSecondaryContainer,
    onSecondaryContainer = lightOnSecondaryContainer,
    tertiary = lightTertiaryColor,
    onTertiary = lightOnTertiaryColor,
    tertiaryContainer = lightTertiaryContainer,
    onTertiaryContainer = lightOnTertiaryContainer,
    background = lightBackgroundColor,
    onBackground = lightOnBackgroundColor,
    surface = lightSurfaceColor,
    onSurface = lightOnSurfaceColor,
    surfaceVariant = lightSurfaceVariantColor,
    onSurfaceVariant = lightOnSurfaceVariantColor,
    error = lightErrorColor,
    onError = lightOnErrorColor,
    errorContainer = lightErrorContainer,
    onErrorContainer = lightOnErrorContainer,
)

private val darkColorScheme = darkColorScheme(
    primary = darkPrimaryColor,
    onPrimary = darkOnPrimaryColor,
    primaryContainer = darkPrimaryContainer,
    onPrimaryContainer = darkOnPrimaryContainer,
    secondary = darkSecondaryColor,
    onSecondary = darkOnSecondaryColor,
    secondaryContainer = darkSecondaryContainer,
    onSecondaryContainer = darkOnSecondaryContainer,
    tertiary = darkTertiaryColor,
    onTertiary = darkOnTertiaryColor,
    tertiaryContainer = darkTertiaryContainer,
    onTertiaryContainer = darkOnTertiaryContainer,
    background = darkBackgroundColor,
    onBackground = darkOnBackgroundColor,
    surface = darkSurfaceColor,
    onSurface = darkOnSurfaceColor,
    surfaceVariant = darkSurfaceVariantColor,
    onSurfaceVariant = darkOnSurfaceVariantColor,
    error = darkErrorColor,
    onError = darkOnErrorColor,
    errorContainer = darkErrorContainer,
    onErrorContainer = darkOnErrorContainer,
)

@Composable
fun TycheTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val extendedColors = if (darkTheme) darkExtendedColorScheme else lightExtendedColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalExtendedColorScheme provides extendedColors,
            LocalBoxSpacing provides BoxSpacing(),
        ) {
            content()
        }
    }
}
