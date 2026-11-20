plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // KSP 版本必须与 Kotlin 版本严格同前缀（2.0.21-*），否则编译期直接失败
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}