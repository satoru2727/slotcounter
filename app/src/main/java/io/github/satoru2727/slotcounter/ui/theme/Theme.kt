package io.github.satoru2727.slotcounter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Always dark: this app is used in a hall, often with the brightness turned
 * down, and a white screen would be both glaring and a battery drain.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8B33C),
    onPrimary = Color(0xFF2A1F00),
    primaryContainer = Color(0xFF3B2E0B),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFF7FD7CB),
    onSecondary = Color(0xFF00322E),
    background = Color(0xFF12151C),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF1A1F29),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF262C38),
    onSurfaceVariant = Color(0xFFB6BDCB),
    outline = Color(0xFF3C4453),
    error = Color(0xFFFF6B6B),
)

val CounterPalette: List<Color> = listOf(
    Color(0xFFE8B33C),
    Color(0xFF7FD7CB),
    Color(0xFFFF8A65),
    Color(0xFFB39DDB),
    Color(0xFF64B5F6),
    Color(0xFFAED581),
)

fun counterColor(index: Int): Color = CounterPalette[Math.floorMod(index, CounterPalette.size)]

@Composable
fun SlotCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
