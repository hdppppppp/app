# 潮汐引擎 Android 客户端

Jetpack Compose + [COUI](https://github.com/suqi8/coui) 的 Android 客户端，
对接现有 NestJS 后端的 `/api` 契约。

本目录是独立 Git 仓库，不参与上级 npm workspaces，也不影响 `server/` 与 `client/`。

## 目录

```text
mobile/
├─ shared/          # Android library：DTO、Ktor 客户端、Token 存储、COUI 页面
├─ nativeBridge/    # 原生单 WebView 游戏容器（Bin 注入、localStorage）
├─ androidApp/      # Android 宿主：Application、MainActivity、签名与版本配置
└─ .github/workflows/android.yml # GitHub Actions 云端构建 APK
```

## 版本

| 组件 | 版本 |
| --- | --- |
| Kotlin | 2.4.21 |
| AGP | 9.3.1 |
| Gradle | 9.6.1 |
| COUI | 1.1.0 |
| Compose Multiplatform | 1.11.1 |
| JDK | 17 |
| compileSdk / targetSdk / minSdk | 37 / 34 / 26 |
| Build Tools | 36.0.0 |

COUI 1.1.0 的 Android AAR 声明 `minCompileSdk=37`，因此必须使用 compileSdk 37
与 AGP 9.3.1。AGP 9 默认会启用内置 Kotlin 与新 DSL；本项目在
`gradle.properties` 中设置 `android.builtInKotlin=false`、`android.newDsl=false`，
继续使用 Kotlin Gradle Plugin 2.4.21，才能读取 COUI 的 Kotlin 2.4 元数据。

## 构建

本项目按「不在本地编译」的约定提交，仓库没有提交 `gradlew` 与 wrapper jar。
云端 CI 使用 Gradle 9.6.1 构建：

```bash
gradle :androidApp:assembleDebug
gradle :androidApp:assembleRelease
```

本地如需构建，先安装 JDK 17、Android SDK Platform 37 与 Build Tools 36.0.0，
然后生成 wrapper：

```bash
gradle wrapper --gradle-version 9.6.1
./gradlew :androidApp:assembleDebug
```

### API 地址

默认值在 `gradle.properties`：

```properties
tideApiBaseUrl=http://192.168.31.17:3100
```

构建时通过 `-PtideApiBaseUrl=http://192.168.1.10:3100` 覆盖，会写入
`BuildConfig.API_BASE_URL`。服务端需同时托管 Web 前端的 `/game/game.html`，
多开管理器会加载该页面。

## 已实现

- 登录（用户名/邮箱 + 密码）、Token 使用 Android Keystore AES-256-GCM 加密保存
- 会话恢复：启动读取 `/api/auth/me`，401 自动回到登录页
- 任务列表、启用/停用、立即执行
- 游戏账号列表、设为默认、解绑（二次确认）、账号任务开关
- 操作日志列表
- 修改密码、退出登录
- 单账号游戏容器：拉取 Bin 后注入 WebView localStorage，再加载 `/game/game.html`
- COUI 主题：跟随系统浅色 / 深色，底部导航、卡片、开关、输入框均为 COUI 组件

## 未实现 / 待验证

- **迁移尚未通过云端编译验证**：AGP 9.3.1、Kotlin 2.4.21、COUI 1.1.0 的组合
  需要以 GitHub Actions 首次构建结果为准。
- 多开管理器目前是单账号单 WebView；Web 端的网格布局、分组、多窗口坐标同步、
  静音/全屏拦截尚未迁移。
- 注册、Gitea 登录、头像上传、任务创建/编辑、登录会话管理未做界面。
- 平板/横屏布局、无障碍语义未完善。
- 未编写单元测试；`shared` 的纯函数后续补 Kotlin 测试。
- `androidApp` release 暂时关闭 R8，确认 COUI / Compose 资源不被裁剪后再开启。
- Manifest 开启 `usesCleartextTraffic` 以兼容可能的内网 http 服务；生产应改为
  `network_security_config` 白名单。

## 安全

- 不提交 `.env`、token、Bin、LoginBuffer、密码、抓包数据。
- Bin 只在内存中使用，不写入磁盘；`GameManagerActivity` 销毁后即释放。
- Token 以 Keystore 加密存储；Bin 由服务端按账号鉴权后下发。
- 日志不输出 token、Bin、请求体。

## 云端构建

远程仓库：<https://github.com/hdppppppp/app>

`.github/workflows/android.yml` 在 push / PR / 手动触发时构建 debug APK，
手动触发时额外构建**已签名**的 release APK，并用 `apksigner` 校验签名。
产物通过 Actions artifact 下载（`tide-engine-android-debug` /
`tide-engine-android-release`）。

## 发布签名

release 签名通过环境变量注入，密钥、密码和 keystore 均不进仓库。

GitHub Actions Secrets：

| Secret | 内容 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | PKCS12 keystore 的 base64（单行，无换行） |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 `tide-engine` |
| `ANDROID_KEY_PASSWORD` | 密钥密码（与 keystore 密码相同） |

本地构建签名版时设置同名环境变量后执行：

```bash
export ANDROID_KEYSTORE_PATH=/path/to/tide-engine-release.jks
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=tide-engine
export ANDROID_KEY_PASSWORD=...
gradle :androidApp:assembleRelease
```

未设置这些变量时 release 构建产物为未签名，便于临时验证。当前签名密钥备份在
`C:\Users\kiwi\.android\tide-engine-release.jks`（密码在同目录
`tide-engine-release.pass`），**必须离线备份**：丢失后无法覆盖安装升级。
