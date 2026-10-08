# Vision

[English](Vision.md) · [简体中文](Vision.zh-CN.md)

## Project Goals

Build one Android APK, written with Flutter and native Android code, that can expose a deliberately enabled and authenticated MCP server on the device or LAN. The APK also serves a same-origin web debug console.

The operator-facing product alias is **ARMCP (Android Root MCP)**. This alias does not change the Android package identity.

## Principles

- **KISS:** one APK, one native Android boundary, one direct MCP server, one debug origin, and no bridge or proxy.
- **Atomicity:** one capability, one tool, one input schema, one capability check, and one focused verification.
- **Truthful capability:** unavailable root, unsupported API levels, and unproven UI control are explicit states, not hidden fallbacks.
- **Least exposure:** the server is disabled by default, binds deliberately, and requires a token.
- **Visible background lifecycle:** when enabled, the MCP listener runs in an Android foreground service with a persistent notification; it is never presented as hidden or stealth operation.

## Non-goals

- Automatically rooting or modifying a device boot chain.
- Claiming that root control is invisible to other software.
- Building a general remote-administration platform.
- Adding a second MCP server, bridge, proxy, extension, or plugin marketplace.
