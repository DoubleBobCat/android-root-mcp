# MCP 与工具规格

[English](MCP-Spec.md) · [简体中文](MCP-Spec.zh-CN.md)

## 直接传输

- 端点：由 Android APK 进程提供的 `POST /mcp` 和经过认证的 `GET /mcp`。
- 绑定：MCP Streamable HTTP 和 UTF-8 JSON-RPC 消息。
- 认证：每个请求都携带 `Authorization: Bearer <opaque-token>`。
- 未认证响应：HTTP 401，并包含 `WWW-Authenticate` 以及适用 MCP 认证规范要求的受保护资源元数据地址。
- 调试页面：同源 `GET /debug/`，使用相同端点和令牌策略。
- 调试工具按钮必须发送 `method: "tools/call"`，将选中的注册工具写入 `params.name`，将表单值写入 `params.arguments`。注册工具名不是 JSON-RPC 方法名。
- 调试页面语言：同源 `POST /debug/locale` 只改变共享界面语言（`en` 或 `zh`），不授予 MCP 访问权限，也不改变令牌策略。
- MVP 不包含 WebSocket 桥接、代理、扩展或第二个 MCP 服务端。

原型 Bearer 令牌由 APK 内嵌组件签发和验证。受保护资源元数据已经存在，但完整 OAuth 授权服务器流程尚未实现。LAN 发布前，应依据保存的官方资料验证当前 MCP 认证元数据和 OAuth 要求；不能把原型 Bearer 流程当作最终合规实现。

当前传输会将非 JSON MCP POST 请求返回 `415`，将格式错误的 JSON 返回 JSON-RPC `-32700`，将无效 JSON-RPC 信封返回 `-32600`，将未知方法返回 `-32601`，将未知工具或无效参数返回结构化工具错误。通知请求返回 `202` 且没有响应正文。为兼容客户端，服务支持旧版 `ping` 健康检查、现代 `server/discover`，以及 `initialize` 和 `tools/*` 方法。

`GET /mcp` 会认证 Bearer 令牌，并为打开 Streamable HTTP SSE 通道的客户端返回预备好的 `text/event-stream` 响应。JSON-RPC 请求仍使用 POST。一次性令牌不适合 SSE 和 POST 请求序列；调试页面建议使用可重复令牌。

## 认证和权限模型

MCP 操作进程的默认权限为只读。每个 Bearer 令牌拥有独立的工具允许列表。允许列表与令牌一起保存，不与其他令牌共享，并在 Bearer 认证完成后、工具执行前校验。

- 新令牌默认启用只读工具。
- `launch_app`、`tap`、`random_tap_by_xpath`、`swipe`、`input_text`、`key_event` 和 `shell_exec` 默认关闭。
- `shell_exec` 对可重复、固定时长和不限时令牌同样默认关闭。
- `tools/list` 只返回当前认证令牌允许的工具。
- 未授权工具调用返回 `isError: true` 的工具结果及 `TOKEN_TOOL_DISABLED`；后端不会被调用。
- 工具允许列表是额外的权限边界；Root、Android API、运行时权限和工具能力检查仍然适用。

HTTP 认证配置要求每个受保护请求都携带 Bearer 令牌。服务在 JSON-RPC 分发前校验令牌。删除令牌会物理移除验证值和元数据；协议没有撤销令牌字段或撤销令牌列表状态。

## 错误模型

工具失败返回带有 `isError: true` 的 MCP 结果，内容为 JSON 对象：

```json
{
  "code": "ROOT_UNAVAILABLE",
  "message": "Root capability is unavailable",
  "retryable": true,
  "details": { "state": "unavailable" }
}
```

错误码包括 `SERVER_DISABLED`、`AUTH_REQUIRED`、`TOKEN_EXPIRED`、`TOKEN_SPENT`、`TOKEN_TOOL_DISABLED`、`ROOT_UNAVAILABLE`、`CAPABILITY_UNSUPPORTED`、`API_LEVEL_UNSUPPORTED`、`INVALID_ARGUMENT`、`EXECUTION_FAILED` 和 `TIMEOUT`。

## 工具契约

每个工具定义包含 `name`、`description`、`inputSchema`、`requiredCapabilities`、`timeoutMs` 和 `sensitiveOutput`。工具名使用稳定的 snake_case。一个工具执行一个原子操作；批处理属于后续设计。

## 初始工具清单

