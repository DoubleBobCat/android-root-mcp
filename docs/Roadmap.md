# Implementation Roadmap

[English](Roadmap.md) · [简体中文](Roadmap.zh-CN.md)

Each item is an atomic slice with one contract and focused verification. Do not start a later slice while its predecessor has an unresolved contract or failing gate.

## Slice 0 — project scaffold — complete

- Created with `flutter create --platforms=android --org com.doublecat --project-name android_root_mcp .`.
- Dart entrypoint: `src/lib/main.dart`; native entrypoint: `src/android/app/src/main/kotlin/com/doublecat/android_root_mcp/MainActivity.kt`.
- Added the `com.doublecat.android_root_mcp/capabilities` channel with the `getHealth` method and a Flutter widget test using a mocked channel.
- Verification commands: `flutter analyze`, `flutter test`, and `flutter build apk`.
- Verification result: the equivalent commands run through `script/flutter.sh`; the APK is generated below `build/app/outputs/flutter-apk/app-release.apk`.
- Environment caveat: `flutter doctor -v` still reports missing Android cmdline-tools and unknown Android license status. Keep this warning visible for future device and release setup even though the current Gradle build succeeds.

## Slice 1 — native boundary — complete

- Added `getHealth`, `getRootStatus`, `attemptRoot`, `getMcpEnabled`, and `setMcpEnabled` to the platform channel.
- Native root probing runs off the UI thread; the global enabled flag is persisted in Android SharedPreferences.
- The focused Flutter widget test covers native status rendering and switch persistence through the channel contract.
- Verification: `flutter analyze`, `flutter test`, and `flutter build apk` pass.

## Slice 2 — root capability — complete

- `RootProbe` runs `su -c id` with a bounded timeout and returns only state/reason metadata.
- The user-triggered attempt currently performs an explicit recheck; it does not claim to root the device.
- Verify on rooted and unrooted devices when available; current app-level tests cover result presentation.

## Slice 3 — direct MCP core — prototype complete

- Added the in-process Android authenticated HTTP server, JSON-RPC dispatch, bearer validation, disabled-server behavior, protected-resource metadata, `/debug/`, and one read-only `root_status` tool.
- Added legacy `ping` and modern `server/discover` compatibility responses so connection probes from both MCP protocol eras do not fail with `Method not found`.
- The global switch starts/stops the local-network listener and persists across process recreation.
- Remaining release gate: automated HTTP protocol tests and full MCP authorization-server flow validation.

## Slice 4 — token store — prototype complete

- Implemented all four modes, atomic one-time consumption under a process-local lock, per-token tool allow-lists with read-only defaults, physical deletion, SHA-256 verifiers, and metadata listing.
- Current prototype persists records in SharedPreferences; migrate to a transactional store before LAN release.
- Remaining release gate: concurrency, clock-boundary, persistence, and authorization compliance tests.

## Slice 5 — first device tools — prototype complete

- Added registry-backed `device_status`, `screenshot`, and bounded root-gated `shell_exec` tools. Screenshot results use MCP image content and shell output is capped.
- Remaining release gate: rooted/unrooted target-device capability tests, API-level matrix, timeout, output-size, and cancellation tests.

## Slice 6 — input and app tools — prototype complete

- Added separate registry-backed `launch_app`, `tap`, `swipe`, `input_text`, and `key_event` tools with bounded arguments. `gesture` remains planned until its point-sequence contract is finalized.
- Remaining release gate: rooted-device manual/automated matrix; no combined “automation” endpoint.

## Slice 7 — UI inspection feasibility gate

- Produce evidence for the required non-accessibility view-tree/content path.
- If no supported path exists, keep `view_tree` and `view_content` marked `未完成` and revise the requirement before implementation.

## Slice 8 — direct debug web page — prototype complete

