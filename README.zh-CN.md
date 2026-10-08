# ARMCP（Android Root MCP）

[English](README.md) · [简体中文](README.zh-CN.md)

这是一个 Flutter Android 应用，APK 直接提供经过身份验证的 MCP 服务及同源调试页面。ARMCP 是 Android Root MCP 面向操作人员的产品名称，Android 应用 ID 仍为 `com.doublecat.android_root_mcp`。当前实现包括 Flutter 到 Kotlin 的状态通道、运行时 Root 探测与用户触发的授权尝试、需风险确认的 Root 权限操作、持久化 MCP 总开关、可见前台服务、本地网络 MCP 服务、令牌生命周期原型和 `/debug/` 页面。

## 入口与能力

- Flutter 界面：`src/lib/main.dart`
- Flutter 原生客户端：`src/lib/platform/android_capability_client.dart`
- Android 宿主：`src/android/app/src/main/kotlin/com/doublecat/android_root_mcp/MainActivity.kt`
- 平台通道：`com.doublecat.android_root_mcp/capabilities`
- 原生方法包括 `getHealth`、`getPermissionStatus`、`getAccessibilityStatus`、`getLocale`、`setLocale`、`grantAllPermissions` 和无障碍设置引导
- Root 方法：`getRootStatus`、`attemptRoot`。后者请求 Root 管理器授权并返回检查结果，不会为设备获取 Root
- 运行状态方法：`getRuntimeMode`、`setRuntimeMode`、`getMcpEnabled`、`setMcpEnabled`、`getAutoGrantPermissions`、`setAutoGrantPermissions`
- MCP 服务：`POST /mcp`、`GET /debug/`，以及设备、应用、输入、Shell、截图、Root UI 检查和 XPath 随机点击工具

## 命令

Flutter 工程位于 `src/`。从仓库根目录通过项目脚本运行：

```bash
bash script/flutter.sh pub get
bash script/flutter.sh analyze
bash script/flutter.sh test
bash script/flutter.sh build apk
bash script/flutter.sh run
```

本地 `flutter build apk` 可以成功。设备运行和发布配置仍可能需要完整 Android SDK 命令行工具及已接受的许可证；使用 `flutter doctor -v` 检查本机环境。

中间文件位于 `build/`。执行 `bash script/build_android_apks.sh` 后，三个 APK 直接写入 `release/`：`armcp-v8.apk`、`armcp-v7.apk` 和 `armcp-universe.apk`。

## 仓库目录

```text
src/       Flutter 工程与 Android 宿主
build/     Flutter/Gradle 中间文件
docs/      产品、协议、运维和研究文档
test/      Flutter widget 与契约测试
script/    Flutter 和发布构建脚本
release/   三个发布 APK，直接位于此目录
```

## 架构边界

```text
Flutter 界面与状态
        -> 类型化平台通道
Kotlin Android 能力层
        -> APK 内直接运行的 MCP Streamable HTTP 服务
        -> 同源 schema 驱动的 /debug/ 页面
```

APK 直接提供 MCP。启用总开关后，监听器绑定本地网络接口的 `8787` 端口；所有 MCP POST 请求都必须携带 Bearer 令牌。文档导航见 [`docs/README.md`](docs/README.md)（[中文](docs/README.zh-CN.md)），实施阶段见 [`docs/Roadmap.md`](docs/Roadmap.md)（[中文](docs/Roadmap.zh-CN.md)）。
