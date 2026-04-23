package com.example.lifelogger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color  // ← ADD THIS IMPORT
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Import color definitions
import com.example.lifelogger.ui.theme.LifeLoggerPrimary
import com.example.lifelogger.ui.theme.LifeLoggerSecondary
import com.example.lifelogger.ui.theme.LifeLoggerBackground
import com.example.lifelogger.ui.theme.LifeLoggerSurface
import com.example.lifelogger.ui.theme.LifeLoggerOnPrimary
import com.example.lifelogger.ui.theme.LifeLoggerOnBackground
import com.example.lifelogger.ui.theme.Purple80
import com.example.lifelogger.ui.theme.PurpleGrey80

private val LightColorScheme = lightColorScheme(
    primary = LifeLoggerPrimary,
    secondary = LifeLoggerSecondary,
    background = LifeLoggerBackground,
    surface = LifeLoggerSurface,
    onPrimary = LifeLoggerOnPrimary,
    onBackground = LifeLoggerOnBackground,
    onSurface = LifeLoggerOnBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = LifeLoggerOnPrimary,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9)
)

@Composable
fun LifeLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
