import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 热更新独立模块：整包更新 + 远程配置（update 包）+ DEX 热修运行时（hotfix 包）。
// 源码自 https://github.com/hdppppppp/hot-update v1.0.0 引入（GPL-3.0，见本目录 LICENSE）。
//
// 两条不能破的约定（与上游一致）：
// 1. 包名保持 com.taotao.music.update / com.taotao.music.hotfix 不动 ——
//    插桩白名单（build-logic）、R8 keep（consumer-rules）与补丁方法键
//    （「类名#方法名#签名」）都以这两个包前缀为准，改名即全体系失配。
// 2. 模块不依赖 app 的任何类型（TokenStorage、ServiceLocator 等）：
//    宿主通过 UpdateHost 接口注入能力，依赖方向永远是 app -> 模块。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // UpdateManager 用 Compose 状态暴露检查结果；经 compose 插件取 runtime，
    // 与 :shared 的解析方式一致（版本由 libs.versions.toml 的 compose 决定）。
    alias(libs.plugins.compose)
}

android {
    namespace = "com.taotao.music.hotupdate"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 24
        // 模块自身的 keep 规则随 AAR 下发；-dontobfuscate / -dontoptimize 由
        // androidApp/proguard-rules.pro 统一声明（当前 release 关闭 R8，规则为前瞻保留）。
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(compose.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
