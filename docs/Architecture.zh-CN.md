# 系统架构

[English](Architecture.md) · [简体中文](Architecture.zh-CN.md)

## 系统分层

```text
Flutter 界面与状态
          │ 类型化平台契约
Kotlin Android 能力层
           ├── RootProbe / RootAttempt
           ├── RootPermissionManager
           ├── DeviceBackend
           ├── UiBackend（ARMCP 无障碍服务及明确披露的 Root 回退）
           ├── TalkBackController（按运行方式和权限控制）
           ├── HardwareRandomBackend（通过 Root 使用 /dev/hwrng）
           ├── ShellBackend
           ├── TokenStore
           └── 直接 MCP Streamable HTTP 服务
                  ├── POST /mcp
                  ├── GET /debug/
                  └── 认证元数据
```

APK 自身就是 MCP 服务端。它接受 MCP 请求、执行 Android 能力，并在同一进程提供调试页面。不增加桥接、代理、扩展或第二个 MCP 服务端。

调试页面的 UI 检查器是已返回的 `view_tree`/`view_content` 文档的客户端视图。它在内存中解析 XML 或 JSON，生成元素路径和 XML XPath，绘制可选边界，并提供中心点击操作。它不增加第二个检查后端，也不执行返回结果中的脚本。

Flutter 负责底部导航、设置、语言选择、状态展示、令牌创建/工具开关/删除界面和操作人员操作。Kotlin 负责特权执行、套接字生命周期、认证、令牌验证、工具注册与执行、权限状态检查、共享语言设置和调试资源。

`McpForegroundService` 通过进程级 `McpRuntime` 管理运行中的监听器。`MainActivity` 只通过平台通道控制该运行时，因此 Flutter 页面销毁或退后台不会调用 `McpHttpServer.stop()`。

`random_tap_by_xpath` 复用序列化 UI 快照路径，在 Android 进程中计算 XPath，并在可用时从 Root 提供的 `/dev/hwrng` 读取随机源。硬件随机设备不存在或有界读取失败时，使用 Android `SecureRandom`；不使用 `java.util.Random`、时间戳或固定中心坐标。

自动权限偏好通过平台通道提供。Flutter 在应用打开时读取它；如果 MCP 已启用，或刚启用 MCP，就调用已有的 `RootPermissionManager`。风险确认由 Flutter 开关负责，原生执行保持有界并返回结果。

平台边界提供只读的 APK 声明权限报告供主页展示。它只检查 manifest 中声明的运行时权限，不执行授予操作；无障碍服务单独报告。

Flutter 外壳使用四个底部目的地：主页、运行方式与权限、令牌管理和设置。设置保存运行模式，运行方式与权限页面依据该模式显示内容。

运行模式决定能力策略。两种模式都可在用户明确启用后使用 ARMCP 无障碍服务；非 Root 模式使用节点操作和 `dispatchGesture` 执行支持的输入，Root 模式额外提供有界 Shell 能力。TalkBack 状态单独管理：Root 可以请求安全设置变化，非 Root 打开系统无障碍页面。

Root TalkBack 自动化是临时恢复流程，不是持久启动动作。Root 模式下操作人员开启按需偏好后，失败的 UI 检查先确认截图含有可见像素，再临时启用 TalkBack 并重试。重试结束后通过 `finally` 路径恢复原有无障碍设置。

## 原子组件

| 组件 | 负责 | 不负责 |
| --- | --- | --- |
| `SettingsStore` | 开关、绑定策略、端口 | Root 执行 |
| `RootProbe` | 运行时 Root 检查和安全依据 | 令牌签发 |
| `RootAttempt` | 可见的授权尝试/重新检查 | 隐藏行为或启动链修改 |
| `RootPermissionManager` | 针对本 APK 声明权限的有界 `su`/`pm grant` 尝试 | 授予未声明权限或绕过 Android 策略 |
| `ToolRegistry` | 工具 schema 和能力声明 | 传输认证 |
| `TokenStore` | 令牌验证值、模式、有效期、工具允许列表、计数和物理删除 | 界面呈现 |
| `AuthIssuer` | 访问令牌签发、元数据和校验策略 | Android 工具执行 |
| `McpHttpServer` | HTTP、认证门、JSON-RPC 分发和调试资源 | Root 检查 |
| `AndroidToolExecutor` | 每次一个工具调用 | Flutter widget 状态 |

## 请求流程

1. Flutter 通过类型化原生契约改变全局开关。
2. Kotlin 校验设置、Root 策略、令牌策略和本地网络权限。
3. `McpHttpServer` 接收 `/mcp`，校验 Bearer 令牌，按令牌允许列表过滤工具，并分发一个注册工具。
4. 服务在到达 `AndroidToolExecutor` 前拒绝未列入允许列表的工具；执行器随后检查能力并调用一个后端操作。
5. 结果转换为 MCP 内容和稳定错误码；敏感值从审计日志中脱敏。

## 后端策略

每项能力使用最小且已验证的后端。Root Shell 后端在能力检查通过时支持有界 Shell、应用启动、截图、设备/应用/定位元数据和输入。两种模式下，ARMCP 无障碍服务选择活动窗口，尽力刷新节点，有限重试动态窗口失败，并在持续更新期间保留最近完整快照。非 Root 模式还使用它执行支持的手势和节点文本操作。该后端不创建 UiAutomation 会话，因此可以和设备已有自动化连接共存。服务已连接时，不会仅因为窗口暂时不可读就回退到 Root `uiautomator dump`；该回退只用于服务不可用的情况，并报告注册冲突。层级没有 ARMCP 字节上限。每个节点的屏幕边界附带宽度、高度和整数中心坐标，可直接用于 `tap`。运行时权限和 API 限制以结构化错误返回。

## 调试页面设计

状态、schema 表单、结果区域和有界日志采用 `zen-mcp/debug-frontend/` 的界面模式作为参考。不得复制其浏览器工作区或 Go/WebSocket 桥接架构。Android 服务提供静态 HTML/CSS/JS，页面直接调用同一个经过认证的 `/mcp` 端点。
