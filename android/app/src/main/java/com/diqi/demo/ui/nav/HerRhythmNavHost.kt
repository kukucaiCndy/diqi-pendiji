package com.diqi.demo.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.diqi.demo.ui.UiTag
import com.diqi.demo.ui.components.HrSecondaryButton
import com.diqi.demo.ui.rememberContainer
import com.diqi.demo.ui.rememberContainerViewModel
import com.diqi.demo.ui.screens.dev.DesignSystemScreen
import com.diqi.demo.ui.screens.home.TrainingHomeScreen
import com.diqi.demo.ui.screens.training.TrainingScreen
import com.diqi.demo.ui.screens.onboarding.ScreeningScreen
import com.diqi.demo.ui.screens.onboarding.WelcomeScreen
import com.diqi.demo.ui.screens.placeholder.PlaceholderScreen
import com.diqi.demo.ui.theme.HrColor
import com.diqi.demo.ui.theme.HrSize
import com.diqi.demo.ui.theme.HrSpacing
import com.diqi.demo.ui.theme.HrType
import kotlinx.coroutines.flow.StateFlow

/**
 * 应用导航根。
 *
 * ## 三条与"返回栈"有关的设计决定（评审 D13）
 *
 * 1. **训练中页面是顶级路由，不在 Tab 图里**：若把它做成 Tab 的子页，
 *    切到别的 Tab 会让 P04 离开 composition，相机随之 unbind，
 *    回来再重建 —— 既丢信号窗口又容易出相机占用异常（AC-18）。
 * 2. **底栏可见性由当前路由决定**，而不是"训练页自己隐藏"。集中判断
 *    才能保证任何新增页面都默认正确（全屏页要显式加进白名单）。
 * 3. **通知带来的导航指令由 [navCommands] 消费**：Activity 只写，
 *    NavHost 只读并清空，冷热启动走同一条路径。
 */
