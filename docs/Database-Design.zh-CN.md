# 数据设计

[English](Database-Design.md) · [简体中文](Database-Design.zh-CN.md)

当前原型使用 Android SharedPreferences 保存标量设置和令牌记录，并通过进程内锁保护令牌更新。面向 LAN 发布前，应将令牌和审计记录迁移到具备原子性、保留期限和并发保证的存储；原型存储不视为可发布数据库。

## `settings`

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `key` | text | 主键 |
| `value` | text | 序列化标量，不保存令牌密钥 |

必需键：`mcp_enabled`、`bind_mode`、`bind_host`、`port`、`target_sdk`、`debug_enabled`。

## `tokens`

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `id` | text | 随机不透明 ID，主键 |
| `verifier` | blob/text | 加盐验证值，不保存原始令牌 |
| `mode` | text | `one_time`、`reusable`、`fixed_duration`、`unlimited` |
| `created_at` | integer | UTC epoch 毫秒 |
| `expires_at` | integer nullable | 固定时长模式必填 |
| `enabled_tools` | JSON array | 此令牌的工具允许列表；默认只读工具，不得包含未知工具 |
| `use_count` | integer | 原子递增 |
| `last_used_at` | integer nullable | 仅保存元数据 |

MCP 操作的默认权限为只读。启动应用、注入输入或执行 Shell 的工具默认不启用，必须针对每个令牌单独选择。新令牌的 `shell_exec` 始终默认关闭。

删除令牌时，完整记录及其验证值一并删除。数据模型不包含 `revoked_at`、`revoked` 或墓碑字段。已删除令牌与未知密钥一样无法认证，也不会出现在列表或审计引用中。

## `audit_events`

保存事件 ID、时间、操作、令牌 ID（不含密钥）、工具名、结果、错误码、耗时和已脱敏的请求摘要。默认不保存命令文本、输入文本、截图、令牌值或完整 UI 内容。

## 保留期限

最多保留 500 条审计事件，并以事务方式删除最旧记录。令牌记录应显式删除；只有在操作人员不再需要查看过期记录后，才可清理过期令牌。
