# Documentation Map

[English](README.md) · [简体中文](README.zh-CN.md)

These documents define the product behavior, technical boundaries, and operating procedures.

## Product alias

**ARMCP** is the product alias for **Android Root MCP**. The package/application ID remains `com.doublecat.android_root_mcp`; ARMCP is the operator-facing name used by the Flutter UI, foreground-service notification, documentation, and token prefix.

- [Vision](Vision.md): product outcome, KISS boundaries, and non-goals. [中文](Vision.zh-CN.md)
- [PRD](PRD.md): user stories, acceptance criteria, and target tool groups. [中文](PRD.zh-CN.md)
- [SRS](SRS.md): functional, non-functional, and feasibility requirements. [中文](SRS.zh-CN.md)
- [Architecture](Architecture.md): direct APK MCP topology and atomic components. [中文](Architecture.zh-CN.md)
- [MCP and Tool Specification](specs/MCP-Spec.md): transport, auth, errors, and tool inventory. [中文](specs/MCP-Spec.zh-CN.md)
- [Data Design](Database-Design.md): settings, token, and audit records. [中文](Database-Design.zh-CN.md)
- [Control API](API/openapi.yaml): authenticated operator API and direct MCP endpoint.
- [Roadmap](Roadmap.md): atomic implementation slices and gates. [中文](Roadmap.zh-CN.md)
- [Test Plan](Test-Plan.md): test layers, fixtures, and release gates. [中文](Test-Plan.zh-CN.md)
- [Deployment](Deployment.md): APK and LAN runtime modes. [中文](Deployment.zh-CN.md)
- [Operation](Operation.md): lifecycle, debug page, token, and incident procedures. [中文](Operation.zh-CN.md)
- [Applicability Decisions](decisions/applicability.md): documentation-stage applicability.
- [References](refs/): MCP/Google-retrieved official Android, Flutter, and MCP sources.
- [Branding](branding/node-bridge.svg): selected ARMCP Node Bridge logo source.
- [Validation Report](validation-report.md): latest documentation validator output.

## Direct MCP decision

The APK itself serves MCP and the debug page. The project does not use a bridge, proxy, browser extension, or second MCP server. The `zen-mcp` debug frontend is a UI-pattern reference only.
