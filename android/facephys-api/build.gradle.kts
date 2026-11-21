plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.facephys.rppg"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // ── 真实 RppgEngineImpl AAR ────────────────────────────────────────────
    // AGP 不允许 library module 用 files() 引用本地 .aar（lint bundle 会报错），
    // 改用 settings.gradle.kts 里声明的 flatDir 本地仓库。
    // AAR 的 classes.jar 不含第三方依赖，以下 4 个必须在此声明：
    // 缺少 okhttp / security-crypto 时初始化会直接 NoClassDefFoundError（见接入教程第一步）。
    implementation(":rppg-sdk-v1.5.0:@aar")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core-ktx:1.12.0")

    testImplementation("junit:junit:4.13.2")
}