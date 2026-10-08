# Architecture

[English](Architecture.md) · [简体中文](Architecture.zh-CN.md)

## System Layers

```text
Flutter UI and state
          │ typed platform contract
Native Android capability layer (Kotlin)
           ├── RootProbe / RootAttempt
           ├── RootPermissionManager
           ├── DeviceBackend
           ├── UiBackend (non-root/root ARMCP AccessibilityService with disclosed Root fallback)
           ├── TalkBackController (mode- and privilege-gated)
           ├── HardwareRandomBackend (/dev/hwrng through Root)
           ├── ShellBackend
          ├── TokenStore
          └── Direct MCP Streamable HTTP server
                 ├── POST /mcp
                 ├── GET /debug/
                 └── authorization metadata
```

The APK itself is the MCP server. It accepts MCP requests, executes Android capabilities, and serves the debug page in the same process. There is no bridge, proxy, extension, or second MCP server.

The Debug page's UI inspector is a client-side view over the already returned `view_tree`/`view_content` document. It parses the XML or JSON in memory, derives element paths and XML XPath expressions, renders selectable bounds, and exposes the existing `tap` operation for a selected center. It does not add a second inspection backend or execute arbitrary returned script.

Flutter owns bottom navigation, settings, locale selection, status presentation, token creation/tool-switch/deletion UI, and operator actions. Kotlin owns privileged execution, socket lifecycle, authentication, token verification, tool registry/execution, permission-state inspection, shared locale persistence, and debug assets.

`McpForegroundService` owns the running listener through the process-level `McpRuntime`. `MainActivity` only controls that runtime through the platform channel, so destroying or backgrounding the Flutter activity does not call `McpHttpServer.stop()`.

`random_tap_by_xpath` reuses the serialized UI snapshot path, evaluates one XPath locally in the Android process, and samples coordinates from a Root-backed `/dev/hwrng` source when available. If the device has no hardware random device or the bounded read fails, it uses Android `SecureRandom` as a cryptographically secure fallback; it never uses `java.util.Random`, timestamps, or coordinate center defaults.

The saved automatic-permission preference is exposed through the platform channel. Flutter reads it when the app opens and invokes the existing `RootPermissionManager` after an already-enabled MCP state is detected or after MCP is enabled. The Flutter switch owns the explicit risk confirmation and native execution remains bounded and result-reporting.

The platform boundary exposes a read-only declared-permission status report for
Home. It checks the ARMCP manifest's runtime permissions without attempting a
grant and excludes the accessibility service, which is reported separately.

The Flutter shell uses four bottom destinations: Home, Runtime support and
permissions, MCP access tokens, and Settings. Settings writes the persisted
operation mode; the Runtime destination derives its content from that mode.

The persisted operation mode selects capability policy. The same ARMCP
AccessibilityService is usable in both modes after explicit user enablement;
non-root mode uses its node actions and `dispatchGesture` for supported input,
while Root mode additionally exposes bounded shell capabilities. TalkBack
enablement is represented separately: Root can request secure-settings changes,
whereas non-root opens the system accessibility page.

Root TalkBack automation is a temporary recovery path, not a persistent
startup action. With the operator's Root-mode opt-in enabled, a failed UI
inspection first checks whether a screenshot contains visible pixels, then
temporarily enables TalkBack and retries the inspection. The prior secure
accessibility settings are restored in a `finally` path after the retry.

## Atomic components

| Component | Owns | Does not own |
| --- | --- | --- |
| `SettingsStore` | enabled flag, bind policy, port | root execution |
| `RootProbe` | runtime root check and safe evidence | token issuance |
| `RootAttempt` | visible acquisition/recheck action | stealth or boot modification |
| `RootPermissionManager` | bounded `su`/`pm grant` attempts for this APK's declared permissions | granting undeclared permissions or bypassing Android policy |
| `ToolRegistry` | tool schemas and capability declarations | transport authentication |
| `TokenStore` | token verifier, mode, expiry, tool allow-list, use count, physical deletion | UI rendering |
| `AuthIssuer` | access-token issuance, metadata, and validation policy | Android tool execution |
| `McpHttpServer` | HTTP framing, auth gate, JSON-RPC dispatch, debug assets | root probing |
| `AndroidToolExecutor` | one tool invocation at a time | Flutter widget state |

## Request flow

1. Flutter changes the global switch through the typed native contract.
2. Kotlin validates settings, root policy, token policy, and local-network permission.
3. `McpHttpServer` receives `/mcp`, validates the bearer token, filters tools by that token's allow-list, and dispatches one registered tool.
4. `McpHttpServer` rejects tools absent from the token allow-list before `AndroidToolExecutor` is reached; the executor then checks the tool's capabilities and invokes exactly one backend operation.
5. The result becomes MCP content plus a stable error code; sensitive values are redacted from audit logs.

## Backend policy

Use the smallest proven backend per capability. A root shell backend supports bounded shell, app launch, screenshots, device/package/location metadata, and input where the capability check passes. In both modes the declared ARMCP `AccessibilityService` selects the active window, best-effort refreshes nodes, retries dynamic-window failures, and retains the latest complete snapshot during bounded continuous updates. In non-root mode it also performs supported gestures and node text actions. This backend does not create a `UiAutomation` session, so it coexists with the device harness' existing automation connection. A connected service does not fall back to Root `uiautomator dump` merely because one active window is temporarily unreadable; that fallback is reserved for a service that is unavailable and reports registration conflicts. The hierarchy is not capped by an ARMCP byte limit. Each node's top-left-origin screen bounds is accompanied by width, height, and integer center coordinates for direct `tap` use. Runtime permissions and API-level limits are returned as structured failures.

## Debug page design

Use the local `zen-mcp/debug-frontend/` pattern for status, metadata-driven forms, result panels, and bounded logs. Do not copy its browser-specific workspace concepts or Go/WebSocket bridge. The Android server serves static HTML/CSS/JS and the page calls the same authenticated `/mcp` endpoint directly.