| 工具 | 必需输入 | 必需能力 | 状态 |
| --- | --- | --- | --- |
| `device_status` | 无 | `device.read` | 原型已实现，待设备验证 |
| `root_status` | 无 | `root.probe` | 原型已实现 |
| `list_apps` | 可选系统应用过滤 | `app.read` | 原型已实现，使用包管理器结果 |
| `app_permissions` | 包名 | `permission.read` | 原型已实现，返回请求/已授予状态 |
| `location_status` | 无 | `location.read` | 原型已实现，返回提供方和权限状态 |
| `get_location` | 无 | `location.read` | 原型已实现，受权限保护的最近位置 |
| `battery_status` | 无 | `device.read` | 原型已实现 |
| `network_status` | 无 | `device.read` | 原型已实现 |
| `display_info` | 无 | `device.read` | 原型已实现 |
| `launch_app` | 包名 | `app.launch` | 原型已实现，待设备验证 |
| `tap` | x、y | `input.inject`、`root.exec` | 原型已实现，待 Root 设备验证 |
| `random_tap_by_xpath` | XPath 表达式 | `ui.inspect`、`input.inject`、`root.exec`、`random.secure` | 读取选中元素边界，在矩形内均匀取整点，并注入一次点击 |
| `swipe` | 起止坐标、时长 | `input.inject`、`root.exec` | 原型已实现，待 Root 设备验证 |
| `input_text` | 文本 | `input.inject`、`root.exec` | 原型已实现，待 Root 设备验证 |
| `key_event` | 键码 | `input.inject`、`root.exec` | 原型已实现，待 Root 设备验证 |
| `gesture` | 有界点序列 | `input.inject` | 计划中 |
| `screenshot` | 无 | `screen.capture`、`root.exec` | 原型已实现，待 Root 设备验证 |
| `view_tree` | 可选窗口/过滤器 | `ui.inspect` | 返回 ARMCP 无障碍服务刷新后的可读缩进 XML，并披露 Root `uiautomator` 回退 |
| `view_content` | 可选文本/包过滤器 | `ui.inspect` | 返回同一快照的可读缩进 JSON，并披露 Root 回退 |
| `shell_exec` | 命令、超时 | `root.exec` | 原型已实现，待 Root 设备验证 |

每一行都有注册表条目、schema、稳定的能力/错误契约和有界执行路径。UI 检查会串行化请求。主要服务选择活动窗口，尽力刷新根节点和子节点，重试动态窗口读取，并在有界持续更新期间保留最近完整快照；它不注册 UiAutomation。服务不可用时，Root 回退可以使用 `uiautomator dump`；Android 注册冲突返回带后端诊断的 `UI_AUTOMATION_CONFLICT`。服务已连接但活动窗口没有可读内容时，返回 `UI_INSPECTION_UNAVAILABLE`，并带有 `backend: accessibility_service` 和 `serviceConnected: true`，不调用 UiAutomation。层级和格式化文档没有 ARMCP 字节上限，但仍受设备内存、命令超时和工具契约之外的传输/客户端限制约束。

### UI 文档和坐标契约

`view_tree` 返回的第一个 MCP 文本内容项是格式化 XML 文档；`view_content` 返回的第一个 MCP 文本内容项是格式化 JSON 文档。文本本身就是文档，不是嵌套在其他结果字段中的 JSON 转义字符串。内容项的 `_meta` 报告 `mimeType`、`backend`、`privilege` 以及是否应用过滤器。

每个拥有有效 Android `bounds` 的元素都提供：

- `bounds`：JSON 中的屏幕像素矩形 `{left, top, right, bottom}`，或 XML 对应属性；
- `size`：宽度和高度；
- `tap`：JSON 中的 `{x, y, point: "center"}`，或 XML 中的 `tap-x`/`tap-y` 属性。

坐标原点是屏幕左上角，x 向右增加，y 向下增加。中心坐标按 `left + (right - left) / 2` 和 `top + (bottom - top) / 2` 计算，并取整数屏幕像素，因此返回的 `tap` 值可以直接传给 `tap` 工具。过滤会保留匹配的祖先和后代，生成有效文档，而不会只过滤单独的 XML 行。

`random_tap_by_xpath` 在与 `view_tree`/`view_content` 相同的当前 UI XML 快照上计算传入 XPath。选中元素必须有有效的 `bounds` 矩形。点击矩形为元素屏幕矩形 `[left, right) × [top, bottom)`，等价于中心点加减宽高的一半。优先从 Linux `/dev/hwrng` 读取的原始字节进行拒绝采样；设备没有该文件或无法完成有界读取时，使用 Android `SecureRandom`。结果报告 `randomSource: "linux_hwrng"` 或 `randomSource: "android_secure_random"`。不使用可预测的软件伪随机或固定中心回退。

### 调试页面 UI 检查契约

调试页面显示成功的 `view_tree` 或 `view_content` 调用时，在结果下方提供本地检查器。检查器包含可展开的元素树、可选的屏幕坐标边界模型和详情面板。元素树随返回层级展开，不设置独立的固定高度滚动条。边界模型将完整屏幕坐标画布缩放到面板宽度，保留详情和点击操作中的原始屏幕坐标，不要求额外横向浏览。树行或边界框选中的是同一个元素。详情面板显示 JSON 路径、元素属性、边界、大小和中心点击坐标。`view_tree` 还显示生成的 XPath 并提供复制操作。路径在本地从返回快照生成；选择元素不会再次请求设备检查。元素有边界时，检查器可以使用计算出的中心坐标调用现有 `tap` 工具。

## 令牌模式

令牌创建是 Flutter 中的操作人员动作。原始密钥只返回一次。服务只保存验证值/哈希、令牌 ID、模式、创建时间、有效期、启用的工具名和使用次数。一次性消费与认证原子完成，避免并发复用。删除令牌会移除记录，而不是标记撤销。
