# Android UI Automation Reference

- Search method: Google via browser MCP
- Search query: `site:developer.android.com root Android UI automation accessibility UiAutomation official`
- Retrieved: 2026-09-24
- Primary source: https://developer.android.com/reference/android/app/UiAutomation

## Relevant Findings

The Android API reference describes `UiAutomation` as a class for interacting with device UI through simulated user actions and screen-content introspection. It relies on platform accessibility APIs to inspect the remote view tree and can inject raw keyboard and touch input events.

The API reference lists capabilities relevant to this project, including:

- `getRootInActiveWindow()` and window APIs for UI inspection.
- `injectInputEvent(...)` and `performGlobalAction(...)` for interaction.
- `takeScreenshot()` for screen capture.
- `executeShellCommand(...)` for shell execution.
- `FLAG_DONT_USE_ACCESSIBILITY`, which disables accessibility-dependent methods rather than providing an independent production UI-control path.

The page also describes `UiAutomation` as a special type of `AccessibilityService` intended for UI test automation. These APIs therefore require explicit validation of API level, process privileges, lifecycle, and behavior on rooted production devices.
