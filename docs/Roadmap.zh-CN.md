# 实施路线图

[English](Roadmap.md) · [简体中文](Roadmap.zh-CN.md)

每项工作都是包含一个契约和针对性验证的原子阶段。前一阶段存在未解决契约或失败门槛时，不开始后续阶段。

## Slice 0 — 工程脚手架 — 已完成

- 使用 `flutter create --platforms=android --org com.doublecat --project-name android_root_mcp .` 创建。
- Dart 入口：`src/lib/main.dart`；原生入口：`src/android/app/src/main/kotlin/com/doublecat/android_root_mcp/MainActivity.kt`。
- 增加 `com.doublecat.android_root_mcp/capabilities` 通道和 `getHealth` 方法，并用 mock 通道编写 Flutter widget 测试。
- 验证命令：`flutter analyze`、`flutter test`、`flutter build apk`。
- 验证结果：通过 `script/flutter.sh` 执行等价命令；APK 位于 `build/app/outputs/flutter-apk/app-release.apk`。
- 环境说明：`flutter doctor -v` 仍报告缺少 Android cmdline-tools 且许可证状态未知。即使当前 Gradle 构建成功，后续设备和发布配置仍需注意这一点。

## Slice 1 — 原生边界 — 已完成

- 平台通道增加 `getHealth`、`getRootStatus`、`attemptRoot`、`getMcpEnabled` 和 `setMcpEnabled`。
- 原生 Root 检查在 UI 线程之外运行；全局开关持久化到 Android SharedPreferences。
- 针对性 Flutter widget 测试覆盖原生状态展示和通道中的开关保存。
- 验证：`flutter analyze`、`flutter test` 和 `flutter build apk` 通过。

## Slice 2 — Root 能力 — 已完成

- `RootProbe` 使用有界超时执行 `su -c id`，只返回状态和原因元数据。
- 用户触发的操作请求 Root 管理器授权并返回检查结果，不声称能够为设备获取 Root。
- 设备可用时，应在 Root 和非 Root 设备上验证；当前应用测试覆盖结果展示。

## Slice 3 — 直接 MCP 核心 — 原型完成

- 增加 APK 进程内的认证 HTTP 服务、JSON-RPC 分发、Bearer 校验、服务关闭行为、受保护资源元数据、`/debug/` 和只读 `root_status` 工具。
- 增加兼容旧客户端的 `ping` 和现代客户端的 `server/discover` 响应。
- 全局开关启动/停止本地网络监听器，并在进程重建后保持设置。
- 剩余发布门槛：自动 HTTP 协议测试和完整 MCP 认证服务器流程验证。

## Slice 4 — 令牌存储 — 原型完成

- 实现四种模式、进程锁内的一次性令牌原子消费、每令牌工具允许列表、只读默认值、物理删除、SHA-256 验证值和元数据列表。
- 当前原型使用 SharedPreferences；LAN 发布前迁移到事务性存储。
- 剩余发布门槛：并发、时间边界、持久化和认证合规测试。

## Slice 5 — 首批设备工具 — 原型完成

- 增加 `device_status`、`screenshot` 和 Root 保护的有界 `shell_exec` 工具。截图使用 MCP 图片内容，Shell 输出有大小上限。
- 剩余发布门槛：Root/非 Root 目标设备能力测试、API 矩阵、超时、输出大小和取消测试。

## Slice 6 — 输入和应用工具 — 原型完成

- 增加独立的 `launch_app`、`tap`、`swipe`、`input_text` 和 `key_event` 工具，并限制参数。`gesture` 等点序列契约确定后再实现。
- 剩余发布门槛：Root 设备人工/自动化矩阵；不增加合并的“自动化”端点。

## Slice 7 — UI 检查可行性门槛

- 为所需的非无障碍视图树/内容路径提供证据。
- 如果没有受支持的路径，将 `view_tree` 和 `view_content` 标记为“未完成”，并在实施前修订需求。

