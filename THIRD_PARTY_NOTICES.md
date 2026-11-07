# 第三方组件与许可证

## hot-update（热更新模块与插桩插件）

- 用途：Android 方法级热修复 + 整包更新 / 远程配置。`hotupdate/` 为客户端运行时，
  `build-logic/` 为编译期插桩插件，均按源码引入并做了宿主适配（白名单、JDK 17、AGP 9.3.1）。
- 许可证：GPL-3.0（SPDX：`GPL-3.0-only`）。完整文本见 [`hotupdate/LICENSE`](hotupdate/LICENSE)。
- 上游：<https://github.com/hdppppppp/hot-update>
- 固定版本：tag `v1.0.0`（commit `f1fe7619dcc0b80d363d446de64276f6102806b5`）。
- 本项目改动范围（均为接入适配，不改算法与协议）：
  `build-logic` 去掉 maven-publish 并把 AGP 依赖对齐 9.3.1；插桩白名单改为
  `com/tideengine/mobile/data/`、`com/tideengine/mobile/update/`；排除
  `com/tideengine/mobile/TideApplication`；`hotupdate` 构建脚本改用本仓库版本目录
  与 JDK 17。运行时代码（`com.taotao.music.update` / `com.taotao.music.hotfix`）未改动。

**注意**：GPL-3.0 具有传染性。`hotupdate/` 与 `build-logic/` 的引入意味着本仓库
（hdppppppp/app）整体按 GPL-3.0 分发时必须同时提供对应源码；再分发 APK 时保留
`hotupdate/LICENSE`。

## XopProtector

- 用途：构建期 APK 加固（DEX 加密、RASP、PVM2 真 VMP）；其 shell
  （`libprotector.so`、shell dex）会嵌入加固后的 APK。
- 许可证：Apache License 2.0（SPDX：`Apache-2.0`）。
- 版权：Copyright contributors to the protector project.
- 上游：<https://github.com/xopJack/XopProtector>
- 固定提交：`e8a15269ae8bb404dee7f11958fd1c1090dd79cb`（0.7.9 内容；上游
  `0.7.9` tag 实际指向前一提交，不能按 tag 拉取）。
- 本项目不修改其源码，仅在 CI 中克隆并构建 packer 与 shell；
  `.github/workflows/android.yml` 会把上游 `LICENSE` 与 `NOTICE` 复制到加固产物
  目录（`out/XopProtector-LICENSE.txt`、`out/XopProtector-NOTICE.txt`）。

加固后的 APK 再分发时，必须同时保留 XopProtector 的 `LICENSE` 与 `NOTICE`。
完整许可证文本见上述产物文件或上游仓库。

### 上游声明的内嵌第三方

以下许可证文本随 XopProtector 源码树分发，本仓库不重复收录；路径相对于上游仓库：

- Dobby：`native/src/main/cpp/external/Dobby/LICENSE`
- bhook / bytehook：`native/src/main/cpp/external/bhook/LICENSE`
- Linux Syscall Support (LSS)：`native/src/main/cpp/external/bhook/bytehook/src/main/cpp/third_party/lss/LICENSE`
- nlohmann/json 及其他 vendored headers：见对应源文件头部
