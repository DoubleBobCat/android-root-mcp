# Product Requirements

[English](PRD.md) · [简体中文](PRD.zh-CN.md)

## Product scope

The first production slice is an Android APK with Flutter settings UI, a Kotlin Android capability layer, an authenticated Streamable HTTP MCP server running inside the APK process, and a same-origin debug page served by that process.

The product alias is **ARMCP (Android Root MCP)**. The package ID remains `com.doublecat.android_root_mcp`.

## User Stories

1. As an operator, I can see whether root is unknown, unavailable, available, or failed, and press a visible acquisition-attempt button when it is unavailable.
2. As an operator, I can turn MCP on or off globally and see the effective bind address, port, permission state, and server state.
3. As an operator, I can create a token with one-time, reusable, fixed-duration, or unlimited lifetime, set each tool's switch independently, and delete the token.
4. As an MCP client, I can authenticate to `/mcp` with a bearer token and invoke only tools enabled for that token and supported by the device.
5. As a developer, I can open `/debug/`, enter a token, discover tools in a modern left-tool/right-result console, execute one tool, inspect structured output, and view call history with HTTP status and duration.
6. As a developer, when inspecting `view_tree` or `view_content` in `/debug/`, I can select an element from its tree or screen-coordinate box, see its path, XML XPath where applicable, attributes, bounds, size, and center tap coordinates, and invoke a center tap.
7. As an MCP client, I can read installed applications, requested/granted application permissions, location capability/status, battery, network, display, and current UI hierarchy/content without requiring a screenshot.
8. As an operator, I can inspect root, Android API, network, and tool capability failures without a silent downgrade.
9. As an operator, I can explicitly confirm a risk warning and ask ARMCP to use Root to grant every permission declared by the APK, with per-permission outcomes shown.
10. As an operator, I can leave ARMCP in the background while an enabled MCP listener continues through a visible Android foreground-service notification.
11. As an operator, I can enable an opt-in automatic permission-grant switch; after confirming its risk warning once, ARMCP retries grants when MCP is enabled and when the app opens with MCP already enabled.
12. As an MCP client, I can provide an XPath and ask ARMCP to tap one hardware-random point within the selected element's screen bounds.
13. As an operator, I can select Root or non-Root operation mode and use one action to reach the required accessibility setting.
14. As an operator, I can enable TalkBack adaptation in either mode, with Root allowing temporary automatic recovery and non-Root guiding me through Android settings.
15. As an operator, I can enable Root TalkBack recovery without starting TalkBack immediately; ARMCP starts it only for a failed UI read with screenshot-confirmed content and then restores the prior state.
16. As an operator, I can use four bottom navigation areas: Home, Runtime support and permissions, Tokens, and Settings.
17. As an operator, I can leave language selection on System so the app follows the device language and falls back to English when unsupported, or choose English/中文 explicitly.

## Acceptance criteria

- The global switch is off after first install and after an explicit stop.
- `/mcp` rejects missing, malformed, expired, and spent tokens; deleted token records no longer authenticate.
- Token secrets are shown only at creation and are stored as verifiers, never as plaintext.
- The MCP process uses a read-only baseline. New tokens enable read-only tools only, and `shell_exec` is disabled by default.
- Each issued token has an independent tool allow-list; `tools/list` and `tools/call` enforce it.
- The debug page uses the same authenticated `/mcp` endpoint and does not persist the token by default.
- Every tool declares required capability and returns a stable error code when unavailable.
- Root detection is runtime-based; APK installation is never treated as proof of root.
- When Root is unavailable, Home shows an action to request Root authorization; the action is hidden after Root becomes available.
- Home shows native-boundary health, Root state, non-accessibility permission state, accessibility-service state, and the MCP switch.
- Settings owns the Root/non-Root operation mode. Runtime support and permission content changes with the saved mode.
- The bottom navigation exposes Home, Runtime support and permissions, MCP access tokens, and Settings.
- The default language follows the system locale; unsupported system locales resolve to English, while an explicit English or Simplified Chinese choice overrides the system.
- The all-permissions action is explicit, risk-confirmed, Root-gated, bounded to permissions declared by this APK, and reports permissions that Android refuses to grant.
- Automatic all-permissions granting is disabled by default; its warning appears when the opt-in is enabled, and subsequent MCP start/app launch attempts follow that saved opt-in.
- The debug console fits within the viewport with page scrolling; its inspector tree shares the box model height, preserves box-model width, and expands the tree for horizontal content.
- Selecting a box highlights it red and nested boxes remain individually selectable above their parents.
- Adaptive launcher foreground artwork stays within Android's safe visual area to avoid clipping at icon edges.
- `random_tap_by_xpath` requires one XPath element with valid bounds, prefers `/dev/hwrng`, falls back to Android `SecureRandom` when hardware random is unavailable, samples uniformly inside the full bounds rectangle, and reports the actual random source.
- An enabled listener is owned by a foreground service and remains available across activity backgrounding while the Android process/service remains eligible to run; disabling MCP stops the service.
- Root UI inspection is disclosed as Android UiAutomation-based and remains subject to target-device feasibility verification.
- Non-root UI inspection and supported gestures use the explicitly enabled ARMCP AccessibilityService; Root does not provide a documented universal private-view-tree API that bypasses accessibility/UiAutomation.
- Root TalkBack adaptation is preference-only until the UI-read recovery condition is met; non-root adaptation only opens the system settings guide.
- LAN behavior documents bind address, port, cleartext/TLS policy, permission, and lifecycle.

## MVP tool groups

| Group | Atomic tools |
| --- | --- |
| Device | `device_status`, `root_status`, `screenshot`, `battery_status`, `network_status`, `display_info` |
| App | `launch_app`, `list_apps`, `app_permissions` |
| Location | `location_status`, `get_location` |
| Input | `tap`, `swipe`, `input_text`, `key_event`, `gesture` |
| UI read | `view_tree`, `view_content` |
| Shell | `shell_exec` |

The list is implemented as prototype contracts. Root, runtime permission, API-level, transport, and target-device verification remain release gates. UI inspection discloses its Root UiAutomation backend and is not an invisible general production-app bypass. `view_tree` returns readable XML and `view_content` returns readable JSON; both include screen bounds and center tap coordinates.

## Current implementation status

The current prototype implements native health reporting, a bounded runtime `su -c id` probe, a user-triggered recheck, a risk-confirmed Root permission action, a persisted global MCP setting, an authenticated local-network direct MCP listener owned by a visible foreground service, four token modes, per-token tool switches with read-only defaults, physical token deletion, schema-driven same-origin debug forms, device/input tools, and formatted Root UI inspection. Full OAuth authorization flow and target-device verification remain release-gated.
