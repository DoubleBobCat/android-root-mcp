# Test Plan

[English](Test-Plan.md) · [简体中文](Test-Plan.zh-CN.md)

## Test layers

1. **Dart unit/widget:** four-destination bottom navigation, system/explicit language selection and fallback, settings-owned runtime mode, global switch, Root authorization action visibility, automatic permission switch warning and persistence, root status presentation, permission/accessibility summaries, token form validation, error rendering.
2. **Kotlin unit:** token hashing/validation, expiry, atomic one-time use, independent per-token tool allow-lists, read-only defaults, `shell_exec` default-off, physical deletion, capability registry, request limits.
3. **Protocol integration:** HTTP status, `WWW-Authenticate`, MCP ping/server-discover/initialize/tools/list/tools/call, malformed JSON-RPC, disabled server.
4. **Debug UI:** static page load, token stays in memory, tools discovered from schema, every tool button emits `tools/call` with `{name, arguments}`, separated history/status/result panels, HTTP status and duration, image content rendering, log redaction and 50-entry cap, viewport-bounded desktop panels with internal scrolling, narrow-layout page scrolling, equal-height selectable UI tree/box model, horizontally readable paths, red selected boxes with descendant hit priority, tree-row auto-scroll after box/tree selection, JSON paths, XML XPath, attributes, bounds, and center-tap action.
5. **Device integration:** rooted/unrooted/denied states; API-level matrix; screenshot, shell, input, launch; process death and restart.
6. **LAN security:** explicit MCP enable, local-network permission denial, token deletion, per-token tool filtering, cleartext/TLS policy, concurrent requests, and advertised-address validation.
7. **Background and permission integration:** foreground-service notification/start-stop behavior across activity backgrounding, eligible restart, Root unavailable/timeout, granted permission, already-granted permission, and signature/special-access refusal cases; permission result rendering shows only failed permissions.
8. **UI inspection integration:** enabled ARMCP accessibility-service snapshot, active-window selection, best-effort node refresh, dynamic child invalidation, latest-complete-snapshot retry behavior, connected-service/no-readable-window diagnostics, no ARMCP hierarchy byte limit, formatted XML/JSON documents, valid center tap coordinates, structurally filtered content, Root fallback conflicts, and Android 15 `already registered` diagnostics, including dynamic special-app windows such as Taobao.
9. **Hardware-random XPath input:** valid and invalid XPath, missing bounds, accessibility and root snapshot backends, `/dev/hwrng` unavailable/read failure, uniform rejection sampling within `[left, right) × [top, bottom)`, bounded input injection, and no software-random fallback.
10. **Mode/accessibility/TalkBack:** persisted Root/non-root selection, Root-only root status and permission controls, non-root service-guidance action, Root preference change without immediate service activation, Root screenshot-confirmed temporary TalkBack retry and exact restoration of both secure settings, preservation when TalkBack was already enabled, empty/unreadable screenshot no-op, and AccessibilityService gesture/text capability failures.
11. **Navigation and environment:** Home contains native boundary, Root, non-accessibility permission, accessibility, and MCP states; Runtime support and permissions changes with mode; Root authorization is hidden after availability; Settings persists mode and language.

## Required fixtures

- Fake root executor with available, unavailable, denied, timeout, and malformed outputs.
- Fake clock for exact expiry boundaries.
- Fake direct MCP HTTP server for debug-page tests.
- Redacted command and screenshot fixtures; never commit real secrets or personal device content.
- Permission fixtures containing only manifest permission names and redacted `pm grant` outcomes.

## Release gates

- No tool is listed as implemented until its registry, schema, capability check, and focused test exist.
- UI XML inspection verifies root-unavailable, timeout, malformed-dump, and successful hierarchy cases; results include backend/privilege metadata.
- UI inspection success cases verify that `view_tree` emits parseable XML and `view_content` emits parseable JSON directly in the MCP text content, with `tap.x`/`tap.y` matching each element's bounds center.
- Debug UI inspection verifies that selecting either a tree row or a bounds box updates one shared details panel, generated paths remain stable for the snapshot, XPath is shown only for XML, and the center-tap action sends the selected center coordinates.
- No LAN default, no unlimited-token default, and no write or `shell_exec` default for a new token.
- New token tool selection is exercised through its dedicated route; the main settings surface does not render the complete tool switch list.
- `flutter analyze`, focused Flutter tests, Android unit/instrumentation tests, and release build must pass once the scaffold defines those commands.
