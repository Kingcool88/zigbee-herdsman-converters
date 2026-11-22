package se.zenmode.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ZenBackground = Color(0xFF0F1A1A)
val ZenAccent = Color(0xFF7FD1C3)

private val colors = darkColorScheme(
    primary = ZenAccent,
    onPrimary = Color(0xFF00201B),
    secondary = Color(0xFFB1CCC6),
    background = ZenBackground,
    surface = Color(0xFF162424),
    surfaceVariant = Color(0xFF1F3130),
    onBackground = Color(0xFFDDE4E2),
    onSurface = Color(0xFFDDE4E2),
)

@Composable
fun ZenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
