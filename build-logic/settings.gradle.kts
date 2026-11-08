// 插桩插件的独立构建。用 includeBuild 引入，不必发布到任何仓库。
//
// 与上游 hot-update 仓库的差异：上游通过 maven-publish 发布并读取 ../VERSION 回写版本号，
// 本仓库按源码 vendor，无需发布配置，AGP compileOnly 与宿主保持同一版本（9.3.1）。
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "build-logic"
