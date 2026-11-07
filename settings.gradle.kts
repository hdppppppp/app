pluginManagement {
    // 热修复插桩插件（com.taotao.hotfix）以源码形式放在 build-logic 里，includeBuild 引入。
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tide-engine-mobile"

include(":shared")
include(":nativeBridge")
include(":hotupdate")
include(":androidApp")
