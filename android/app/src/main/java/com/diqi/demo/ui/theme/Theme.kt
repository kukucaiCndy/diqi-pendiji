package com.diqi.demo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * HerRhythm DS v1.0 主题。
 *
 * 本期只做浅色主题：老年用户在暗环境下长期使用深色界面的可读性未经临床验证，
 * 且设计稿只产出浅色方案。深色面板（P04 训练页）是通过局部 [HrColor.Panel]
 * 实现的深色区块，而非全局深色主题。
 */
private val HerRhythmColorScheme = lightColorScheme(
    primary = HrColor.Brand,
    onPrimary = HrColor.Surface,
    primaryContainer = HrColor.GoodContainer,
    onPrimaryContainer = HrColor.GoodDeep,
    secondary = HrColor.Accent,
    onSecondary = HrColor.OnAccent,
    tertiary = HrColor.BreathingRate,
    background = HrColor.Canvas,
    onBackground = HrColor.Ink,
    surface = HrColor.Surface,
    onSurface = HrColor.Ink,
    surfaceVariant = HrColor.SubtleContainer,
    onSurfaceVariant = HrColor.InkSecondary,
    outline = HrColor.Line,
    outlineVariant = HrColor.Track,
    error = HrColor.Danger,
    onError = HrColor.Surface,
    errorContainer = HrColor.DangerContainer,
    onErrorContainer = HrColor.Danger,
    scrim = HrColor.Ink,
)

@Composable
fun HerRhythmTheme(content: @Composable () -> Unit) {
    // 本期不提供深色主题（见文件头注释），保留 isSystemInDarkTheme 读取点以便后续切换。
    val darkTheme = isSystemInDarkTheme()
    @Suppress("UNUSED_VARIABLE")
    val ignoredDarkForNow = darkTheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = HerRhythmColorScheme,
        typography = HerRhythmTypography,
        content = content,
    )
}