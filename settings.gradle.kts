pluginManagement {
    // Kuikly Gradle 插件没有发布 plugin marker，按官方指引重定向到 core-gradle-plugin。
    // 版本与 gradle.properties 的 kuiklyVersion 保持一致。
    val kuiklyVersion = providers.gradleProperty("kuiklyVersion").get()
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.tencent.kuikly-open.kuikly") {
                useModule("com.tencent.kuikly-open:core-gradle-plugin:$kuiklyVersion")
            }
        }
    }

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
    }
}

rootProject.name = "tide-engine-mobile"

include(":shared")
include(":nativeBridge")
include(":androidApp")
