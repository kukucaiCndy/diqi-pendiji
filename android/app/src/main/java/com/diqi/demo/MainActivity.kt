package com.diqi.demo

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.diqi.demo.service.TrainingForegroundService
import com.diqi.demo.ui.nav.HerRhythmNavHost
import com.diqi.demo.ui.nav.NavCommand
import com.diqi.demo.ui.theme.HerRhythmTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /**
     * 跨入口导航指令（D13）。
     *
     * Activity 只负责写入，消费方是 NavHost —— 冷启动与热启动走同一条路径，
     * 不会出现"热启动时指令丢失"或"新建第二个 NavHost"。
     */
    private val navCommands = MutableStateFlow<NavCommand?>(null)

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 页面底色是浅色 canvas，必须显式声明"浅色系统栏"，
        // 否则 enableEdgeToEdge 会按深色处理系统栏图标（白字压浅底 → 不可读）。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        consumeIntent(intent)

        setContent {
            HerRhythmTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        // 决策 D14：让 Compose 的 testTag 出现在 uiautomator dump 的
                        // resource-id 里。实测本机 Compose 节点 resource-id 默认为空，
                        // 不做这一步则全部 E2E 脚本只能靠中文文案定位，
                        // 而 S9 会按文案红线改措辞 —— 改一个字就全线失效。
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HerRhythmNavHost(
                        navCommands = navCommands,
                        onConsumeCommand = { navCommands.value = null },
                    )
                }
            }
        }
    }

    /**
     * `singleTop` 下再次启动本 Activity 会走这里。
     *
     * 必须做两件事，缺一个都会出问题：
     *  1. `setIntent(intent)` —— 不调用的话 `getIntent()` 永远是首次启动的意图，
     *     冷热启动读到不同值时排障成本极高
     *  2. 把意图转成 [NavCommand] 写进 flow，而不是在这里直接操作导航
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(TrainingForegroundService.EXTRA_NAV_TO_TRAINING, false)) {
            navCommands.value = NavCommand.OPEN_TRAINING
        }
    }
}