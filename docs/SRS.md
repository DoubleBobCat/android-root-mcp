# Software Requirements Specification

[English](SRS.md) · [简体中文](SRS.zh-CN.md)

## Functional requirements

### FR-01 Global lifecycle

The app SHALL persist one global MCP enabled flag. Starting the server requires the flag, valid configuration, a usable authentication policy, and required local-network permission. The listener SHALL be owned by a visible Android foreground service while enabled so activity backgrounding does not stop it. Stopping it SHALL close the listener, stop the service, and reject new MCP requests.

### FR-02 Root capability

The Android layer SHALL expose `unknown`, `available`, `unavailable`, and `error` states with safe evidence. A user-triggered acquisition attempt SHALL be available for `unavailable`; it may request an installed root manager or re-run a controlled check, but SHALL NOT imply that the app can root the device. The action SHALL be hidden when Root is available. It SHALL also expose a Root-gated `grantAllPermissions` action that enumerates only permissions declared by the ARMCP APK, attempts bounded `su`/`pm grant` operations, avoids logging command output, and returns per-permission outcomes. Android signature, privileged, special-access, and device-policy restrictions may still prevent a grant and SHALL be reported. The UI SHALL require explicit risk confirmation when enabling the automatic permission switch before invoking it.

### FR-03 Tool capability checks

Each tool SHALL check its own required capabilities before execution. Required dimensions are root state, Android API level, foreground/background constraints, and backend support. Unsupported operations SHALL not silently downgrade.

The MCP operation process SHALL report a fixed `operationPermission: read_only` baseline. Any state-changing or command-execution capability is available only when the authenticated token's independent tool allow-list enables that tool.

### FR-04 Token policy and read-only baseline

The MCP operation process SHALL have a read-only baseline. Tokens SHALL support `one_time`, `reusable`, `fixed_duration`, and `unlimited` modes. Each token SHALL carry an independent allow-list of registered tools. New tokens SHALL enable read-only tools only; launch, input, and shell tools SHALL be disabled by default, with `shell_exec` explicitly defaulting to disabled. One-time tokens are consumed atomically after one successful authenticated request; reusable tokens remain valid until deleted; fixed-duration tokens expire at a server-defined timestamp; unlimited tokens have no time expiry but remain deletable. All modes are subject to the global switch. Deleting a token SHALL remove its complete record rather than setting a revoked state.

### FR-05 MCP transport

The APK SHALL expose Streamable HTTP at `POST /mcp` and authenticated `GET /mcp`, require `Authorization: Bearer <token>`, preserve JSON-RPC semantics, and return structured MCP errors. It SHALL expose the protected-resource metadata required by the applicable MCP authorization profile. There is no bridge transport.

The prototype token issuer is embedded in the APK for the local operator workflow. Its metadata, access-token audience, and validation behavior SHALL be checked against the current MCP authorization specification before LAN release is accepted; the prototype is not yet a complete OAuth authorization server.

### FR-06 Debug page

`GET /debug/` SHALL serve static same-origin content from the APK process. The page SHALL show server/root status, accept a token in memory, call `tools/list`, render forms from schemas, execute `tools/call`, show JSON results/errors, and keep at most 50 redacted call-log entries. For successful `view_tree` and `view_content` results it SHALL provide a client-side selectable element tree and bounds box model, show paths, attributes, bounds, size, and center coordinates, show/copy XPath for XML, and offer the existing center-coordinate `tap` action. It SHALL not execute arbitrary JavaScript received from an MCP response.

### FR-07 LAN lifecycle

The server SHALL bind only while the operator explicitly enables MCP, report the effective local-network address, and require authentication for MCP requests. Cleartext LAN use and Android local-network permission behavior SHALL be explicit in the settings and operation docs.

### FR-08 Debug layout and automatic permission grant

The debug page SHALL keep its title bar and desktop panels within the viewport; each panel may scroll its own overflowing content, while narrow layouts SHALL use one page-scrolling column. Result content SHALL not expand the outer grid beyond the viewport. The `view_tree`/`view_content` inspector SHALL give the tree and box-model stage the same rendered height, retain the box-model width, allow the tree to grow horizontally for long paths, render selected boxes in red with descendants above ancestors for pointer selection, and scroll the corresponding tree row into view after selection from either side. The optional automatic all-permissions setting SHALL be persisted disabled by default, show the risk warning when enabled, and invoke the existing bounded Root grant operation on app launch and MCP enable when enabled.

