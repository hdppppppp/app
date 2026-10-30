import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose)
    id("com.tencent.kuikly-open.kuikly")
}

val kuiklyVersion = property("kuiklyVersion").toString()

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.tencent.kuikly-open:core:$kuiklyVersion")
            implementation("com.tencent.kuikly-open:core-annotations:$kuiklyVersion")
            implementation("com.tencent.kuikly-open:compose:$kuiklyVersion")
            implementation(compose.runtime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}

dependencies {
    // Kuikly 通过 KSP 生成 KuiklyCoreEntry，负责注册所有 @Page 页面。
    add("kspAndroid", "com.tencent.kuikly-open:core-ksp:$kuiklyVersion")
}

android {
    namespace = "com.tideengine.mobile.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
