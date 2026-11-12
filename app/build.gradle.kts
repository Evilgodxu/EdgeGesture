import com.android.build.api.variant.HasUnitTestBuilder
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.edgegesture.evilgodxu"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.edgegesture.evilgodxu"
        minSdk = 34
        targetSdk = 37
        versionCode = 23
        versionName = "5.3.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../jh.keystore")
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("KEY_ALIAS", "jh")
            keyPassword = localProperties.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            // AGP 9.3+ 的统一优化入口：代码优化、优化型资源缩减与平台默认 keep 规则一并开启，
            // 默认规则已等价于 proguard-android-optimize.txt，无需再单独声明
            optimization {
                enable = true
            }
            isCrunchPngs = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    androidResources {
        // 保留打包语言，支持应用内语言切换
        localeFilters += listOf("zh", "en")
        // 由 AGP 依据资源目录生成 locale_config 并写入清单，避免手写文件与资源漂移
        generateLocaleConfig = true
    }

    // 语言资源不分包，确保运行时语言切换可用
    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        jniLibs {
            // 不压缩原生库，确保可以 mmap 加载，安装后体积最小
            useLegacyPackaging = false
        }
        dex {
            // 定向压缩 DEX，减小分发体积
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                // Kotlin 模块元数据文件（每个 Kotlin 库都会生成，必然重复）
                "META-INF/*.kotlin_module",
                // Kotlin 协程调试探针
                "META-INF/DebugProbesKt.bin",
                "DebugProbesKt.bin",
                // 常见的重复许可证文件
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                // 版本控制索引文件
                "META-INF/INDEX.LIST",
                // 第三方库签名文件
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                // AndroidX 版本属性文件（各库独立包含，无需打包）
                "META-INF/*.version",
                "META-INF/androidx/**",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        aidl = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    testOptions {
        unitTests {
            // Compose Preview 与资源解析任务依赖编译后的 Android 资源
            isIncludeAndroidResources = true
        }
    }

    lint {
        // 质量门禁：Lint Error 阻断构建，release 变体同规则
        abortOnError = true
        checkReleaseBuilds = true
    }
}

// 构建产物统一命名为 EdgeGesture-<versionName>-arm64.apk
val apkVersionName = android.defaultConfig.versionName ?: "0.0.0"

kotlin {
    compilerOptions {
        // Kotlin 编译警告提级为 Error，弃用 API 告警源头清零
        allWarningsAsErrors.set(true)
    }
}

androidComponents {
    // AGP 9.5 会为开启 compose 的每个变体注册 Compose Preview 任务，变体未启用单元测试时创建即失败，
    // 使 tasks 与 IDE 同步中断；该开关在 VariantBuilder 上已隐藏，需经 HasUnitTestBuilder 设置
    beforeVariants(selector().all()) { variant ->
        (variant as HasUnitTestBuilder).enableUnitTest = true
    }
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("EdgeGesture-$apkVersionName-arm64.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.window)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Hidden API Bypass（用于调用 setLaunchWindowingMode 等小窗隐藏 API）
    implementation(libs.hidden.api.bypass)

    // 应用更新网络请求
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