The ARMCP permission result SHALL always show the overall result and SHALL list only permissions whose grant status is failed; successful and already-granted permissions SHALL not be listed individually.

### FR-09 Runtime operation mode and accessibility permission

ARMCP SHALL persist an explicit `root` or `non_root` operation mode. Non-root
mode SHALL use the ARMCP AccessibilityService for supported UI inspection and
gesture/input operations, and SHALL report Root-only operations as unavailable
with a stable reason. A one-click permission action SHALL open Android's
accessibility settings when the service is not enabled. Root MAY request the
service in secure settings, but the UI SHALL still verify the service state.

The operation mode selector SHALL be located in Settings. The Runtime support
and permissions area SHALL show Root permission controls when the selected mode
is Root and SHALL show only ARMCP running support when it is non-Root. Home
SHALL summarize Root state, non-accessibility permission state, and
accessibility-service state.

### FR-12 Navigation and language

The Flutter UI SHALL expose Home, Runtime support and permissions, MCP access
tokens, and Settings through a bottom navigation bar. Language preference SHALL
support `system`, `en`, and `zh`; `system` SHALL leave `MaterialApp.locale`
unoverridden, use the device locale when supported, and resolve unsupported
locales to English. An explicit language choice SHALL override the system and
persist through the native platform contract.

### FR-10 TalkBack adaptation

Both operation modes SHALL expose a TalkBack adaptation checkbox. Enabling it
SHALL only persist the preference and detect the installed/enabled TalkBack
component. In Root mode the preference permits a bounded secure-settings
recovery during a failed UI read; it SHALL not start TalkBack immediately. In
non-root mode enabling the control SHALL open accessibility settings for user
confirmation. ARMCP SHALL keep its own service's window-content and gesture
capabilities enabled where Android and the target application expose them.

### FR-11 On-demand TalkBack

The TalkBack adaptation checkbox SHALL only persist the operator preference and
MCP/UI compatibility behavior. It SHALL NOT start TalkBack when Root mode is
selected. Root SHALL start it only as part of the bounded failed-UI-read
recovery described below; non-root SHALL open Android's accessibility settings
for explicit confirmation instead.

When Root mode has the on-demand TalkBack preference enabled, a failed
`view_tree`/`view_content` inspection MAY first confirm through a Root
screenshot that the display contains visible content. If content is present,
ARMCP MAY temporarily add TalkBack to the enabled accessibility services,
retry the same inspection, and restore the exact previous accessibility
service list and accessibility-enabled value after the retry. It SHALL not
leave TalkBack enabled merely because the fallback was attempted.

## Non-functional requirements

- One request performs one bounded operation and returns one result.
- Root checks and tool execution do not block the Flutter UI thread.
- Token values never appear in normal logs, crash reports, or debug results.
- Errors have stable machine-readable codes.
- Dependencies remain minimal; use Flutter/Dart and Android/Kotlin facilities where practical.
- Audit logs contain metadata, outcome, duration, and error code, never token material or sensitive payloads by default.

## Explicit feasibility gate

UI inspection SHALL disclose its backend and privilege. `view_tree` SHALL return directly readable, parseable XML and `view_content` SHALL return directly readable, parseable JSON in their MCP text content. Each node with screen bounds SHALL expose its top-left-origin bounds, width, height, and integer center point usable by `tap`. The primary backend SHALL be the explicitly declared ARMCP accessibility service with `canRetrieveWindowContent=true`; it SHALL select the active window, treat node refresh and child retrieval as best effort, retry dynamic-window failures, and retain the latest complete snapshot during bounded continuous updates. It SHALL not create a UiAutomation session. A Root `uiautomator dump` fallback MAY be used only when the service is unavailable and SHALL disclose registration conflicts; a connected service SHALL report a service-backed inspection error rather than switch back to UiAutomation when an active window is temporarily unreadable. Neither backend has an ARMCP hierarchy byte limit.