## Slice 8 — 直接调试页面 — 原型完成

- APK 通过直接 MCP 服务提供静态同源 `/debug/` 页面。
- 页面接受内存中的 Bearer 令牌，通过 `tools/list` 发现工具，按 schema 显示表单，并通过 `/mcp` 调用工具。
- 剩余发布门槛：有界脱敏调用记录、浏览器冒烟测试和设备集成测试。

## Slice 8a — 操作地址和语言选择 — 已完成

- Flutter 界面显示平台通道返回的调试地址和 MCP 地址。
- 原生边界报告 MCP 端点 URL 以及认证监听器是否运行。
- 界面支持 System、English 和简体中文；System 跟随受支持的设备语言，不支持时回退英文。
- 调试页面读取同一持久化语言设置，并通过 `POST /debug/locale` 更新。
- 验证：URL、监听状态和三种语言选择均有 widget 覆盖。

## Slice 8b — 能力控制台和只读设备清单 — 原型完成

- 将单列调试页面替换为响应式工具栏和请求/结果工作区。
- 增加请求历史、HTTP 状态/耗时标记、结构化结果和 MCP 图片预览。
- 增加应用、权限、定位、电池、网络、显示和 Root UI 层级/内容工具。
- 增加 `random_tap_by_xpath`，使用 `/dev/hwrng` 或 Android `SecureRandom` 在元素边界内选择均匀随机点。
- 剩余发布门槛：权限/API/Root 矩阵和浏览器/设备冒烟测试。

## Slice 9 — LAN 加固和发布

- 增加 LAN 明确确认、目标 SDK 37+ 的 Android 本地网络权限处理、TLS 决策、生命周期行为和发布检查。当前原型在 MCP 启用时已监听本地接口的 `8787` 端口，并报告有效地址。
- 验证：LAN 认证、权限拒绝、重启、令牌删除和网络暴露测试。

## Slice 9a — 可见后台生命周期和权限操作 — 已完成

- 确认 ARMCP 是 Android Root MCP 的面向操作人员名称，不改变应用 ID。
- 将直接 MCP 监听器从 `MainActivity` 移交给 `McpRuntime` 和 `McpForegroundService`，启用期间使用持续通知。
- 增加类型化 `grantAllPermissions` 平台方法和需风险确认的自动权限开关。只尝试 APK 声明权限，并报告每项拒绝结果。开启后，在 MCP 启用和应用打开时重试。
- 针对性验证：别名、风险提示、结果和服务开关契约的 Flutter widget 测试；`flutter analyze`、`flutter test`、`flutter build apk` 和文档校验通过。

## Slice 9b — Root UI 检查后端 — 进行中

- 根因：在测试的 Android 15/MIUI 设备上，并发或重复的 `uiautomator dump` 会失败，出现 `UiAutomationService already registered` 和进程退出 `137`。
- 主要后端：明确启用的 ARMCP `AccessibilityService`，其 `canRetrieveWindowContent=true`；它尽力刷新节点，重试动态窗口，并返回最近完整快照，不创建 UiAutomation。
- 层级和格式化器不设置字节上限；Android 命令超时、设备内存和 MCP 传输/客户端限制仍是运行边界。
- 回退：只有服务不可用时才使用 Root `uiautomator dump`，并返回结构化 `UI_AUTOMATION_CONFLICT`；服务已连接但窗口不可读时返回服务错误。

## Slice 10 — 运行模式、TalkBack 和工具选择 — 已实现

- 增加持久化 Root/非 Root 模式、非 Root 无障碍检查及支持的手势/文本操作，并提供一键设置引导。
- 增加两种模式下的 TalkBack 适配状态和有界启用行为。
- TalkBack 恢复只在 UI 读取失败时按条件自动执行；切换适配选项不会立即启动 TalkBack。
- Root 模式增加失败 UI 读取时基于截图确认的临时 TalkBack 恢复，并精确恢复设置。
- 将令牌工具开关移动到独立选择页面。
