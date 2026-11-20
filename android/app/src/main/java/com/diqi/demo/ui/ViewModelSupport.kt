package com.diqi.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diqi.demo.AppContainer
import com.diqi.demo.HerRhythmApp

/** 从 Compose 里拿应用级容器。 */
@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as HerRhythmApp).container
}

/**
 * 轻量 ViewModel 工厂。
 *
 * 不用 Hilt（D7）：本工程的 ViewModel 依赖只有一个 `AppContainer`，
 * 手写工厂的代码量比引入注解处理器更小，也不会拖慢构建。
 *
 * 注意必须由 Composable 侧把容器**显式传进来** —— `create()` 不是
 * Composable 上下文，在那里调 `rememberContainer()` 编译不过。
 */
inline fun <reified VM : ViewModel> containerFactory(
    container: AppContainer,
    crossinline create: (AppContainer) -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
}

/** 一行式：记住并按需创建 ViewModel。 */
@Composable
inline fun <reified VM : ViewModel> rememberContainerViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = rememberContainer()
    return viewModel(factory = containerFactory(container, create))
}