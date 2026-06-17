package jobs.procrush.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass(),
    content: @Composable () -> Unit,
) {
    val maxWidth =
        when (windowSizeClass) {
            WindowSizeClass.Compact -> 480.dp
            WindowSizeClass.Medium -> 720.dp
            WindowSizeClass.Expanded -> 960.dp
        }
    val horizontalPadding =
        when (windowSizeClass) {
            WindowSizeClass.Compact -> 20.dp
            WindowSizeClass.Medium -> 32.dp
            WindowSizeClass.Expanded -> 48.dp
        }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = maxWidth)
                    .padding(horizontal = horizontalPadding, vertical = 24.dp),
        ) {
            content()
        }
    }
}
