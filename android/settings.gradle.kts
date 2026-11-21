pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        // 真实 rPPG SDK AAR 本地仓库（AGP 不允许 library module 用 files() 引用本地 .aar，
        // flatDir 方式让 AAR 作为普通依赖引入，避免 lint bundle 报错）
        flatDir {
            dirs("${rootProject.projectDir}/facephys-api/libs")
        }
    }
}

rootProject.name = "HerRhythm"
include(":app")

// FacePhys 端侧感知 SDK 的 API 契约桩模块。
// 真实 AAR 到手后：删掉此 module，把 app 的 implementation(project(":facephys-api"))
// 换成 implementation(files("libs/facephys-v1.5.0.aar"))，并只需调整
// sensor/FacePhysBridge.kt 一个文件（该文件是唯一 SDK 接触点）。
include(":facephys-api")
