package jobs.procrush.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val width = LocalWindowInfo.current.containerSize.width.dp
    return when {
        width < 600.dp -> WindowSizeClass.Compact
        width < 840.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}