@Composable
fun HerRhythmNavHost(
    navCommands: StateFlow<NavCommand?>,
    onConsumeCommand: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val container = rememberContainer()
    // 引导流程共用一个 ViewModel 实例（跨 welcome/screening 两页）
    val onboardingVm = rememberContainerViewModel { com.diqi.demo.ui.screens.onboarding.OnboardingViewModel(it) }

    // 首屏：筛查做完了就直接进首页，否则先走引导（AC-20 / FR-01）
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val settings = container.settingsStore.current()
        startRoute = if (settings.safetyAcknowledgedAt != null) Routes.TRAINING_HOME else Routes.ONBOARDING_WELCOME
    }

    val command by navCommands.collectAsState()
    LaunchedEffect(command) {
        when (command) {
            NavCommand.OPEN_TRAINING -> {
                // 通知点进来：默认走「不用摄像头」的路径，不让用户卡在权限门上
                navController.navigate(Routes.trainingLive(camera = false)) { launchSingleTop = true }
                onConsumeCommand()
            }
            NavCommand.OPEN_HOME -> {
                navController.navigate(Routes.TRAINING_HOME) { launchSingleTop = true; popUpTo(Routes.TRAINING_HOME) }
                onConsumeCommand()
            }
            null -> Unit
        }
    }

    val entry = startRoute ?: return

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = entry,
                modifier = modifier.fillMaxSize(),
            ) {
                // ── 引导 ─────────────────────────────────────────────────
                composable(Routes.ONBOARDING_WELCOME) {
                    WelcomeScreen(
                        vm = onboardingVm,
                        onStart = { navController.navigate(Routes.ONBOARDING_SCREENING) },
                    )
                }
                composable(Routes.ONBOARDING_SCREENING) {
                    ScreeningScreen(
                        vm = onboardingVm,
                        onFinished = {
                            navController.navigate(Routes.TRAINING_HOME) {
                                popUpTo(Routes.ONBOARDING_WELCOME) { inclusive = true }
                            }
                        },
                        onBackFromFirst = { navController.popBackStack() },
                    )
                }

                // ── 主 Tab ───────────────────────────────────────────────
                // 合并后的「训练」Tab：两条路径都在这一页上，不做二级跳转
                composable(Routes.TRAINING_HOME) {
                    TrainingHomeScreen(
                        onStartWithoutCamera = {
                            navController.navigate(Routes.trainingLive(camera = false))
                        },
                        onStartWithCamera = { navController.navigate(Routes.CALIBRATE) },
                    )
                }
                composable(Routes.RECORDS) {
                    com.diqi.demo.ui.screens.records.RecordsScreen(
                        onOpenWeekly = { navController.navigate(Routes.WEEKLY_REPORT) },
                        onOpenLeak = { navController.navigate(Routes.LEAK_DIARY) },
                    )
                }
                composable(Routes.PROFILE) {
                    com.diqi.demo.ui.screens.settings.SettingsScreen(
                        onOpenDesignSystem = { navController.navigate(Routes.DEV_DESIGN) },
                    )
                }

                /**
                 * 设计系统与语音自检页（仅 debug 可达）。
                 *
                 * 保留它在导航里，是因为语音素材是唯一无法靠文字检索验收的通道，
                 * 每次改动后都需要能一键回到这个页面点在真机上听。
                 */
                composable(Routes.DEV_DESIGN) {
                    DesignSystemScreen()
                }

                // ── 全屏路由（底部栏不显示）───────────────────────────────
                composable(
                    route = Routes.TRAINING_LIVE,
                    arguments = listOf(
                        androidx.navigation.navArgument("camera") {
                            type = androidx.navigation.NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { liveEntry ->
                    TrainingScreen(
                        cameraEnabled = liveEntry.arguments?.getBoolean("camera") ?: false,
                        onFinished = { sessionId ->
                            val route = if (sessionId != null) {
                                "${Routes.COMPLETE}?sessionId=$sessionId"
                            } else {
                                Routes.COMPLETE
                            }
                            navController.navigate(route) {
                                // 训练页不留在返回栈里：返回键不应回到"已结束的训练"
                                popUpTo(Routes.TRAINING_LIVE) { inclusive = true }
                            }
                        },
                        onExit = { navController.popBackStack() },
                    )
                }
                composable(Routes.CALIBRATE) {
                    com.diqi.demo.ui.screens.calibrate.CalibrateScreen(
                        // 校准成功 → 带摄像头参数进训练（camera=true）
                        onStartWithSensor = {
                            navController.navigate(Routes.trainingLive(camera = true)) {
                                popUpTo(Routes.CALIBRATE) { inclusive = true }
                            }
                        },
                        // 「跳过，直接训练」→ 不带摄像头进训练（camera=false）。
                        // 首版两处都是 popBackStack()，于是点「开始训练」会退回
                        // 训练 Tab 而不是进训练页 —— 合并 IA 之后这条路径必须显式导航。
                        onSkip = {
                            navController.navigate(Routes.trainingLive(camera = false)) {
                                popUpTo(Routes.CALIBRATE) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = Routes.COMPLETE + "?sessionId={sessionId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("sessionId") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    com.diqi.demo.ui.screens.complete.CompleteScreen(
                        sessionId = entry.arguments?.getString("sessionId"),
                        onDone = {
                            navController.navigate(Routes.TRAINING_HOME) {
                                popUpTo(Routes.TRAINING_HOME) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.WEEKLY_REPORT) {
                    com.diqi.demo.ui.screens.weekly.WeeklyReportScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.LEAK_DIARY) {
                    com.diqi.demo.ui.screens.records.LeakDiaryScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        if (currentRoute in Routes.TAB_ROUTES) {
            AppBottomBar(
                currentRoute = currentRoute,
                onSelect = { tab ->
                    // 点当前 Tab 不应重建页面：首版直接 navigate 会先把当前 Tab
                    // popUpTo 掉、使 launchSingleTop 失效，再 push 一个新 entry ——
                    // 对训练 Tab 等于每次重拿相机，与 D13 的意图相反。
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(Routes.TRAINING_HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}

/**
 * 底部 4 Tab。
 *
 * 每个 Tab 的**可点区域是整个格子**（≥56dp 高），而不是只有图标与文字 ——
 * 这是适老化的基本要求（FR-15 热区 ≥44dp）。
 */
@Composable
private fun AppBottomBar(
    currentRoute: String?,
    onSelect: (BottomTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HrColor.Surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .heightIn(min = 64.dp)
            .testTag(UiTag.BOTTOM_BAR),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            val tint = if (selected) HrColor.Brand else HrColor.WeakText
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = HrSize.minTouchTarget)
                    .clickable(role = Role.Tab, onClickLabel = tab.label) { onSelect(tab) }
                    .testTag(tab.tag)
                    .padding(vertical = HrSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
                Text(tab.label, style = HrType.micro, color = tint)
            }
        }
    }
}

private val BottomTab.icon: ImageVector
    get() = when (this) {
        BottomTab.TRAINING -> Icons.Filled.PlayArrow
        BottomTab.RECORDS -> Icons.Filled.List
        BottomTab.PROFILE -> Icons.Filled.Person
    }
