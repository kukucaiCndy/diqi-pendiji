package com.diqi.demo

import android.app.Application

class HerRhythmApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        @Volatile
        private var instance: HerRhythmApp? = null

        /** 供 foreground service 等无法注入的入口取容器。 */
        fun container(): AppContainer =
            requireNotNull(instance) { "HerRhythmApp 尚未初始化" }.container
    }
}