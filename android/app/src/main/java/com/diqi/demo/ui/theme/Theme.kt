package com.diqi.demo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HerRhythmColors = lightColorScheme(
    primary = Color(0xFF0D746F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF14958A),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF141F1C),
    onSurface = Color(0xFF141F1C),
)

@Composable
fun HerRhythmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HerRhythmColors, content = content)
}
