# 第三方组件与许可证

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
