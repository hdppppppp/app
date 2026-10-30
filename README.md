# 潮汐引擎 Android 客户端

Kotlin Multiplatform + [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) Compose DSL 的
Android 客户端，复用 `../shared` 的 oRPC 契约与现有 NestJS 后端。

本目录是独立 Git 仓库，不参与上级 npm workspaces，也不影响 `server/` 与 `client/`。

## 目录

```text
mobile/
├─ shared/          # KMP：DTO、Ktor 客户端、Token 存储、Kuikly Compose 页面
├─ nativeBridge/    # 原生多 WebView 游戏容器（Bin 注入、localStorage）
├─ androidApp/      # Android 宿主：Application、KuiklyRenderActivity、适配器
└─ .gitea/workflows/android.yml  # 云端构建 APK
```

## 版本

| 组件 | 版本 |
| --- | --- |
| Kuikly | 2.29.0-2.1.21 |
| Kotlin | 2.1.21 |
| AGP | 8.5.0 |
| Gradle | 8.7 |
| KSP | 2.1.21-2.0.1 |
| JDK | 17 |
| compileSdk / minSdk | 34 / 26 |

Kuikly 版本只在 `gradle.properties` 的 `kuiklyVersion` 定义一次。依赖解析必须包含
腾讯镜像 `https://mirrors.tencent.com/nexus/repository/maven-tencent/`：Maven Central
上的 `com.tencent.kuikly-open` 只更新到 2.4.2。

## 构建

本项目按「不在本地编译」的约定提交，仓库没有提交 `gradlew` 与 wrapper jar。
云端 CI 使用系统 Gradle 8.7 构建：

```bash
gradle :androidApp:assembleDebug
gradle :androidApp:assembleRelease
```

本地如需构建，先安装 JDK 17 与 Android SDK 34，然后生成 wrapper：

```bash
gradle wrapper --gradle-version 8.7
./gradlew :androidApp:assembleDebug
```

### API 地址

默认值在 `gradle.properties`：

```properties
tideApiBaseUrl=https://git.xydaigua.cn
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
- Kuikly 适配器：路由、日志、图片（base64/http/file/assets）、线程、异常

## 未实现 / 待验证

- **未在本地编译过**：首次云端构建可能需要按实际编译错误调整 Kuikly API 或依赖版本。
- 多开管理器目前是单账号单 WebView；Web 端的网格布局、分组、多窗口坐标同步、
  静音/全屏拦截尚未迁移。
- 注册、Gitea 登录、头像上传、任务创建/编辑、登录会话管理未做界面。
- 深色主题、平板/横屏布局、无障碍语义未完善。
- 未编写单元测试；`shared` 的纯函数后续补 Kotlin 测试。
- `androidApp` release 暂时关闭 R8，确认 Kuikly KSP 生成类不被裁剪后再开启。
- Manifest 开启 `usesCleartextTraffic` 以兼容可能的内网 http 服务；生产应改为
  `network_security_config` 白名单。

## 安全

- 不提交 `.env`、token、Bin、LoginBuffer、密码、抓包数据。
- Bin 只在内存中使用，不写入磁盘；`GameManagerActivity` 销毁后即释放。
- Token 以 Keystore 加密存储；Bin 由服务端按账号鉴权后下发。
- 日志不输出 token、Bin、请求体。

## 云端构建

`.gitea/workflows/android.yml` 在 push / PR / 手动触发时构建 debug APK，
并在手动触发时构建 release（未签名）。产物通过 Actions artifact 下载。
