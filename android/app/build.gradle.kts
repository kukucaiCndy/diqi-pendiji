import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// AppKey 来源：local.properties（已在 .gitignore，不进 Git）→ 注入 BuildConfig。
// 生产环境应改为服务端下发。缺省为空串，缺失时不阻断构建（这样 AAC/NullSensor
// 路径始终可构建、可跑通）。
val facePhysAppKey: String = run {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
    props.getProperty("facephys.appKey") ?: ""
}

android {
    namespace = "com.diqi.demo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.diqi.demo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FACEPHYS_APP_KEY", "\"$facePhysAppKey\"")
    }

    buildTypes {
        debug {
            // debug 下开放造数广播与故障注入入口，release 必须为 false
            buildConfigField("boolean", "ENABLE_DEBUG_HOOKS", "true")
        }
        release {
            buildConfigField("boolean", "ENABLE_DEBUG_HOOKS", "false")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // 让 android.jar 的桩方法返回默认值而不是抛异常。
        // ⚠️ 代价：Bitmap.getPixels 之类会静默返回 0，所以
        // RoiConverter 的 Bitmap 重载**不做** JVM 单测，只测纯函数版本。
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // 自适应图标必须放在 mipmap-anydpi-v26：AAPT 找不到不带 v26 的
        // adaptive-icon 资源（实测报 resource not found），所以这条告警只能抑制。
        // 其余未处理的只有 GradleDependency（版本是刻意锁定的基线）。
        disable += "ObsoleteSdkInt"
        warningsAsErrors = false
        abortOnError = true
    }
}

ksp {
    // 没有 schemaLocation，第一次改表就失去迁移基线，只能
    // fallbackToDestructiveMigration() → 抹掉 AC-12 要验的 3 天历史。
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    // ── Compose ────────────────────────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // ── 导航 ──────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ── 存储 ──────────────────────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── 相机与人脸检测 ─────────────────────────────────────────────────────
    //
    // ⚠️ 关于「是否依赖 GMS」的重要事实（已用 dependencies 树核实，勿再凭印象改注释）：
    //   com.google.mlkit:face-detection:16.1.7 是 **model-bundled** 版
    //   （APK 里确有 libface_detector_v2_jni.so，模型随包，所以无 GMS 服务的设备
    //    也能检出人脸），但它的传递依赖**确实包含**：
    //     · com.google.android.gms:play-services-mlkit-face-detection:17.1.0
    //     · com.google.android.datatransport:transport-{api,runtime,backend-cct}
    //     · com.google.firebase:firebase-encoders-*
    //   这套组合带一个遥测上传通道。需求 §7「仅 License 元数据出网」的静态审计
    //   必须能解释这条链；AC-03「完全离线」的验证也要覆盖到它。
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:face-detection:16.1.7")

    // ── 后台 ──────────────────────────────────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // ── FacePhys 感知 SDK（当前为 API 契约桩，见 settings.gradle.kts 注释）──
    implementation(project(":facephys-api"))

    // ── 测试 ──────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    // ui-test 必须显式声明：ui-test-junit4 只把 ui-test 作为 implementation 依赖，
    // 不显式声明时 onNodeWithTag / assertExists 这些扩展函数在 androidTest
    // 编译期不可见（实测报 Unresolved reference）。
    androidTestImplementation("androidx.compose.ui:ui-test")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}