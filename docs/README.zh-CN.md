# 文档导航

[English](README.md) · [简体中文](README.zh-CN.md)

设计文档是产品需求的依据。每份一级 Markdown 文档均提供英文和简体中文版本，可通过文首语言链接切换。

## 产品名称

**ARMCP** 是 **Android Root MCP** 面向操作人员的产品名称。Android 应用 ID 保持为 `com.doublecat.android_root_mcp`；Flutter 界面、前台服务通知、文档和令牌前缀使用 ARMCP。

- [产品愿景](Vision.md) · [中文](Vision.zh-CN.md)：产品目标、原则和非目标。
- [产品需求](PRD.md) · [中文](PRD.zh-CN.md)：用户故事、验收标准和工具分组。
- [软件需求规格](SRS.md) · [中文](SRS.zh-CN.md)：功能、非功能和可行性要求。
- [系统架构](Architecture.md) · [中文](Architecture.zh-CN.md)：APK 内 MCP 拓扑和组件边界。
- [MCP 与工具规格](specs/MCP-Spec.md)：传输、认证、错误和工具清单。
- [数据设计](Database-Design.md) · [中文](Database-Design.zh-CN.md)：设置、令牌和审计记录。
- [控制 API](API/openapi.yaml)：经过认证的操作 API 和 MCP 端点。
- [实施路线图](Roadmap.md) · [中文](Roadmap.zh-CN.md)：原子实施阶段和验证门槛。
- [测试计划](Test-Plan.md) · [中文](Test-Plan.zh-CN.md)：测试层、测试夹具和发布门槛。
- [部署说明](Deployment.md) · [中文](Deployment.zh-CN.md)：APK 和 LAN 运行方式。
- [运行手册](Operation.md) · [中文](Operation.zh-CN.md)：生命周期、调试页面、令牌和事故处理。
- [适用性决策](decisions/applicability.md)：文档阶段的适用性记录。
- [参考资料](refs/)：从 Google 检索的 Android、Flutter 和 MCP 官方资料。
- [品牌图稿](branding/node-bridge.svg)：ARMCP Node Bridge 标志。
- [文档校验报告](validation-report.md)：文档完整性检查结果。

## MCP 运行方式

APK 自身运行 MCP 服务并提供调试页面。系统不另设桥接服务、代理、浏览器扩展或第二个 MCP 服务端。
