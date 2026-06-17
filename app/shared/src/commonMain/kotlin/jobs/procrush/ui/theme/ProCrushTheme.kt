package jobs.procrush.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF1B6B4A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB8F0D3),
        secondary = Color(0xFF3D5A80),
        background = Color(0xFFF7F9F8),
        surface = Color.White,
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF7FD9A8),
        onPrimary = Color(0xFF003822),
        primaryContainer = Color(0xFF005233),
        secondary = Color(0xFFADC8E6),
        background = Color(0xFF101412),
        surface = Color(0xFF1A211D),
    )

@Composable
fun ProCrushTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