- The APK serves a static same-origin `/debug/` page from the direct MCP server.
- The debug page accepts an in-memory bearer token, discovers the registry through `tools/list`, renders bounded forms, and calls tools through `/mcp`.
- Remaining release gate: bounded redacted call logs, browser smoke tests, and device integration tests.

## Slice 8a — operator URL and language selector — complete

- The Flutter control surface displays the native debug and MCP URLs returned through the platform channel.
- The native boundary also reports the MCP endpoint URL and whether the authenticated local-network listener is running.
- The UI supports runtime English and Simplified Chinese selection using `MaterialApp.locale` and localization delegates.
- The Debug page reads and updates the same persisted language setting through the native server, so its selector stays aligned with the APK UI.
- Verify: widget coverage for URL rendering, listener state, and both language selections.

## Slice 8b — modern capability console and read-only device inventory — prototype complete

- Replaced the single-column debug page with a responsive left tool rail and right request/result workspace.
- Added separated request history, HTTP status/duration badges, structured result rendering, and MCP image previews.
- Added registry-backed application, permission, location, battery, network, display, and root-uiautomator hierarchy/content tools.
- Added `random_tap_by_xpath`, which evaluates an XPath against the current UI snapshot and uses `/dev/hwrng` bytes, or Android `SecureRandom` when hardware random is unavailable, to choose a uniform point inside the selected element before injecting a tap.
- Remaining release gate: permission/API/root matrices and browser/device smoke tests.

## Slice 9 — LAN hardening and release

- Add explicit LAN confirmation, Android local-network permission handling for target SDK 37+, TLS decision, lifecycle behavior, and release checks. The current prototype already binds port `8787` to local interfaces when MCP is enabled and reports the effective address.
- Verify: LAN auth, permission denial, restart, token deletion, and network-exposure tests.

## Slice 9a — ARMCP visible background lifecycle and permission action — complete

- Confirm ARMCP as the operator-facing alias for Android Root MCP without changing the package ID.
- Move the direct MCP listener from `MainActivity` ownership into `McpRuntime` and `McpForegroundService`, using a persistent notification while enabled.
- Add the typed `grantAllPermissions` platform method and a risk-confirmed automatic-permission switch. Only manifest-declared permissions are attempted; per-permission refusal is reported. When enabled, the Flutter lifecycle retries the operation on MCP enable and on opening with MCP already enabled.
- Focused verification: Flutter widget coverage for the alias, warning, action result, and service-backed switch contract; `flutter analyze`, `flutter test`, `flutter build apk`, and documentation validation pass.

## Slice 9b — Root UI inspection backend — in progress

- Root-cause: concurrent or repeated shell `uiautomator dump` calls on the tested Android 15/MIUI device fail with `UiAutomationService already registered` and process exit `137`.
- Primary backend: explicitly enabled ARMCP `AccessibilityService` with `canRetrieveWindowContent=true`; it best-effort refreshes nodes, retries dynamic windows, and returns the latest complete snapshot without creating UiAutomation. Device regression verified this path against Taobao on Android 15/MIUI.
- The hierarchy and formatter no longer impose a byte-size limit. Android's command timeout, device memory, and MCP transport/client limits remain operational boundaries.
- Fallback: Root `uiautomator dump` only when the AccessibilityService is unavailable, with structured `UI_AUTOMATION_CONFLICT`; a connected but unreadable window returns a service-backed error instead. Root does not replace Android's UiAutomation implementation.

## Slice 10 — operation modes, TalkBack, and tool selection — implemented

- Added persisted Root/non-root mode, non-root accessibility-backed inspection
  and supported gesture/text operations, and one-click settings guidance.
- Added Root/non-root TalkBack adaptation state and bounded enablement behavior.
- Made TalkBack recovery conditional and automatic only during a failed UI read;
  changing the adaptation checkbox never starts TalkBack.
- Added Root-only temporary TalkBack recovery for failed UI reads with a
  screenshot-confirmed visible display, including exact settings restoration.
- Moved token tool switches to a dedicated selection route.
