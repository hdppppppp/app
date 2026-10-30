plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val kuiklyVersion = property("kuiklyVersion").toString()
val apiBaseUrl = providers.gradleProperty("tideApiBaseUrl").get()

android {
    namespace = "com.tideengine.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tideengine.mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            // 骨架阶段先关闭 R8，避免 Kuikly KSP 生成类被裁掉；确认稳定后再开启。
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":nativeBridge"))

    implementation("com.tencent.kuikly-open:core-render-android:$kuiklyVersion")
    implementation("com.tencent.kuikly-open:core:$kuiklyVersion")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.webkit)
    implementation(libs.kotlinx.coroutines.android)
}
