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
- 用户头像：我的页显示服务端头像（加载失败回退首字母）、本地选图（系统照片选择器，
  无需存储权限）后居中裁切为 256×256 JPEG 上传 / 恢复默认，与 Web 端能力一致
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
- 注册、Gitea 登录、任务创建/编辑、登录会话管理未做界面。
- 平板/横屏布局、无障碍语义未完善。
- 未编写单元测试；`shared` 的纯函数后续补 Kotlin 测试。
- `androidApp` release 暂时关闭 R8，确认 COUI / Compose 资源不被裁剪后再开启。
- Manifest 开启 `usesCleartextTraffic` 以兼容可能的内网 http 服务；生产应改为
  `network_security_config` 白名单。
- PVM2 加固已接入手动 workflow，canary / business 已在 CI 跑通；尚未真机验证；
  上游 KMP/Ktor 兼容问题未关闭。

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

### 构建缓存

- Gradle 用户目录缓存（依赖、构建缓存）和 Gradle 发行版由
  `gradle/actions/setup-gradle` 自动缓存；PR 上只读，避免缓存污染。
- `build` job 把 debug 版本固定为 `0.1.0-debug`（`VERSION_CODE=1`），并启用
  configuration cache。configuration cache 由 `setup-gradle` 随 Gradle 用户目录
  一起保存/恢复，GitHub 仓库需要配置 Secret `GRADLE_ENCRYPTION_KEY`
  （`openssl rand -base64 16` 生成，必须保持稳定，否则新 runner 无法解密旧条目）。
  构建脚本、`gradle.properties`、`libs.versions.toml` 变化时 Gradle 会自动重建。
- `release` job 的版本号仍随 run number 变化，因此不复用 configuration cache，
  但会复用 Gradle 构建缓存，共享模块的编译结果不必每次重算。
- XopProtector 的 packer jar 与 shell 文件按固定 commit 和工具链版本缓存；
  命中后跳过 NDK/CMake 安装和 XopProtector 的 Gradle 构建，只重新执行加固。
  修改 `XOP_REF`、NDK/CMake/Gradle 版本或缓存内容时，需同步递增 cache key 的
  `-v1` 后缀。

### PVM2 加固（手动）

手动触发时选择 `protect=business`（业务层全量）或 `protect=canary`（仅
`auth/`、`ApiClient`、`ApiException`），`protect-pvm2` job 会在签名 release APK
上执行 [XopProtector](https://github.com/xopJack/XopProtector) 的 DEX 加密、RASP
与 PVM2 真 VMP，产物单独上传（`tide-engine-android-pvm2-*`），不影响普通 release。

- VMP 范围：`auth/`、`api/ApiClient`、`api/ApiException`；`business` 另加 `data/`。
- 排除：`ui/`（Compose 热路径）、`api/dto/`（`@Serializable` serializer）、
  `MobileBootstrap` / `ServiceLocator` / `Application` / `Activity`（启动链与
  Manifest 组件）。
- `risk_profile=baseline` 不检测 root/模拟器；`strict` 开启检测。`rasp_action`
  为 0/1/2 时分别是记录/降级/阻断。
- 上游固定到 commit `e8a1526`（0.7.9 内容）。上游 `0.7.9` tag 实际指向前一提交，
  不要按 tag 拉取。
- 首次建议先跑 `protect=canary`，确认 `PVM2 admission` 的 success 数和
  `size_report` 后再切 `business`。
- 构建机预装的 NDK 27.3 / 29 / 30 会让 `findNdk27Plus` 选中 NDK 30，其未裁剪的
  `libc++_shared.so` 使每个 64 位 ABI 增加约 9MB；workflow 只保留 AGP 默认的
  25.1 和满足 16KB 对齐的 27.2，强制使用 27.2。

2026-11-01 的 CI 结果（`risk_profile=baseline`、`rasp_action=1`）：

- `canary`：PVM2 38/38（100%），按 `size_report` 输入 12.76MB → 输出 15.89MB
  （+3.12MB）。
- `business`：PVM2 85/92（92.4%），按 `size_report` 输入 12.76MB → 输出 15.96MB
  （+3.20MB）；7 个超过 PVM2 体积上限的方法回退为 DEX 加密，未进入 VMP：
  `AuthRepository.login/changePassword/updateAvatar`、
  `GameAccountRepository.updateSwitch`、`OperationLogRepository.list`、
  `TaskRepository.list/executions`。
- 同日在 Android 14（API 34，x86_64）模拟器安装 `business` APK：XopProtector shell
  正常替换 `TideApplication`、解密 3 个 DEX，登录页正常渲染；模拟器 System UI 在
  软件渲染下出现 ANR，未完成登录请求、任务和游戏容器验证。

**许可证**：XopProtector 使用 Apache License 2.0（`Apache-2.0`），其 shell 会嵌入
加固后的 APK。CI 会把上游 `LICENSE` 与 `NOTICE` 复制到产物目录；再分发加固 APK
时必须一并保留。第三方组件与固定提交见
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

已知限制：上游存在 KMP/Ktor 兼容问题（issue #25）；DEX 运行期解密会落盘
`code_cache`（issue #28）；暂不支持 Android 17。上线前必须在真机跑通登录、任务、
游戏容器全流程。

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
