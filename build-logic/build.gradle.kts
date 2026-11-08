plugins {
    `kotlin-dsl`
}

// 热修复插桩插件（com.taotao.hotfix）。源码自 https://github.com/hdppppppp/hot-update v1.0.0
// 的 build-logic 引入（GPL-3.0），仅去掉发布配置并把 AGP 依赖对齐本仓库（9.3.1）。
repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // AGP 的 Instrumentation API（AsmClassVisitorFactory）在这个构件里。
    // 版本必须与根工程 gradle/libs.versions.toml 的 agp 一致，否则 API 可能失配。
    compileOnly("com.android.tools.build:gradle:9.3.1")
    // ASM 用来改字节码。AGP 9 内部使用 ASM 9.x，这里对齐大版本避免 ClassReader 版本报错。
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
}

gradlePlugin {
    plugins {
        create("hotfixInstrumentation") {
            id = "com.taotao.hotfix"
            implementationClass = "com.taotao.hotfix.HotfixInstrumentationPlugin"
        }
    }
}
